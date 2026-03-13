#!/bin/bash

set -e

echo '╔══════════════════════════════════════════╗'
echo '║   修复Docker并部署所有服务        ║'
echo '╚══════════════════════════════════════════╝'
echo ''

# 1. 移除docker-compose.yml中的version字段（已废弃）
echo '### 1. 修复docker-compose配置 ###'
sed -i '/^version:/d' docker-compose-full.yml
echo '✅ 已移除废弃的version字段'

# 2. 启动基础服务（MySQL + Redis）
echo ''
echo '### 2. 启动基础服务 (MySQL + Redis) ###'
docker compose -f docker-compose-full.yml up -d mysql redis

echo ''
echo '⏳ 等待数据库启动 (30秒)...'
sleep 30

# 3. 检查数据库健康状态
echo ''
echo '### 3. 检查数据库状态 ###'
docker compose -f docker-compose-full.yml ps mysql redis

# 4. 构建并启动后端
echo ''
echo '### 4. 构建后端服务 (这可能需要5-10分钟) ###'
docker compose -f docker-compose-full.yml build backend
docker compose -f docker-compose-full.yml up -d backend

echo ''
echo '⏳ 等待后端启动 (60秒)...'
sleep 60

# 5. 构建并启动前端
echo ''
echo '### 5. 构建前端服务 ###'
docker compose -f docker-compose-full.yml build miniapp admin
docker compose -f docker-compose-full.yml up -d miniapp admin

# 6. 启动测试服务
echo ''
echo '### 6. 启动测试服务 ###'
docker compose -f docker-compose-full.yml build tester

# 7. 显示最终状态
echo ''
echo '### 7. 所有服务状态 ###'
docker compose -f docker-compose-full.yml ps

echo ''
echo '╔══════════════════════════════════════════╗'
echo '║        ✅ 部署完成！                      ║'
echo '╚══════════════════════════════════════════╝'
echo ''
echo '访问地址：'
echo '  后端API:    http://localhost:8082'
echo '  小程序:     http://localhost:8083'
echo '  管理后台:   http://localhost:8084'
echo ''
echo '运行测试：'
echo '  docker compose -f docker-compose-full.yml run tester'
echo ''
echo '查看日志：'
echo '  docker compose -f docker-compose-full.yml logs -f [服务名]'
