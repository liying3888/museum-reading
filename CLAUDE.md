# 博物馆读书小程序 - 开发指南

## 项目概述

- **项目名称：** 荣河读书（博物馆读书小程序）
- **技术栈：** Java 8 + Spring Boot 2.7.5 + Vue 3 + uni-app
- **基础框架：** likeadmin_java
- **开发周期：** 5天（2026-03-14 ~ 2026-03-18）

## 项目结构

```
server/
├── like-admin/     # 管理后台API（端口8082）
├── like-front/     # 前台API
├── like-common/    # 公共模块
└── like-generator/ # 代码生成器

uniapp/            # 小程序端
admin/             # 管理后台前端（Vue3）
```

## 已完成功能（Day 1）

### 数据库表
- la_book（书籍表）
- la_bookshelf（书架表）
- la_reading_record（阅读记录表）

### 后端API
- BookController（书籍管理）
- BookService（业务逻辑）
- BookMapper（数据访问）

### 前端页面
- pages/index/index.vue（首页）
- pages/reading/timer.vue（计时器）
- pages/bookshelf/index.vue（书架）

## 待开发功能

### Day 2（2026-03-15）
- [ ] 书架完整功能（加入/更新进度/状态切换）
- [ ] 书籍详情页
- [ ] 阅读统计API

### Day 3（2026-03-16）
- [ ] 个人中心
- [ ] 阅读统计展示
- [ ] 进度可视化

### Day 4（2026-03-17）
- [ ] 管理后台书籍管理
- [ ] 分类管理

### Day 5（2026-03-18）
- [ ] 联调测试
- [ ] Bug修复
- [ ] 部署

## 开发规范

### Java后端
- 使用 @RestController + @RequestMapping
- 返回 AjaxResult 统一格式
- 使用 MyBatis-Plus 查询
- 参考 likeadmin 现有Controller写法

### Vue前端
- 使用 Vue 3 + TypeScript
- 使用 Composition API（setup语法）
- 调用API使用 @/api/*.ts

## API基础路径

- 管理后台：/api/admin/
- 前台：/api/front/

## 数据库连接

- MySQL：localhost:3306
- Redis：localhost:6379
- 数据库：likeadmin_java
- 账号：root / root
