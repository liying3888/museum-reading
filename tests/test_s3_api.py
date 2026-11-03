#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""S3 批 2 新接口测试——docs/S3/01-接口契约与DDL草案.md §5（leader 2026-11-04 过闸）

覆盖：B1/B2 books 公开域、R1 start 幂等三型、R2/R3/R4 真暂停链+冻结断言（修正令）、
R5 session、R6 记录删除、G1/G2 goal 边界、S2/S3 书架移出+置顶、T1 checkin 月参数化、T2 overview goal 键。

不在此文件（验证路径另定）：
- 240 自动结束扫描：@Scheduled 周期 5min，HTTP 层等不起——净时长公式已由本文件断言覆盖，
  扫描触发留验收单人工验证（SQL 造 start_time 老数据+观察日志 auto_close 行）
- B1 未上架不可见：需 admin 造下架书，留验收单人工
"""

import random
import string
import time

import pytest
import requests

from test_api import ADMIN_BASE, FRONT_BASE, TIMEOUT, _check, _front_token

# front 注册接口（底座）：用于造第二个用户做真越权用例
REGISTER_URL = f"{FRONT_BASE}/api/login/register"


def _register_user():
    """注册随机新用户并返回 token（每次唯一，避免重跑撞用户名）
    底座校验：账号 3-12 位数字+字母混合（不可纯数字/纯字母），密码 6-12 位"""
    digits = "".join(random.choices(string.digits, k=6))
    username = f"t3a{digits}"  # 9 位，字母数字混合
    r = requests.post(
        REGISTER_URL,
        json={"username": username, "password": "test1234"},
        timeout=TIMEOUT,
    )
    data = r.json()
    if not isinstance(data, dict) or data.get("code") != 200:
        pytest.skip(f"注册接口不可用或参数不符: {data}")
    token = (data.get("data") or {}).get("token") if isinstance(data.get("data"), dict) else None
    if not token:
        # 部分底座注册后需登录换 token
        r2 = requests.post(
            f"{FRONT_BASE}/api/login/accountLogin",
            json={"username": username, "password": "test1234"},
            timeout=TIMEOUT,
        )
        d2 = r2.json()
        _check(d2)
        token = d2["data"]["token"]
    return token


def _h(token):
    return {"like-token": token}


def _start(token, book_id=None):
    r = requests.post(
        f"{FRONT_BASE}/api/front/reading/start",
        json={} if book_id is None else {"book_id": book_id},
        headers=_h(token),
        timeout=TIMEOUT,
    )
    return r.json()["data"]


def _post(token, path, body=None):
    r = requests.post(
        f"{FRONT_BASE}{path}", json=body or {}, headers=_h(token), timeout=TIMEOUT
    )
    return r.json()


def _get(token, path, params=None):
    r = requests.get(
        f"{FRONT_BASE}{path}", params=params or {}, headers=_h(token), timeout=TIMEOUT
    )
    return r.json()


def _ensure_shelf(token, book_id):
    """幂等加入书架：先移出（可能在架）再加入"""
    _post(token, "/api/front/bookshelf/delete", {"book_id": book_id})
    return _check(_post(token, "/api/front/bookshelf/add", {"book_id": book_id}))


class TestBooksList:
    """B1 书籍列表（公开+搜索+过滤+分页）"""

    def test_public_without_token(self):
        r = requests.get(f"{FRONT_BASE}/api/front/books/list", timeout=TIMEOUT)
        data = r.json()
        _check(data)
        assert data["data"]["count"] > 0, "库内应有测试书目"
        first = data["data"]["lists"][0]
        for key in ("id", "title", "author", "pages", "category_id", "category_name"):
            assert key in first, f"B1 缺字段: {key}"

    def test_keyword_title(self):
        r = requests.get(
            f"{FRONT_BASE}/api/front/books/list",
            params={"keyword": "三体"},
            timeout=TIMEOUT,
        )
        data = r.json()
        _check(data)
        assert any("三体" in b["title"] for b in data["data"]["lists"]), "书名搜索未命中"

    def test_keyword_author(self):
        r = requests.get(
            f"{FRONT_BASE}/api/front/books/list",
            params={"keyword": "刘慈欣"},
            timeout=TIMEOUT,
        )
        data = r.json()
        _check(data)
        assert any("刘慈欣" in (b["author"] or "") for b in data["data"]["lists"]), "作者搜索未命中"

    def test_category_filter(self):
        # 先取全量里的一个分类，再过滤验证
        r0 = requests.get(f"{FRONT_BASE}/api/front/books/list", timeout=TIMEOUT).json()
        categories = r0["data"].get("categories")
        assert categories, "首屏应带 categories 聚合键（契约 §6 决策点②）"
        cid = categories[0]["id"]
        r = requests.get(
            f"{FRONT_BASE}/api/front/books/list",
            params={"category_id": cid},
            timeout=TIMEOUT,
        )
        data = r.json()
        _check(data)
        assert data["data"]["lists"], "分类过滤后不应为空"
        assert all(b["category_id"] == cid for b in data["data"]["lists"]), "分类过滤不纯"

    def test_pagination_consistent(self):
        r1 = requests.get(
            f"{FRONT_BASE}/api/front/books/list", params={"page_size": 2}, timeout=TIMEOUT
        ).json()
        r2 = requests.get(
            f"{FRONT_BASE}/api/front/books/list", params={"page_no": 2, "page_size": 2},
            timeout=TIMEOUT,
        ).json()
        assert r1["data"]["count"] == r2["data"]["count"], "两页 count 应一致"

    def test_category_filter_diff(self):
        # 补强令（leader 2026-11-04）：传/不传 category_id 列表集必须有差异——
        # camelCase 吞参 bug 时过滤静默失效返回全量，仅靠「纯」断言不足防回归
        r_all = requests.get(f"{FRONT_BASE}/api/front/books/list", timeout=TIMEOUT).json()
        categories = r_all["data"]["categories"]
        # 选一个「过滤后少于全量」的分类（库内多分类时必存在）
        all_ids = {b["id"] for b in r_all["data"]["lists"]}
        diff_cid = None
        for cate in categories:
            r = requests.get(
                f"{FRONT_BASE}/api/front/books/list",
                params={"category_id": cate["id"]},
                timeout=TIMEOUT,
            ).json()
            ids = {b["id"] for b in r["data"]["lists"]}
            assert all(
                b["category_id"] == cate["id"] for b in r["data"]["lists"]
            ), f"分类 {cate['id']} 过滤不纯"
            if len(ids) < len(all_ids):
                diff_cid = cate["id"]
                break
        assert diff_cid is not None, "库内应存在过滤后少于全量的分类（多分类数据前提）"

    def test_pagination_no_overlap(self):
        # 补强令（leader 2026-11-04）：page_no=2 与第 1 页 id 集不重合——
        # defaultValue 掩盖盲区（page_no 被吞时两页返回同一段数据，count 断言测不出）
        r1 = requests.get(
            f"{FRONT_BASE}/api/front/books/list", params={"page_size": 2}, timeout=TIMEOUT
        ).json()
        assert r1["data"]["count"] > 2, "库内应多于 2 本才可验翻页"
        r2 = requests.get(
            f"{FRONT_BASE}/api/front/books/list",
            params={"page_no": 2, "page_size": 2},
            timeout=TIMEOUT,
        ).json()
        ids1 = {b["id"] for b in r1["data"]["lists"]}
        ids2 = {b["id"] for b in r2["data"]["lists"]}
        assert ids2, "第 2 页不应为空（库内 >2 本）"
        assert not (ids1 & ids2), f"翻页数据重合: {ids1 & ids2}（page_no 被忽略疑似）"


class TestBooksDetail:
    """B2 书籍详情"""

    def test_detail_public_with_intro(self):
        list0 = requests.get(f"{FRONT_BASE}/api/front/books/list", timeout=TIMEOUT).json()
        book_id = list0["data"]["lists"][0]["id"]
        r = requests.get(
            f"{FRONT_BASE}/api/front/books/detail",
            params={"id": book_id},
            timeout=TIMEOUT,
        )
        data = r.json()
        _check(data)
        assert "intro" in data["data"], "B2 缺 intro"
        assert data["data"]["id"] == book_id

    def test_detail_not_found(self):
        r = requests.get(
            f"{FRONT_BASE}/api/front/books/detail", params={"id": 999999}, timeout=TIMEOUT
        )
        assert r.json().get("code") != 200


class TestStartIdempotent:
    """R1 start 幂等（悬置会话重入三型，02 §5.4）"""

    def test_same_book_returns_open_session(self):
        token = _front_token()
        s1 = _start(token, 1)
        assert s1["resumed"] is False
        s2 = _start(token, 1)
        assert s2["resumed"] is True, "同书重入应返回既有会话"
        assert s2["record_id"] == s1["record_id"], "不应新建会话"
        _post(token, "/api/front/reading/end", {"record_id": s1["record_id"]})

    def test_different_book_returns_open_session(self):
        token = _front_token()
        s1 = _start(token, 1)
        s2 = _start(token, 2)
        assert s2["resumed"] is True, "异书重入应返回 A 书既有会话"
        assert s2["record_id"] == s1["record_id"]
        assert s2["book_id"] == 1, "不应串到 B 书"
        _post(token, "/api/front/reading/end", {"record_id": s1["record_id"]})

    def test_free_reading_returns_open_session(self):
        token = _front_token()
        s1 = _start(token, 1)
        s2 = _start(token)  # 自由阅读（无 book_id）
        assert s2["resumed"] is True
        assert s2["record_id"] == s1["record_id"]
        _post(token, "/api/front/reading/end", {"record_id": s1["record_id"]})


class TestPauseResumeEnd:
    """R2/R3/R4/R5 真暂停链（含修正令冻结断言）"""

    def test_pause_freezes_net_duration(self):
        """冻结断言（修正令）：paused 态 R5 net_duration 不随墙钟增长"""
        token = _front_token()
        s = _start(token, 1)
        rid = s["record_id"]
        time.sleep(2)          # 运行 2s
        _check(_post(token, "/api/front/reading/pause", {"record_id": rid}))

        q1 = _get(token, "/api/front/reading/session")["data"]
        time.sleep(2)          # 暂停中过 2s
        q2 = _get(token, "/api/front/reading/session")["data"]

        assert q1["paused_at"] > 0, "paused_at 应为暂停起点"
        assert q1["net_duration"] == q2["net_duration"], \
            f"paused 态净时长应冻结: {q1['net_duration']} vs {q2['net_duration']}"
        _post(token, "/api/front/reading/end", {"record_id": rid})

    def test_pause_resume_end_chain_excludes_pause_segment(self):
        """暂停 3s 后结束：duration 应小于 end-start（净时长=Σ运行段）"""
        token = _front_token()
        s = _start(token, 1)
        rid = s["record_id"]
        start = s["start_time"]

        time.sleep(1)
        _check(_post(token, "/api/front/reading/pause", {"record_id": rid}))
        time.sleep(3)          # 暂停 3s（不计入）
        _check(_post(token, "/api/front/reading/resume", {"record_id": rid}))
        time.sleep(1)
        end = _check(_post(token, "/api/front/reading/end", {"record_id": rid}))

        wall = int(time.time()) - start
        duration = end["data"]["duration"]
        assert wall - duration >= 2, f"净时长应扣除暂停段: wall={wall} duration={duration}"
        for key in ("duration", "today_total", "streak_days", "goal",
                    "goal_gap", "goal_achieved", "auto_closed"):
            assert key in end["data"], f"R4 成果缺字段: {key}"

    def test_end_idempotent(self):
        """重复结束返回既有结果，不产生双记录"""
        token = _front_token()
        rid = _start(token, 1)["record_id"]
        e1 = _check(_post(token, "/api/front/reading/end", {"record_id": rid}))
        e2 = _check(_post(token, "/api/front/reading/end", {"record_id": rid}))
        assert e1["data"]["duration"] == e2["data"]["duration"]


class TestForeignRecords:
    """越权：user2 操作 user1 会话全拒（真归属场景）"""

    def test_pause_foreign_record_rejected(self):
        t1 = _front_token()
        rid = _start(t1, 1)["record_id"]
        t2 = _register_user()
        r = _post(t2, "/api/front/reading/pause", {"record_id": rid})
        assert r.get("code") != 200, "他人 pause 应被拒"
        _post(t1, "/api/front/reading/end", {"record_id": rid})

    def test_record_delete_foreign_rejected(self):
        t1 = _front_token()
        rid = _start(t1, 1)["record_id"]
        _post(t1, "/api/front/reading/end", {"record_id": rid})
        t2 = _register_user()
        r = _post(t2, "/api/front/reading/record/delete", {"record_id": rid})
        assert r.get("code") != 200, "他人删记录应被拒"
        _post(t1, "/api/front/reading/record/delete", {"record_id": rid})  # 本人可删

    def test_pin_foreign_bookshelf_rejected(self):
        t1 = _front_token()
        _ensure_shelf(t1, 3)
        t2 = _register_user()
        r = _post(t2, "/api/front/bookshelf/pin", {"book_id": 3, "pinned": 1})
        assert r.get("code") != 200, "他人置顶应被拒（user2 书架无此书）"


class TestGoal:
    """G1/G2 每日目标读写+边界"""

    def test_goal_default_30_then_roundtrip(self):
        token = _register_user()  # 新用户无配置行→兜底 30
        g = _get(token, "/api/front/reading/goal")["data"]
        assert g["daily_goal"] == 30, "无配置应兜底 30"

        _check(_post(token, "/api/front/reading/goal", {"minutes": 45}))
        assert _get(token, "/api/front/reading/goal")["data"]["daily_goal"] == 45
        _check(_post(token, "/api/front/reading/goal", {"minutes": 60}))  # upsert
        assert _get(token, "/api/front/reading/goal")["data"]["daily_goal"] == 60

    def test_goal_bounds(self):
        token = _front_token()
        for bad in (9, 241, 0, -1):
            r = _post(token, "/api/front/reading/goal", {"minutes": bad})
            assert r.get("code") != 200, f"{bad} 分钟应被拒"
        for good in (10, 240):
            _check(_post(token, "/api/front/reading/goal", {"minutes": good}))
        _check(_post(token, "/api/front/reading/goal", {"minutes": 30}))  # 还原


class TestBookshelfPinDelete:
    """S2 移出+S3 置顶"""

    def test_pin_toggle_and_order(self):
        token = _front_token()
        _ensure_shelf(token, 3)
        _check(_post(token, "/api/front/bookshelf/pin", {"book_id": 3, "pinned": 1}))

        lst = _get(token, "/api/front/bookshelf/list")["data"]["lists"]
        mine = [b for b in lst if b["book_id"] == 3]
        assert mine and mine[0]["pinned"] == 1, "置顶标记应返回"
        if len(lst) > 1:
            assert lst[0]["book_id"] == 3, "置顶项应排首位（pinned DESC）"
        _check(_post(token, "/api/front/bookshelf/pin", {"book_id": 3, "pinned": 0}))

    def test_delete_then_readd(self):
        token = _front_token()
        _ensure_shelf(token, 4)
        _check(_post(token, "/api/front/bookshelf/delete", {"book_id": 4}))
        lst = _get(token, "/api/front/bookshelf/list")["data"]["lists"]
        assert all(b["book_id"] != 4 for b in lst), "移出后不应在列表"
        _check(_post(token, "/api/front/bookshelf/add", {"book_id": 4}))  # 重加恢复

    def test_delete_not_in_shelf(self):
        token = _register_user()
        r = _post(token, "/api/front/bookshelf/delete", {"book_id": 1})
        assert r.get("code") != 200, "空书架移出应失败"


class TestCheckinAndOverview:
    """T1 checkin 月参数化 + T2 overview goal 键"""

    def test_checkin_default_month(self):
        r = _get(_front_token(), "/api/front/stats/checkin")
        _check(r)
        assert "dates" in r["data"] and "total_days" in r["data"]

    def test_checkin_explicit_month_shape(self):
        r = _get(_front_token(), "/api/front/stats/checkin", {"month": "2026-11"})
        _check(r)
        for d in r["data"]["dates"]:
            assert d.startswith("2026-11"), f"月过滤不纯: {d}"

    def test_overview_has_goal(self):
        data = _get(_front_token(), "/api/front/stats/overview")["data"]
        for key in ("today", "week", "month", "total", "goal"):
            assert key in data, f"overview 缺字段: {key}"
        assert 10 <= data["goal"] <= 240
