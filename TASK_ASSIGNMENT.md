# 博物馆读书小程序 - Day 2 任务分配

> **日期：** 2026-03-14 01:05
> **组长：** 组长-博物馆 👨‍💼
> **目标：** Day 2 核心功能开发

---

## 一、任务分配表

### 开发-博物馆-1（前端）💻

**优先级：P0**

| 任务 | 预计时间 | 交付物 |
|------|----------|--------|
| 书籍详情页 | 1h | pages/book/detail.vue |
| 个人中心页 | 1h | pages/user/center.vue |
| 前端API完善 | 0.5h | api/bookshelf.ts, api/reading.ts |

**启动命令：**
```bash
cd ~/openclaw-projects/museum-reading/code
/home/leey/.npm-global/bin/claude
# 然后输入：请创建书籍详情页 pages/book/detail.vue，参考 CLAUDE.md 中的开发规范
```

---

### 开发-博物馆-2（后端）💻

**优先级：P0**

| 任务 | 预计时间 | 交付物 |
|------|----------|--------|
| 书架Controller | 1h | BookshelfController.java |
| 阅读Controller | 1h | ReadingController.java |
| 统计Service | 0.5h | ReadingStatsService.java |

**启动命令：**
```bash
cd ~/openclaw-projects/museum-reading/code
/home/leey/.npm-global/bin/claude
# 然后输入：请创建书架Controller，包含list/add/updateProgress API
```

---

### 测试-博物馆 🐛

**任务：**
- Day 2 结束时进行集成测试
- 准备测试用例文档

---

### 文档-博物馆 📝

**任务：**
- 更新 API 文档
- 编写部署文档

---

## 二、开发环境

### 后端启动
```bash
cd ~/openclaw-projects/museum-reading/code/server
mvn spring-boot:run -pl like-admin
```

### 前端启动
```bash
cd ~/openclaw-projects/museum-reading/code/uniapp
npm run dev:mp-weixin
```

### 管理后台启动
```bash
cd ~/openclaw-projects/museum-reading/code/admin
npm run dev
```

---

## 三、Claude Code 提示词

### 后端开发提示词
```
我是开发-博物馆-2，负责后端开发。

请帮我完成以下任务：

1. 创建 BookshelfController（书架管理）
   - GET /api/front/bookshelf/list - 我的书架（参数：status）
   - POST /api/front/bookshelf/add - 加入书架（参数：book_id）
   - POST /api/front/bookshelf/progress - 更新进度（参数：book_id, current_page）
   - POST /api/front/bookshelf/status - 更新状态（参数：book_id, status）

2. 创建 ReadingController（阅读管理）
   - POST /api/front/reading/start - 开始计时（参数：book_id?）
   - POST /api/front/reading/end - 结束计时（参数：record_id）
   - GET /api/front/reading/stats - 阅读统计
   - GET /api/front/reading/records - 阅读记录

3. 创建 ReadingStatsService（统计服务）
   - 统计今日/本周/本月阅读时长
   - 统计连续打卡天数

参考：
- CLAUDE.md 项目说明
- .claude/skills.md 开发规范
- server/like-admin/src/main/java/com/mdd/admin/controller/books/BookController.java
```

### 前端开发提示词
```
我是开发-博物馆-1，负责小程序前端开发。

请帮我完成以下任务：

1. 创建书籍详情页 pages/book/detail.vue
   - 显示书籍信息（封面、书名、作者、简介）
   - 加入书架按钮
   - 开始阅读按钮
   - 进度条（如果已在书架）

2. 创建个人中心页 pages/user/center.vue
   - 用户信息展示
   - 阅读统计（累计时长、已读书籍、连续打卡）
   - 功能入口（我的书架、我的笔记、设置）

3. 创建前端API文件
   - api/bookshelf.ts（书架相关API）
   - api/reading.ts（阅读相关API）

参考：
- CLAUDE.md 项目说明
- .claude/skills.md 开发规范
- uniapp/src/pages/index/index.vue
- uniapp/src/api/book.ts
```

---

## 四、部署计划

### 部署时间
- **Day 2 结束时（预计今晚 23:00）**
- **Day 3 中期（预计明天下午 15:00）**
- **Day 5 最终部署（预计 3/18 晚上）**

### 部署内容
1. 后端服务部署到 Ubuntu
2. 小程序上传到微信开发者工具
3. 管理后台部署到 Nginx

---

## 五、测试计划

### Day 2 测试用例

#### 书架功能测试
- [ ] 加入书架
- [ ] 查看书架列表
- [ ] 更新阅读进度
- [ ] 切换书籍状态

#### 阅读计时测试
- [ ] 开始计时
- [ ] 暂停计时
- [ ] 结束计时
- [ ] 查看阅读记录

#### 统计功能测试
- [ ] 今日阅读统计
- [ ] 累计阅读统计
- [ ] 连续打卡统计

---

**任务分配完成，各岗位立即开始执行！**

**组长签字：** 组长-博物馆 👨‍💼
**时间：** 2026-03-14 01:05
