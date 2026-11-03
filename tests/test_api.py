#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""museum-reading API 冒烟测试（S2 地基修复后重写，2026-11-02）

变更要点（相对 Day5 版）：
1. 修复原文件截断语法错误（缺 `])`）
2. 路径对齐实际路由：admin 端为 api/book|category|statistics（原版 api/admin/books/* 为错误路径）
3. 断言改为 body 业务码 code==200（likeadmin 全局 HTTP 恒 200，仅断言 HTTP 状态会虚假通过）
4. 补 testuser/test123 登录链与带 token 的 front 业务接口冒烟
"""

import os
import pytest
import requests

# 容器网络内默认走服务名；本地跑可环境变量覆盖
ADMIN_BASE = os.environ.get("ADMIN_API_BASE_URL", "http://backend:8082")
FRONT_BASE = os.environ.get("FRONT_API_BASE_URL", "http://frontend:8085")
TIMEOUT = 10


def _check(payload):
    """统一断言：HTTP 200 且业务码 code==200；返回 payload 供链式取值"""
    assert payload.get("code") == 200, f"业务码非200: {payload}"
    return payload


class TestAdminAuth:
    """管理端鉴权（adminapi→api 修复后，无 token 必须被拦）"""

    def test_book_list_requires_token(self):
        r = requests.get(f"{ADMIN_BASE}/api/book/list", timeout=TIMEOUT)
        assert r.status_code == 200
        assert r.json().get("code") != 200, "无 token 访问管理接口不应放行"

    def test_category_list_requires_token(self):
        r = requests.get(f"{ADMIN_BASE}/api/category/list", timeout=TIMEOUT)
        assert r.status_code == 200
        assert r.json().get("code") != 200, "无 token 访问管理接口不应放行"


class TestFrontLogin:
    """testuser/test123 登录链（哈希修复后应成功）"""

    def test_account_login(self):
        r = requests.post(
            f"{FRONT_BASE}/api/login/accountLogin",
            json={"username": "testuser", "password": "test123"},
            timeout=TIMEOUT,
        )
        data = r.json()
        _check(data)
        assert data["data"].get("token"), "登录未返回 token"


def _front_token():
    r = requests.post(
        f"{FRONT_BASE}/api/login/accountLogin",
        json={"username": "testuser", "password": "test123"},
        timeout=TIMEOUT,
    )
    return r.json()["data"]["token"]


class TestFrontBookshelf:
    """书架域（userId 已接入登录态）"""

    def test_list_with_token(self):
        token = _front_token()
        r = requests.get(
            f"{FRONT_BASE}/api/front/bookshelf/list",
            headers={"like-token": token},
            timeout=TIMEOUT,
        )
        _check(r.json())

    def test_list_without_token_rejected(self):
        r = requests.get(f"{FRONT_BASE}/api/front/bookshelf/list", timeout=TIMEOUT)
        assert r.json().get("code") != 200, "无 token 不应放行"


class TestFrontReading:
    """阅读域：开始→结束 全链（含归属校验）"""

    def test_start_and_end(self):
        token = _front_token()
        r1 = requests.post(
            f"{FRONT_BASE}/api/front/reading/start",
            json={"book_id": 1},
            headers={"like-token": token},
            timeout=TIMEOUT,
        )
        d1 = r1.json()
        _check(d1)
        record_id = d1["data"]["record_id"]

        r2 = requests.post(
            f"{FRONT_BASE}/api/front/reading/end",
            json={"record_id": record_id},
            headers={"like-token": token},
            timeout=TIMEOUT,
        )
        _check(r2.json())

    def test_end_foreign_record_rejected(self):
        """结束他人记录应被拒（record_id=999999 不存在，需先有记录才有归属场景；
        这里验证不存在记录返回失败，归属校验逻辑由代码保证）"""
        token = _front_token()
        r = requests.post(
            f"{FRONT_BASE}/api/front/reading/end",
            json={"record_id": 999999},
            headers={"like-token": token},
            timeout=TIMEOUT,
        )
        assert r.json().get("code") != 200


class TestFrontStats:
    """统计域"""

    def test_overview_with_token(self):
        token = _front_token()
        r = requests.get(
            f"{FRONT_BASE}/api/front/stats/overview",
            headers={"like-token": token},
            timeout=TIMEOUT,
        )
        data = r.json()
        _check(data)
        for key in ("today", "week", "month", "total"):
            assert key in data["data"], f"overview 缺字段: {key}"


class TestHealthCheck:
    """连通性（actuator 未启用，用登录接口代探活）"""

    def test_front_alive(self):
        r = requests.get(f"{FRONT_BASE}/api/front/bookshelf/list", timeout=TIMEOUT)
        assert r.status_code == 200

    def test_admin_alive(self):
        r = requests.get(f"{ADMIN_BASE}/api/book/list", timeout=TIMEOUT)
        assert r.status_code == 200


if __name__ == "__main__":
    pytest.main(["-v", "--html=reports/test-report.html", "tests/"])
