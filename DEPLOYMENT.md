# 博物馆读书小程序 - Docker部署指南

## 🐳 架构说明

所有服务都在Docker中运行：

| 服务 | 容器名 | 端口 | 说明 |
|------|--------|------|------|
| MySQL | museum-mysql | 3306 | 数据库 |
| Redis | museum-redis | 6379 | 缓存 |
| 后端 | museum-backend | 8082 | Spring Boot |
| 小程序 | museum-miniapp | 8083 | Nginx |
| 管理后台 | museum-admin | 8084 | Nginx |
| 测试 | museum-tester | - | 自动化测试 |

---

## 🚀 快速开始

### 1. 一键启动所有服务

```bash
cd ~/openclaw-projects/museum-reading/code
./start-all.sh
```

### 2. 自动化部署（推荐）

```bash
./auto-deploy.sh
```

**自动执行：**
1. ✅ 拉取最新代码
2. ✅ 构建/更新镜像
3. ✅ 启动所有服务
4. ✅ 等待服务健康
5. ✅ 运行自动化测试
6. ✅ 生成部署报告

### 3. 手动控制

```bash
# 启动所有服务
docker-compose -f docker-compose-full.yml up -d

# 查看服务状态
docker-compose -f docker-compose-full.yml ps

# 查看日志
docker-compose -f docker-compose-full.yml logs -f [服务名]

# 停止所有服务
docker-compose -f docker-compose-full.yml down

# 重启某个服务
docker-compose -f docker-compose-full.yml restart backend
```

---

## 📊 访问地址

| 服务 | 地址 | 账号 |
|------|------|------|
| 后端API | http://localhost:8082 | - |
| API文档 | http://localhost:8082/doc.html | - |
| 小程序 | http://localhost:8083 | - |
| 管理后台 | http://localhost:8084 | admin / 123456 |

---

## 🧪 自动化测试

### 运行测试

```bash
# 方式1：通过测试容器
docker-compose -f docker-compose-full.yml run tester

# 方式2：手动运行测试
cd tests
pytest -v --html=reports/test-report.html
```

### 测试报告

测试报告生成在：`tests/reports/test-report.html`

---

## 🔄 CI/CD 流程

### 自动触发

当执行 `git pull` 时，自动部署钩子会：
1. 检测代码更新
2. 自动执行部署
3. 运行测试
4. 生成报告

### 手动触发

```bash
# 拉取最新代码并部署
git pull origin feature/museum-reading
# 自动部署...

# 或手动触发
./auto-deploy.sh
```

---

## 🛠️ 开发流程

### 1. 开发新功能

```bash
# 在Ubuntu终端启动Claude Code
cd ~/openclaw-projects/museum-reading/code
~/start-claude-code.sh

# 让Claude Code帮你开发
"请创建XXX功能"
```

### 2. 本地测试

```bash
# 运行自动化测试
docker-compose -f docker-compose-full.yml run tester
```

### 3. 提交代码

```bash
git add .
git commit -m "feat: 新功能描述"
git push origin feature/museum-reading
```

### 4. 自动部署（在其他机器）

```bash
git pull
# 自动部署！
```

---

## 📝 常用命令

```bash
# 查看所有容器
docker ps -a

# 进入容器
docker exec -it museum-backend bash

# 查看后端日志
docker logs -f museum-backend

# 查看MySQL日志
docker logs -f museum-mysql

# 重启某个服务
docker restart museum-backend

# 查看容器资源使用
docker stats
```

---

## 🔧 故障排查

### 后端无法启动

```bash
# 查看日志
docker logs museum-backend

# 检查数据库连接
docker exec -it museum-mysql mysql -uroot -proot -e "SELECT 1"

# 检查Redis连接
docker exec -it museum-redis redis-cli ping
```

### 测试失败

```bash
# 查看测试日志
docker logs museum-tester

# 手动运行测试
cd tests
pytest -v test_api.py
```

---

## 💾 数据备份

```bash
# 备份MySQL
docker exec museum-mysql mysqldump -uroot -proot likeadmin_java > backup.sql

# 恢复MySQL
docker exec -i museum-mysql mysql -uroot -proot likeadmin_java < backup.sql
```

---

**部署完成后，所有服务都在Docker中运行！** 🎉
