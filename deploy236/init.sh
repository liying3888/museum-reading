#!/bin/bash
# ops70 20261103 卡003 museum 236 幂等初始化
set -e
cd /www/museum/staging

# 1) .env 口令（幂等）
if [ ! -f .env ]; then
  echo "MUSEUM_MYSQL_PASSWORD=$(openssl rand -hex 12)" > .env
  chmod 600 .env
fi
echo ".env keys: $(cut -d= -f1 .env | tr '\n' ' ')"

# 2) seed-init.sql 拼合（依赖序主动控序，非字典序）：框架全量 -> 业务表 -> 分类表 -> 测试数据 -> 测试用户
#    每段头 SET NAMES utf8mb4 防 mojibake（memory 坑族）
python3 - <<'PY'
parts = []
for f in ['install_v1.5.0.sql', 'museum_reading.sql', 'book_category.sql', 'test_data.sql', 'test_user.sql']:
    parts.append('\n-- ===== %s ===== --\n' % f)
    parts.append('SET NAMES utf8mb4;\n')
    parts.append(open('/www/museum/src/sql/' + f, encoding='utf-8').read())
    parts.append('\n')
open('/www/museum/staging/seed-init.sql', 'w', encoding='utf-8').write(''.join(parts))
print('seed-init.sql written (5 files, ordered)')
PY

echo INIT_DONE
