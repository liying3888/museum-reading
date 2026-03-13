# 博物馆阅读小程序

基于 likeadmin-java 开发的博物馆阅读小程序系统。

## 项目简介

这是一个为博物馆设计的阅读小程序系统，包含：
- 📱 微信小程序/H5 前端
- 💻 管理后台
- 🔌 前端 API 服务
- 🔌 管理后台 API 服务

## 技术栈

### 后端
- Java 8
- Spring Boot 2.7.5
- MyBatis Plus
- MySQL 5.7
- Redis 7.0
- Sa-Token (权限认证)

### 前端
- Vue 3 + TypeScript
- Vite 2
- Element Plus (管理后台)
- UniApp (小程序/H5)

## 快速开始

### 环境要求
- Docker & Docker Compose
- Git

### 1. 克隆项目

```bash
git clone https://github.com/museum-reading-team/museum-reading.git
cd museum-reading/code
```

### 2. 配置环境

#### 后端配置
```bash
cd server
cp like-admin/src/main/resources/application-dev-example.yml \
   like-admin/src/main/resources/application-dev.yml
# 编辑 application-dev.yml 配置数据库等
```

#### 前端配置
```bash
# 小程序配置
cd ../uniapp
cp .env.production.example .env.production
# 编辑 .env.production 设置 VITE_APP_BASE_URL

# 管理后台配置
cd ../admin
cp .env.production.example .env.production
# 编辑 .env.production 设置 VITE_APP_BASE_URL
```

### 3. Docker 部署

```bash
cd ..
docker-compose -f docker-compose-full.yml up -d
```

### 4. 访问系统

- 小程序/H5: http://YOUR_IP:8083/
- 管理后台: http://YOUR_IP:8084/
- 前端 API: http://YOUR_IP:8085/api/
- 管理后台 API: http://YOUR_IP:8082/adminapi/

## 开发环境

### 测试服务器
- SSH: `ssh leey@100.77.128.110`
- 项目路径: `~/openclaw-projects/museum-reading/code/`

### 测试账号

#### 前端用户
- 用户名: `testuser`
- 密码: `test123`

#### 管理后台
- 用户名: `admin`
- 密码: `123456`

详细测试文档请查看: [TESTING.md](./TESTING.md)

## 项目结构

```
code/
├── admin/              # 管理后台前端 (Vue 3)
├── uniapp/             # 小程序/H5 前端 (UniApp)
├── server/             # 后端服务 (Spring Boot)
│   ├── like-admin/     # 管理后台 API
│   ├── like-front/     # 前端 API
│   ├── like-common/    # 公共模块
│   └── like-generator/ # 代码生成器
├── pc/                 # PC 端前端 (未使用)
├── docker/             # Docker 配置
├── nginx/              # Nginx 配置
├── sql/                # 数据库脚本
└── tests/              # 测试文件
```

## 主要功能

### 前端功能
- ✅ 用户登录/注册
- ✅ 首页展示
- ✅ 书籍分类浏览
- ✅ 书籍详情
- ✅ 在线阅读
- ✅ 书架管理
- ✅ 阅读记录

### 管理后台功能
- ✅ 分类管理
- ✅ 书籍管理
- ✅ 用户管理
- ✅ 数据统计

## API 文档

### 前端 API

#### 认证相关
- `POST /api/login/accountLogin` - 账号登录
- `POST /api/login/register` - 用户注册

#### 用户相关
- `GET /api/user/info` - 获取用户信息

#### 书籍相关
- `GET /api/bookshelf/lists` - 获取书架列表
- `GET /api/books/list` - 获取书籍列表
- `GET /api/books/detail` - 获取书籍详情

### 管理后台 API

#### 分类管理
- `GET /adminapi/category/list` - 获取分类列表
- `POST /adminapi/category/add` - 添加分类
- `POST /adminapi/category/edit` - 编辑分类
- `POST /adminapi/category/del` - 删除分类

#### 书籍管理
- `GET /adminapi/books/list` - 获取书籍列表
- `POST /adminapi/books/add` - 添加书籍
- `POST /adminapi/books/edit` - 编辑书籍
- `POST /adminapi/books/del` - 删除书籍

## 数据库

### 主要表结构
- `la_user` - 用户表
- `la_user_auth` - 用户授权表
- `la_book` - 书籍表
- `la_book_category` - 书籍分类表
- `la_bookshelf` - 书架表
- `la_reading_record` - 阅读记录表

## 部署

### Docker 部署（推荐）

```bash
# 构建并启动所有服务
docker-compose -f docker-compose-full.yml up -d --build

# 查看日志
docker-compose -f docker-compose-full.yml logs -f

# 停止服务
docker-compose -f docker-compose-full.yml down
```

### 手动部署

详细步骤请查看: [DEPLOYMENT.md](./DEPLOYMENT.md)

## 开发指南

### 后端开发
```bash
cd server
mvn clean install
mvn spring-boot:run
```

### 前端开发
```bash
# 小程序
cd uniapp
npm install
npm run dev:h5

# 管理后台
cd admin
npm install
npm run dev
```

## 常见问题

### 1. API 请求 404
检查 API 路径是否正确：
- 前端 API: `/api/xxx`
- 管理后台 API: `/adminapi/xxx`

### 2. Token 认证失败
确保请求头中包含正确的 token：
```bash
curl -H 'like-token: YOUR_TOKEN' http://...
```

### 3. Docker 容器无法启动
检查端口是否被占用：
```bash
netstat -tunlp | grep -E '8082|8083|8084|8085|3306|6379'
```

## 开发进度

- [x] Day 1-2: 项目初始化和环境搭建
- [x] Day 3: 后端基础功能开发
- [x] Day 4: 前端页面开发
- [x] Day 5: 测试优化
  - [x] 前端 API 配置
  - [x] 测试用户创建
  - [x] 环境变量配置
  - [x] 测试文档编写
  - [x] README 更新

## 贡献

欢迎提交 Issue 和 Pull Request！

## 许可证

本项目基于 MIT 协议开源。

## 联系方式

- GitHub: https://github.com/museum-reading-team

---

基于 [likeadmin-java](https://github.com/likeadmin-likeshop/likeadmin_java) 开发
