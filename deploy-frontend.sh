#!/bin/bash
set -e

cd ~/openclaw-projects/museum-reading/code

echo "=== 停止所有服务 ==="
docker-compose -f docker-compose-full.yml down

echo "=== 构建并启动 frontend 服务 ==="
docker-compose -f docker-compose-full.yml build frontend
docker-compose -f docker-compose-full.yml up -d frontend

echo "=== 等待服务启动 ==="
sleep 10

echo "=== 检查服务状态 ==="
docker-compose -f docker-compose-full.yml ps frontend

echo "=== 查看日志 ==="
docker-compose -f docker-compose-full.yml logs --tail=50 frontend

echo "=== 部署完成 ==="
