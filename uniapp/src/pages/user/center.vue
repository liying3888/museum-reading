<template>
  <view class="container">
    <!-- 用户信息 -->
    <view class="user-card">
      <image :src="userInfo.avatar || '/static/default-avatar.png'" class="avatar" />
      <view class="user-info">
        <text class="nickname">{{ userInfo.nickname || '读者' }}</text>
        <text class="id">ID: {{ userInfo.sn }}</text>
      </view>
    </view>
    
    <!-- 阅读统计 -->
    <view class="stats-card">
      <view class="stat-item">
        <text class="stat-value">{{ formatTime(stats.total) }}</text>
        <text class="stat-label">累计阅读</text>
      </view>
      <view class="stat-item">
        <text class="stat-value">{{ stats.booksRead }}</text>
        <text class="stat-label">已读书籍</text>
      </view>
      <view class="stat-item">
        <text class="stat-value">{{ stats.continuousDays }}</text>
        <text class="stat-label">连续打卡</text>
      </view>
    </view>
    
    <!-- 功能入口 -->
    <view class="menu-list">
      <view class="menu-item" @click="goTo('/pages/bookshelf/index')">
        <text class="menu-icon">📚</text>
        <text class="menu-title">我的书架</text>
        <text class="menu-arrow">></text>
      </view>
      
      <view class="menu-item" @click="goTo('/pages/note/list')">
        <text class="menu-icon">📝</text>
        <text class="menu-title">我的笔记</text>
        <text class="menu-arrow">></text>
      </view>
      
      <view class="menu-item" @click="goTo('/pages/activity/my')">
        <text class="menu-icon">📅</text>
        <text class="menu-title">我的活动</text>
        <text class="menu-arrow">></text>
      </view>
      
      <view class="menu-item" @click="goTo('/pages/reading/records')">
        <text class="menu-icon">📊</text>
        <text class="menu-title">阅读记录</text>
        <text class="menu-arrow">></text>
      </view>
      
      <view class="menu-item" @click="goTo('/pages/user/settings')">
        <text class="menu-icon">⚙️</text>
        <text class="menu-title">设置</text>
        <text class="menu-arrow">></text>
      </view>
    </view>
    
    <!-- 今日目标 -->
    <view class="goal-card">
      <text class="goal-title">今日目标</text>
      <view class="goal-progress">
        <text class="current">已读 {{ todayMinutes }} 分钟</text>
        <text class="target">目标 {{ goalMinutes }} 分钟</text>
      </view>
      <view class="progress-bar">
        <view class="progress-fill" :style="{ width: goalPercent + '%' }"></view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { readingStats } from '@/api/book'

const userInfo = ref<any>({})
const stats = ref<any>({
  total: 0,
  booksRead: 0,
  continuousDays: 0
})

const todayMinutes = ref(0)
const goalMinutes = ref(30)

const goalPercent = computed(() => {
  return Math.min(100, (todayMinutes.value / goalMinutes.value) * 100)
})

const formatTime = (seconds: number) => {
  const hours = Math.floor(seconds / 3600)
  if (hours > 0) {
    return `${hours}小时`
  }
  const minutes = Math.floor(seconds / 60)
  return `${minutes}分钟`
}

const loadStats = async () => {
  try {
    const res = await readingStats()
    if (res.data) {
      stats.value.total = res.data.total
      todayMinutes.value = Math.round(res.data.today / 60)
      goalMinutes.value = res.data.goal || 30
    }
  } catch (e) {
    console.error(e)
  }
}

const goTo = (url: string) => {
  uni.navigateTo({ url })
}

onShow(() => {
  loadStats()
})
</script>

<style lang="scss" scoped>
.container {
  min-height: 100vh;
  background: #f5f5f5;
}

.user-card {
  display: flex;
  align-items: center;
  background: linear-gradient(to right, #667eea, #764ba2);
  padding: 60rpx 30rpx;
  
  .avatar {
    width: 120rpx;
    height: 120rpx;
    border-radius: 60rpx;
    background: white;
  }
  
  .user-info {
    margin-left: 30rpx;
    
    .nickname {
      font-size: 36rpx;
      color: white;
      font-weight: bold;
      display: block;
      margin-bottom: 10rpx;
    }
    
    .id {
      font-size: 24rpx;
      color: rgba(255, 255, 255, 0.8);
    }
  }
}

.stats-card {
  display: flex;
  background: white;
  padding: 40rpx 0;
  margin: -30rpx 20rpx 20rpx;
  border-radius: 20rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.1);
  
  .stat-item {
    flex: 1;
    text-align: center;
    
    .stat-value {
      font-size: 40rpx;
      font-weight: bold;
      color: #333;
      display: block;
      margin-bottom: 10rpx;
    }
    
    .stat-label {
      font-size: 26rpx;
      color: #999;
    }
  }
}

.menu-list {
  background: white;
  margin-bottom: 20rpx;
  
  .menu-item {
    display: flex;
    align-items: center;
    padding: 30rpx;
    border-bottom: 1rpx solid #f0f0f0;
    
    &:last-child {
      border-bottom: none;
    }
    
    .menu-icon {
      font-size: 40rpx;
      margin-right: 20rpx;
    }
    
    .menu-title {
      flex: 1;
      font-size: 32rpx;
      color: #333;
    }
    
    .menu-arrow {
      font-size: 28rpx;
      color: #ccc;
    }
  }
}

.goal-card {
  background: white;
  margin: 20rpx;
  padding: 30rpx;
  border-radius: 20rpx;
  
  .goal-title {
    font-size: 32rpx;
    font-weight: bold;
    display: block;
    margin-bottom: 20rpx;
  }
  
  .goal-progress {
    display: flex;
    justify-content: space-between;
    margin-bottom: 15rpx;
    
    .current {
      font-size: 28rpx;
      color: #666;
    }
    
    .target {
      font-size: 28rpx;
      color: #999;
    }
  }
  
  .progress-bar {
    height: 16rpx;
    background: #eee;
    border-radius: 8rpx;
    overflow: hidden;
    
    .progress-fill {
      height: 100%;
      background: linear-gradient(to right, #667eea, #764ba2);
    }
  }
}
</style>
