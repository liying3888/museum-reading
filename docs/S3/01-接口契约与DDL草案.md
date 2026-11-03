# S3 批 1 · 接口契约与 DDL 草案（技术设计）

- **主笔**：museum 岗（w3:p9）· 2026-11-04
- **状态**：**已定稿**（leader 2026-11-04 契约评审过闸；§6 四决策点全裁=①reading 域②聚合键③物理删+记录保留④id DESC 留迭代；唯一实质意见=paused_total 公式修正令，已修文并入 §1.1/§2.3 R5/§2.5/§5）
- **上游**：`docs/design/01-06`（S2 定稿，lee 22:4x 签）+ 后端地基 `9f8d780a`
- **下游**：批 2 后端实现（本契约为准）+ 批 3 C 端落码 + tests 扩充
- **依据注记**：02 规格 §0.3 计时口径/§5.4 会话重入/§5.1 状态机；01 §7 API 清单；06 §5 书库排序口径

---

## 0. 通用约定

- 路径前缀 `api/front/**`（front 登录态）；响应=底座 `AjaxResult`（HTTP 恒 200，业务码在 body：`code=200` 成功，失败 `code≠200`+`msg`）
- 参数命名：body JSON 与 query 均 **snake_case**（对齐存量 `book_id`/`current_page` 惯例）
- 时间：库内 INT 秒级时间戳（存量惯例）；**统计/打卡日界=东八区**（`ZoneId.of("Asia/Shanghai")` 显式，修 S1 UTC 串日，jnpf 时区三问同源条款）
- 归属校验：一切按 record_id/book_id 的写操作先验 `user_id` 归属（地基批次 end 接口同款，越权返 `failed("无权操作")`）
- 公开接口仅 books/list+detail（`@NotLogin`，书单公开可看——02 §1.3）

## 1. DDL 草案（migration：`sql/s3_migration.sql`，5 项）

### 1.1 `la_reading_record` 加 3 列（真暂停+自动结束承载，02 §0.3/§5.1）

```sql
ALTER TABLE la_reading_record
  ADD COLUMN paused_at INT NOT NULL DEFAULT 0 COMMENT '当前暂停起点(0=非暂停态)' AFTER end_time,
  ADD COLUMN paused_total INT NOT NULL DEFAULT 0 COMMENT '已累计暂停秒数' AFTER paused_at,
  ADD COLUMN auto_closed TINYINT NOT NULL DEFAULT 0 COMMENT '是否自动结束: 0=正常, 1=240分钟自动结束' AFTER paused_total,
  ADD INDEX idx_open (end_time, start_time);  -- 240 扫描与进行中会话查询
```

**会话语义**（不加状态列，由现有列+新列推导，存量数据天然兼容）：

| 态 | 判定 | 净时长公式 |
|---|---|---|
| running | `end_time IS NULL AND paused_at=0` | `now − start − paused_total` |
| paused | `end_time IS NULL AND paused_at>0` | `paused_at − start − paused_total`（**冻结在暂停点**，不随墙钟增长——修正令 2026-11-04：paused_total 是累计暂停秒数非运行段，勿混） |
| finished | `end_time IS NOT NULL` | `end − start − paused_total`（paused 结束时先结算 pause 段入 paused_total） |

- 结束时若 `paused_at>0`：先 `paused_total += end − paused_at` 再清零落库（上表 finished 公式统一成立）
- **240 墙钟线**= `start_time + 14400`（暂停计入墙钟——02 §0.3 round3：暂停不能拖延自动结束）；到线结算净时长按上表 paused 行口径（02 §0.3：跨天归属取结束时刻所在日）

### 1.2 `la_bookshelf` 加置顶列（02 §4.1）

```sql
ALTER TABLE la_bookshelf
  ADD COLUMN pinned TINYINT NOT NULL DEFAULT 0 COMMENT '置顶: 0=否, 1=是' AFTER progress,
  ADD INDEX idx_user_status (user_id, status, update_time);  -- 三段 Tab 查询
```

- 排序口径：`ORDER BY pinned DESC, update_time DESC`（置顶优先，其余 update_time 倒序）

### 1.3 新表 `la_user_reading_config`（每日目标单一数据源，02 §0.1）

