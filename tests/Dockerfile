FROM python:3.9-slim

WORKDIR /app

# 安装测试依赖
RUN pip install --no-cache-dir pytest requests pytest-html

# 复制测试脚本
COPY . /app/tests/

# 运行测试
CMD ["pytest", "-v", "--html=reports/test-report.html", "tests/"
