# 博物馆小程序测试文档

## 测试用户

### 前端用户（小程序/H5）

- **用户名**: testuser
- **密码**: test123
- **用户 ID**: 1

#### 登录示例

```bash
curl -X POST http://100.77.128.110:8085/api/login/accountLogin \
  -H 'Content-Type: application/json' \
  -d '{"username":"testuser","password":"test123"}'
```

响应示例：
```json
{
  "code": 200,
  "msg": "成功",
  "data": {
    "id": 1,
    "isBindMobile": true,
    "token": "guhO52qmRhzN6xfGIcpV3ln2YkE3sPd4NWmZK2lgY53LANusC0xLiJ2Wl1YaFecu",
    "isNew": 1
  }
}
```

#### 使用 Token 访问 API

```bash
curl -H 'like-token: YOUR_TOKEN_HERE' \
  http://100.77.128.110:8085/api/user/info
```

## API 端点

### 前端 API (端口 8085)
- 基础路径: http://100.77.128.110:8085/api/
- 示例:
  - 首页: GET /api/index/index
  - 登录: POST /api/login/accountLogin
  - 用户信息: GET /api/user/info (需要 token)
  - 书架列表: GET /api/bookshelf/lists (需要 token)

### 管理后台 API (端口 8082)
- 基础路径: http://100.77.128.110:8082/adminapi/
- 示例:
  - 分类列表: GET /adminapi/category/list
  - 书籍列表: GET /adminapi/books/list

## 前端访问

### 小程序/H5
- URL: http://100.77.128.110:8083/
- API: http://100.77.128.110:8085/api/

### 管理后台
- URL: http://100.77.128.110:8084/
- API: http://100.77.128.110:8082/adminapi/

## 数据库

### 连接信息
- 主机: localhost (容器内)
- 端口: 3306
- 数据库: likeadmin_java
- 用户名: root
- 密码: root

### 连接命令
```bash
docker exec -it museum-mysql mysql -uroot -proot likeadmin_java
```

### 主要表
- la_user: 用户表
- la_user_auth: 用户授权表
- la_book: 书籍表
- la_book_category: 书籍分类表

## Docker 容器

### 查看运行状态
```bash
docker ps | grep museum
```

### 查看日志
```bash
docker logs museum-backend
docker logs museum-frontend
docker logs museum-admin
docker logs museum-miniapp
```

### 重启服务
```bash
cd ~/openclaw-projects/museum-reading/code
docker-compose -f docker-compose-full.yml restart
```

### 容器列表
- museum-mysql: 数据库 (3306)
- museum-redis: 缓存 (6379)
- museum-backend: 管理后台 API (8082)
- museum-frontend: 前端 API (8085)
- museum-admin: 管理后台前端 (8084)
- museum-miniapp: 小程序/H5 前端 (8083)

## 测试清单

### 前端测试
- [x] 用户登录
- [x] 获取首页数据
- [ ] 获取用户信息
- [ ] 获取书架列表
- [ ] 获取阅读记录

### 管理后台测试
- [x] 获取分类列表
- [ ] 添加/编辑分类
- [ ] 获取书籍列表
- [ ] 添加/编辑书籍

### 性能测试
- [ ] API 响应时间
- [ ] 并发请求测试
- [ ] 数据库查询优化