```sql
CREATE TABLE IF NOT EXISTS la_user_reading_config (
  id INT PRIMARY KEY AUTO_INCREMENT,
  user_id INT NOT NULL COMMENT '用户ID',
  daily_goal INT NOT NULL DEFAULT 30 COMMENT '每日目标分钟数(10-240)',
  create_time INT,
  update_time INT,
  UNIQUE KEY uk_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- **不动底座 `la_user`**（likeadmin 表保持升级路径）；无记录=默认 30（读取时兜底，不预建行）

### 1.4 存量数据处理

- 全部 `ADD COLUMN ... DEFAULT`：存量行自动取缺省（paused_at=0/paused_total=0/auto_closed=0/pinned=0），**无回填脚本**；存量已结束记录语义不变。

## 2. 接口契约（front 端）

### 2.1 books 域【新增，@NotLogin 公开】`api/front/books`

| # | 接口 | 入参 | 出参（data 内） | 备注 |
|---|---|---|---|---|
| B1 | GET `/list` | `page_no`(1)/`page_size`(15)/`category_id?`/`keyword?`（书名/作者模糊） | `{count, lists:[{id,title,author,publisher,cover,pages,rating,category_id,category_name}]}` | 仅 `status=1` 上架；排序 `id DESC`（06 §5：上架时间倒序+录入序 tie-break；首期 create_time 与 id 同序，运营置顶位留迭代） |
| B2 | GET `/detail` | `id` | B1 字段全集 + `intro` + `status` | 公开（未登录可看详情，加入书架才需登录） |

分类源：`category_id` 直传（分类常量前端已缓存分类列表——B1 返回聚合 `category_name`；分类列表接口用 admin 侧既有 `api/category/list` 的公开版？**不另开**：书库 chips 首期走 B1 首屏返回的 `categories:[{id,name}]` 聚合键（一次请求带出），切换走 B1 过滤）

### 2.2 bookshelf 域【2 新增 1 改造】`api/front/bookshelf`

| # | 接口 | 入参 | 出参 | 备注 |
|---|---|---|---|---|
| S1 | GET `/list`（改造） | `page_no`/`page_size`/`status?`（0/1/2，不传=全量——详情页在架判定用） | `{count, lists:[{id,book_id,status,current_page,progress,pinned,finish_time,update_time, book:{...B1 字段}}]}` | 排序改 `pinned DESC, update_time DESC`；返 `pinned`+联表 book 信息（修 S1 页面二次请求） |
| S2 | POST `/delete`【新增】 | `book_id` | `{}` | 物理删书架行（uk 唯一）；**la_reading_record 阅读记录保留**（02 §3.2：记录是用户资产）；归属校验 |
| S3 | POST `/pin`【新增】 | `book_id`/`pinned`(0\|1) | `{}` | 置顶切换（长按置顶 02 §4.1）；归属校验 |

既有 `/add` `/progress` `/status` 三接口不动（地基已接登录态）。`/progress` 行为补一条：达到总页数时**前端**弹确认转已读（02 §3.2），后端不自动转（progress 接口仅存页码+算百分比；status 切换走既有 `/status`）。

### 2.3 reading 域【3 新增 3 改造】`api/front/reading`

| # | 接口 | 入参 | 出参 | 备注 |
|---|---|---|---|---|
| R1 | POST `/start`（改造：幂等） | `book_id?`（空=自由阅读） | `{record_id, book_id, book_title, start_time, paused_at, paused_total, resumed}` | **存在未结束会话（end_time IS NULL）→ 返回该会话+`resumed:true`，不新建不串页码**（02 §5.4）；无则新建返回 `resumed:false`。前端按 `resumed && book_id≠请求值` 分支异书重入提示 |
| R2 | POST `/pause`【新增】 | `record_id` | `{}` | `paused_at=now`；已 paused 幂等忽略；归属校验 |
| R3 | POST `/resume`【新增】 | `record_id` | `{}` | `paused_total += now−paused_at; paused_at=0`；running 幂等忽略；归属校验 |
| R4 | POST `/end`（改造：真暂停结算+成果数据） | `record_id` | `{duration, today_total, streak_days, goal, goal_gap, auto_closed}` | duration=净时长秒（§1.1 公式，paused 结束先结算 pause 段）；成果弹层三行小结数据源（02 §5.3）；重复结束幂等返回既有记录结果不产生双记录（02 §0.3）；`auto_closed` 供前端标注 |
| R5 | GET `/session`【新增】 | — | `{record_id, book_id, book_title, start_time, paused_at, paused_total, net_duration} \| null` | 当前进行中会话查询（首页/书架「回到计时」入口联动+onShow 恢复态）；`net_duration`=服务端按 §1.1 口径算好的净时长秒（paused 态冻结不随墙钟增长——修正令），前端免重复实现公式 |
| R6 | POST `/record/delete`【新增】 | `record_id` | `{}` | 记录管理异常删除（02 §0.3③）；物理删；今日/本周累计天然重算（stats 均 SUM 实时聚合）；归属校验 |

既有 GET `/records`（分页）不动，补返 `auto_closed`/`book_title`/`duration` 字段（记录管理列表展示）。

### 2.4 stats+goal 域【1 参数化 1 补键 2 新增】`api/front/stats` · `api/front/reading/goal`

| # | 接口 | 入参 | 出参 | 备注 |
|---|---|---|---|---|
| T1 | GET `/checkin`（参数化） | `month?`（`yyyy-MM`，缺省当月） | `{dates:["2026-11-01",...], total_days}` | 月历增量请求（02 §6.2）；日界东八区（修 S1 UTC 串日）；月份范围前端控（注册月~当月） |
| T2 | GET `/overview`（补键） | — | 既有 today/week/month/total + **`goal`** | 首页目标环一次拿全（today.minutes/goal 同源） |
| G1 | GET `/reading/goal`【新增】 | — | `{daily_goal}` | 无配置行兜底 30 |
| G2 | POST `/reading/goal`【新增】 | `minutes`(10-240) | `{}` | 越界校验 `failed("目标需在 10-240 分钟之间")`；upsert |

### 2.5 240 分钟自动结束【新增：@Scheduled 任务】

- `ReadingAutoCloseService`：`@Scheduled(fixedDelay=300_000)` 扫 `end_time IS NULL AND start_time+14400 <= now(东八区墙钟)` → 逐条结算（**paused 行净时长=paused_at−start−paused_total（§1.1 冻结口径，同源修正令）**，running 行=now−start−paused_total）落 `end_time=now`/`duration`/`auto_closed=1`
- 与 R4 双通道幂等：前端在线时 end 调用与扫描竞争由「end_time IS NULL 才更新」行级条件保证（`UPDATE ... WHERE id=? AND end_time IS NULL`，影响行数 0=已被结算，R4 幂等返回）
- 120 分钟提醒=纯前端行为（无接口）

## 3. admin 侧（无新接口）

- 13 接口地基已就位（`9f8d780a`，api/ 前缀+satoken 鉴权），批 5 admin 前端联调
- 批 5 配菜单+权限码（`book:list`/`book:add`... 走 likeadmin 权限体系）

## 4. C 端调用映射（api 层重写对照，批 1 前端地基）

| api 文件 | 域 | 改动 |
|---|---|---|
| `api/book.ts` | books+分类 | 重写：B1/B2（去手写 `/api` 前缀——拦截器 urlPrefix 已拼，S1 双前缀 404 病根） |
| `api/bookshelf.ts` | bookshelf | 重写：S1-S3+add/progress/status |
| `api/reading.ts` | reading | 重写：R1-R6+goal G1/G2 |
| `api/stats.ts` | stats | 重写：T1/T2+today/week/month/total |
| `api/user.ts` | 底座用户 | 不动（底座登录/资料） |

## 5. 测试要点（批 2 tests 扩充对照，验收单素材）

- B1：keyword 命中书名/作者两路；category 过滤+未上架不可见；分页
- R1 幂等：A 会话未结束再 start（同书/异书/自由阅读三型）均返同一 record_id+resumed=true
- R2→R3→R4 链：暂停 60s 后结束，duration 不含暂停段（净时长公式）；R4 重复调用幂等
- **冻结断言（修正令补）**：paused 态期间 R5/session 返回的 `net_duration` 不随时间增长（隔 2s 两次查询值不变）
- 越权：user2 pause/resume/end/delete/pin user1 的记录全拒
- G2 边界：9/241 拒、10/240 过、upsert 覆盖
- 240 扫描：造 start_time=now-14500s 的 running+paused 两行，触发扫描→auto_closed=1+净时长口径分型验证
- T1：跨月参数+东八区日界（22:00 EST 案例勿漂日）

## 6. 待评审决策点（leader 定）

1. **goal 接口归属**：现定 `api/front/reading/goal`（阅读域内聚）；备选=并入 stats 或 user 域——影响不大，定了不改
2. **B1 聚合 categories 键**：书库 chips 数据源一次带出（省一次请求）vs 独立公开分类接口——现定前者
3. **S2 delete 物理删**：uk 约束下重加即新行（progress 归零）；备选软删（status=-1）会破坏「不传 status=全量」的在架判定——现定物理删+记录保留
4. **排序 id DESC 近似「上架时间倒序」**：la_book 无独立上架时间列（create_time 与录入序同源）；如需运营可控的「上架时间」需加列——首期不加，留迭代
