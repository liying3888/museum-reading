#!/bin/bash

# 颜色定义
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

PROJECT_NAME="museum-reading"
PROJECT_DIR="$HOME/openclaw-projects/museum-reading/code"

echo -e "${BLUE}
╔═══════════════════════════════════════════════════════════╗
║      博物馆读书小程序 - 自动化部署系统                    ║
╚═══════════════════════════════════════════════════════════╝
${NC}"

# 1. 拉取最新代码
echo -e "\n${BLUE}[1/7] 拉取最新代码${NC}"
cd $PROJECT_DIR
git pull origin feature/museum-reading

# 2. 停止旧容器
echo -e "\n${BLUE}[2/7] 停止旧容器${NC}"
docker-compose -f docker-compose-full.yml down
sleep 3

# 3. 清理旧镜像（可选）
echo -e "\n${BLUE}[3/7] 清理旧镜像${NC}"
docker image prune -f

# 4. 构建新镜像
echo -e "\n${BLUE}[4/7] 构建Docker镜像${NC}"
echo "  构建后端镜像..."
docker-compose -f docker-compose-full.yml build backend

echo "  构建小程序镜像..."
docker-compose -f docker-compose-full.yml build miniapp

echo "  构建管理后台镜像..."
docker-compose -f docker-compose-full.yml build admin

echo "  构建测试镜像..."
docker-compose -f docker-compose-full.yml build tester

# 5. 启动所有服务
echo -e "\n${BLUE}[5/7] 启动所有服务${NC}"
docker-compose -f docker-compose-full.yml up -d mysql redis

echo "  等待数据库启动..."
sleep 10

docker-compose -f docker-compose-full.yml up -d backend

echo "  等待后端启动..."
sleep 30

docker-compose -f docker-compose-full.yml up -d miniapp admin

# 6. 等待服务健康
echo -e "\n${BLUE}[6/7] 等待服务健康检查${NC}"
for i in {1..30}; do
  if curl -f http://localhost:8082/actuator/health > /dev/null 2>&1; then
    echo -e "  ${GREEN}✓ 后端服务健康${NC}"
    break
  fi
  echo "  等待后端服务启动... ($i/30)"
  sleep 2
done

# 7. 运行自动化测试
echo -e "\n${BLUE}[7/7] 运行自动化测试${NC}"
docker-compose -f docker-compose-full.yml up tester

# 检查测试结果
if [ $? -eq 0 ]; then
  echo -e "\n${GREEN}╔═══════════════════════════════════════════════════════════╗"
  echo -e "${GREEN}║              ✅ 部署成功！所有测试通过                    ║"
  echo -e "${GREEN}╚═══════════════════════════════════════════════════════════╝"
  
  echo -e "\n${BLUE}服务访问地址："
  echo -e "  后端API:   http://localhost:8082"
  echo -e "  小程序:    http://localhost:8083"
  echo -e "  管理后台:  http://localhost:8084"
  
  # 发送飞书通知
  # curl -X POST "${FEISHU_WEBHOOK}" -H 'Content-Type: application/json' -d '{"msg_type":"text","content":{"text":"博物馆读书小程序部署成功！"}}'
else
  echo -e "\n${RED}╔═══════════════════════════════════════════════════════════╗"
  echo -e "${RED}║              ❌ 部署失败！测试未通过                      ║"
  echo -e "${RED}╚═══════════════════════════════════════════════════════════╝"
  
  # 发送失败通知
  # curl -X POST "${FEISHU_WEBHOOK}" -H 'Content-Type: application/json' -d '{"msg_type":"text","content":{"text":"博物馆读书小程序部署失败！"}}'
  
  exit 1
fi

# 显示容器状态
echo -e "\n${BLUE}容器状态："
docker-compose -f docker-compose-full.yml ps
