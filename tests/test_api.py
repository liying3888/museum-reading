#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import pytest
import requests
import time

# API 基础地址
BASE_URL = "http://backend:8082"

class TestBookAPI:
    """书籍API测试"""
    
    def test_book_list(self):
        """测试书籍列表"""
        response = requests.get(f"{BASE_URL}/api/admin/books/list", params={
            "pageNo": 1,
            "pageSize": 10
        })
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 200
        
    def test_book_detail(self):
        """测试书籍详情"""
        response = requests.get(f"{BASE_URL}/api/admin/books/detail", params={
            "id": 1
        })
        assert response.status_code == 200

class TestBookshelfAPI:
    """书架API测试"""
    
    def test_bookshelf_list(self):
        """测试书架列表"""
        response = requests.get(f"{BASE_URL}/api/front/bookshelf/list")
        assert response.status_code == 200
        
    def test_bookshelf_add(self):
        """测试加入书架"""
        response = requests.post(
            f"{BASE_URL}/api/front/bookshelf/add",
            json={"book_id": 1}
        )
        assert response.status_code == 200

class TestReadingAPI:
    """阅读API测试"""
    
    def test_reading_start(self):
        """测试开始阅读"""
        response = requests.post(
            f"{BASE_URL}/api/front/reading/start",
            json={"book_id": 1}
        )
        assert response.status_code == 200
        data = response.json()
        return data.get("data", {}).get("record_id")
        
    def test_reading_stats(self):
        """测试阅读统计"""
        response = requests.get(f"{BASE_URL}/api/front/reading/stats")
        assert response.status_code == 200

class TestHealthCheck:
    """健康检查"""
    
    def test_backend_health(self):
        """测试后端健康"""
        response = requests.get(f"{BASE_URL}/actuator/health")
        assert response.status_code == 200
        
    def test_mysql_health(self):
        """测试MySQL连接"""
        # 通过API间接测试
        response = requests.get(f"{BASE_URL}/api/admin/books/list", params={
            "pageNo": 1,
            "pageSize": 1
        })
        assert response.status_code == 200

if __name__ == "__main__":
    pytest.main(["-v", "--html=reports/test-report.html"
