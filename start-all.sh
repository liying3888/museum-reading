#!/bin/bash

echo "🚀 启动博物馆读书小程序所有服务..."

cd $(dirname 0)

# 停止旧服务
docker compose -f docker-compose-full.yml down 2>/dev/null || true

# 启动所有服务
echo ''
echo '启动服务中...'
docker compose -f docker-compose-full.yml up -d

echo ''
echo "⏳ 等待服务启动..."
sleep 10

echo ''
echo '### 服务状态 ###'
docker compose -f docker-compose-full.yml ps

echo ''
echo "✅ 所有服务已启动！"
echo ""
echo "访问地址："
echo "  后端API:    http://localhost:8082"
echo "  小程序:     http://localhost:8083"
echo "  管理后台:   http://localhost:8084"
echo ""
echo "查看日志："
echo "  docker compose -f docker-compose-full.yml logs -f"
echo ""
echo "停止服务："
echo "  docker compose -f docker-compose-full.yml down"
