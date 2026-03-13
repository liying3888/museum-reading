#!/bin/bash

echo "=== 博物馆读书小程序部署脚本 ==="

# 颜色定义
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

# 项目路径
PROJECT_DIR="$HOME/openclaw-projects/museum-reading/code"
BACKEND_DIR="$PROJECT_DIR/server"
FRONTEND_DIR="$PROJECT_DIR/uniapp"
ADMIN_DIR="$PROJECT_DIR/admin"

# 1. 拉取最新代码
echo -e "\n${BLUE}[1/5] 拉取最新代码${NC}"
cd $PROJECT_DIR
git pull origin feature/museum-reading

# 2. 后端部署
echo -e "\n${BLUE}[2/5] 后端服务部署${NC}"
cd $BACKEND_DIR
echo "  编译后端项目..."
mvn clean package -DskipTests 2>&1 | tail -10

# 停止旧服务
echo "  停止旧服务..."
pkill -f 'like-admin.*jar' || true
sleep 2

# 启动新服务
echo "  启动新服务..."
nohup java -jar like-admin/target/like-admin-1.0.0.jar > /dev/null 2>&1 &
echo -e "  ${GREEN}✓ 后端服务已启动${NC}"

# 3. 小程序构建
echo -e "\n${BLUE}[3/5] 小程序构建${NC}"
cd $FRONTEND_DIR
echo "  安装依赖..."
npm install --legacy-peer-deps 2>&1 | tail -5

echo "  构建微信小程序..."
npm run build:mp-weixin 2>&1 | tail -5

# 4. 管理后台构建
echo -e "\n${BLUE}[4/5] 管理后台构建${NC}"
cd $ADMIN_DIR
echo "  安装依赖..."
npm install --legacy-peer-deps 2>&1 | tail -5

echo "  构建生产版本..."
npm run build 2>&1 | tail -5

# 5. 数据库迁移（如果有新SQL）
echo -e "\n${BLUE}[5/5] 数据库迁移${NC}"
if [ -f "$PROJECT_DIR/sql/museum_reading.sql" ]; then
  echo "  执行SQL脚本..."
  docker exec -i likeadmin-java-mysql mysql -uroot -proot likeadmin_java < $PROJECT_DIR/sql/museum_reading.sql 2>&1 | grep -v 'Warning' || true
  echo -e "  ${GREEN}✓ 数据库更新完成${NC}"
fi

echo -e "\n${GREEN}=== 部署完成！ ==="
echo ""
echo "访问地址："
echo "  后端API: http://localhost:8082"
echo "  小程序: 打开微信开发者工具，导入 $FRONTEND_DIR/dist/build/mp-weixin"
echo "  管理后台: $ADMIN_DIR/dist/index.html"
echo ""
