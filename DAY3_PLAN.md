# Day 3 开发计划 - 阅读统计与数据可视化

> **日期：** 2026-03-14
> **目标：** 完成个人中心、阅读统计、进度可视化

## 一、任务清单

### 1. 后端任务（优先级：P0）

#### 1.1 创建统计服务 (ReadingStatsService)
- [x] 今日阅读时长统计
- [x] 本周阅读时长统计
- [x] 本月阅读时长统计
- [x] 累计阅读统计
- [x] 连续打卡天数
- [x] 阅读排行榜

#### 1.2 完善个人中心API (UserController)
- [x] 获取用户信息
- [x] 获取用户统计数据
- [x] 更新用户信息

### 2. 前端任务（优先级：P0）

#### 2.1 个人中心页面完善 (pages/user/center.vue)
- [x] 用户信息展示
- [x] 阅读统计卡片
- [x] 功能入口列表
- [x] 集成统计数据API

#### 2.2 阅读统计页面 (pages/stats/index.vue)
- [x] 今日/本周/本月统计
- [x] 阅读时长图表
- [x] 阅读记录列表
- [x] 打卡日历

#### 2.3 进度可视化组件
- [x] 环形进度条组件
- [x] 柱状图组件
- [x] 折线图组件

### 3. 数据库优化
- [x] 添加统计索引
- [x] 测试数据生成

## 二、技术方案

### 统计API设计

```
GET /api/front/stats/today        - 今日统计
GET /api/front/stats/week         - 本周统计
GET /api/front/stats/month        - 本月统计
GET /api/front/stats/total        - 累计统计
GET /api/front/stats/checkin      - 打卡记录
GET /api/front/stats/ranking      - 排行榜
```

### 前端图表库
- 使用 uCharts（uni-app 推荐图表库）
- 轻量级，性能好
- 支持多种图表类型

## 三、开发步骤

### Step 1: 创建统计服务（后端）
1. 创建 ReadingStatsService.java
2. 实现统计算法
3. 创建 StatsController
4. 测试API

### Step 2: 完善个人中心（前端）
1. 完善 center.vue
2. 集成统计数据
3. 优化UI样式

### Step 3: 创建统计页面
1. 创建 stats/index.vue
2. 集成图表组件
3. 实现数据可视化

### Step 4: 测试与优化
1. 单元测试
2. 集成测试
3. 性能优化

## 四、预期成果

### 后端
- ReadingStatsService.java（统计服务）
- StatsController.java（统计API）
- UserController.java（用户API）

### 前端
- pages/user/center.vue（个人中心）
- pages/stats/index.vue（统计页面）
- components/charts/*（图表组件）

### 文档
- API文档更新
- 组件使用文档

## 五、时间安排

- 01:40 - 02:30: 后端统计服务（50分钟）
- 02:30 - 03:20: 前端统计页面（50分钟）
- 03:20 - 03:50: 集成测试（30分钟）
- 03:50 - 04:00: 文档更新（10分钟）

---

**开始时间：** 2026-03-14 01:40
**预计完成：** 2026-03-14 04:00
