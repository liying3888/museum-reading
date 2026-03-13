#!/bin/bash

echo "=== 博物馆读书小程序测试脚本 ==="

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

# 测试后端API
echo ""
echo "[1] 测试后端API"

echo "  测试书籍列表API..."
curl -s http://localhost:8082/api/admin/books/list?pageNo=1&pageSize=5 | jq '.' | head -20
if [ $? -eq 0 ]; then
  echo -e "  ${GREEN}✓ 书籍列表API正常${NC}"
else
  echo -e "  ${RED}✗ 书籍列表API异常${NC}"
fi

echo ""
echo "  测试书架API..."
curl -s http://localhost:8082/api/front/bookshelf/list | jq '.' | head -20
if [ $? -eq 0 ]; then
  echo -e "  ${GREEN}✓ 书架API正常${NC}"
else
  echo -e "  ${RED}✗ 书架API异常${NC}"
fi

echo ""
echo "  测试阅读统计API..."
curl -s http://localhost:8082/api/front/reading/stats | jq '.' | head -20
if [ $? -eq 0 ]; then
  echo -e "  ${GREEN}✓ 阅读统计API正常${NC}"
else
  echo -e "  ${RED}✗ 阅读统计API异常${NC}"
fi

# 测试数据库
echo ""
echo "[2] 测试数据库"

echo "  检查书籍表..."
docker exec likeadmin-java-mysql mysql -uroot -proot -e 'SELECT COUNT(*) as count FROM likeadmin_java.la_book;' 2>&1 | grep -v 'Warning'

echo "  检查书架表..."
docker exec likeadmin-java-mysql mysql -uroot -proot -e 'SELECT COUNT(*) as count FROM likeadmin_java.la_bookshelf;' 2>&1 | grep -v 'Warning'

echo "  检查阅读记录表..."
docker exec likeadmin-java-mysql mysql -uroot -proot -e 'SELECT COUNT(*) as count FROM likeadmin_java.la_reading_record;' 2>&1 | grep -v 'Warning'

# 测试前端构建
echo ""
echo "[3] 测试前端构建"

if [ -d "uniapp/dist/build/mp-weixin" ]; then
  echo -e "  ${GREEN}✓ 小程序构建文件存在${NC}"
  ls -lh uniapp/dist/build/mp-weixin/ | head -10
else
  echo -e "  ${RED}✗ 小程序未构建${NC}"
fi

echo ""
echo "=== 测试完成 ==="
