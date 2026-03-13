<template>
  <view class="container">
    <!-- 用户信息 -->
    <view class="user-card">
      <image :src="userInfo.avatar || '/static/default-avatar.png'" class="avatar" />
      <view class="user-info">
        <text class="nickname">{{ userInfo.nickname || '读者' }}</text>
        <text class="id">ID: {{ userInfo.sn }}</text>
      </view>
      <view class="stats-link" @click="goTo('/pages/stats/index')">
        <text>查看统计</text>
        <text class="arrow">></text>
      </view>
    </view>
    
    <!-- 阅读统计 -->
    <view class="stats-card">
      <view class="stat-item" @click="goTo('/pages/stats/index')">
        <text class="stat-value">{{ formatHours(totalStats.duration_hours) }}</text>
        <text class="stat-label">累计阅读(小时)</text>
      </view>
      <view class="stat-item" @click="goTo('/pages/bookshelf/index')">
        <text class="stat-value">{{ totalStats.finished_books || 0 }}</text>
        <text class="stat-label">已读书籍</text>
      </view>
      <view class="stat-item">
        <text class="stat-value">{{ totalStats.consecutive_days || 0 }}</text>
        <text class="stat-label">连续打卡(天)</text>
      </view>
    </view>
    
    <!-- 今日目标 -->
    <view class="goal-card">
      <view class="goal-header">
        <text class="goal-title">今日目标</text>
        <text class="goal-tip">{{ goalText }}</text>
      </view>
      <view class="goal-progress">
        <text class="current">已读 {{ todayStats.duration_minutes || 0 }} 分钟</text>
        <text class="target">目标 {{ goalMinutes }} 分钟</text>
      </view>
      <view class="progress-bar">
        <view class="progress-fill" :style="{ width: goalPercent + '%' }"></view>
      </view>
    </view>
    
    <!-- 功能入口 -->
    <view class="menu-list">
      <view class="menu-item" @click="goTo('/pages/bookshelf/index')">
        <text class="menu-icon">📚</text>
        <text class="menu-title">我的书架</text>
        <text class="menu-desc">{{ totalStats.total_books || 0 }}本书</text>
        <text class="menu-arrow">></text>
      </view>
      
      <view class="menu-item" @click="goTo('/pages/stats/index')">
        <text class="menu-icon">📊</text>
        <text class="menu-title">阅读统计</text>
        <text class="menu-desc">查看详情</text>
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
        <text class="menu-icon">⏱️</text>
        <text class="menu-title">阅读记录</text>
        <text class="menu-arrow">></text>
      </view>
      
      <view class="menu-item" @click="goTo('/pages/user/settings')">
        <text class="menu-icon">⚙️</text>
        <text class="menu-title">设置</text>
        <text class="menu-arrow">></text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getTotalStats, getTodayStats } from '@/api/stats'

const userInfo = ref<any>({
  avatar: '',
  nickname: '书友',
  sn: '100001'
})

const todayStats = ref<any>({})
const totalStats = ref<any>({})

// 目标设置（可以从配置中读取）
const goalMinutes = ref(60)

// 目标完成百分比
const goalPercent = computed(() => {
  const minutes = todayStats.value.duration_minutes || 0
  return Math.min((minutes / goalMinutes.value) * 100, 100)
})

// 目标提示文本
const goalText = computed(() => {
  const minutes = todayStats.value.duration_minutes || 0
  if (minutes >= goalMinutes.value) {
    return '🎉 已完成目标'
  } else if (minutes > 0) {
    return '继续加油'
  } else {
    return '开始阅读吧'
  }
})

// 格式化小时数
const formatHours = (hours: number) => {
  if (!hours) return '0'
  return hours.toFixed(1)
}

// 加载统计数据
const loadStats = async () => {
  try {
    const [totalRes, todayRes] = await Promise.all([
      getTotalStats(),
      getTodayStats()
    ])
    
    if (totalRes.code === 200) {
      totalStats.value = totalRes.data
    }
    
    if (todayRes.code === 200) {
      todayStats.value = todayRes.data
    }
  } catch (e) {
    console.error('加载统计失败', e)
  }
}

// 跳转页面
const goTo = (url: string) => {
  uni.navigateTo({ url })
}

onMounted(() => {
  loadStats()
})
</script>

<style lang="scss" scoped>
.container {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 40rpx;
}

.user-card {
  display: flex;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 60rpx 40rpx;
  padding-top: 100rpx;
  
  .avatar {
    width: 120rpx;
    height: 120rpx;
    border-radius: 50%;
    border: 4rpx solid #fff;
    margin-right: 30rpx;
  }
  
  .user-info {
    flex: 1;
    
    .nickname {
      display: block;
      font-size: 36rpx;
      font-weight: bold;
      color: #fff;
      margin-bottom: 10rpx;
    }
    
    .id {
      font-size: 24rpx;
      color: rgba(255, 255, 255, 0.8);
    }
  }
  
  .stats-link {
    display: flex;
    align-items: center;
    background: rgba(255, 255, 255, 0.2);
    padding: 16rpx 24rpx;
    border-radius: 30rpx;
    
    text {
      font-size: 26rpx;
      color: #fff;
    }
    
    .arrow {
      margin-left: 10rpx;
    }
  }
}

.stats-card {
  display: flex;
  background: #fff;
  margin: -40rpx 30rpx 30rpx;
  border-radius: 20rpx;
  padding: 40rpx 20rpx;
  box-shadow: 0 8rpx 20rpx rgba(0, 0, 0, 0.1);
  
  .stat-item {
    flex: 1;
    text-align: center;
    
    .stat-value {
      display: block;
      font-size: 48rpx;
      font-weight: bold;
      color: #667eea;
      margin-bottom: 10rpx;
    }
    
    .stat-label {
      font-size: 24rpx;
      color: #999;
    }
  }
}

.goal-card {
  background: #fff;
  margin: 0 30rpx 30rpx;
  border-radius: 20rpx;
  padding: 30rpx;
  box-shadow: 0 8rpx 20rpx rgba(0, 0, 0, 0.1);
  
  .goal-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20rpx;
    
    .goal-title {
      font-size: 32rpx;
      font-weight: bold;
      color: #333;
    }
    
    .goal-tip {
      font-size: 24rpx;
      color: #667eea;
    }
  }
  
  .goal-progress {
    display: flex;
    justify-content: space-between;
    margin-bottom: 20rpx;
    
    .current, .target {
      font-size: 24rpx;
      color: #666;
    }
  }
  
  .progress-bar {
    height: 12rpx;
    background: #f0f0f0;
    border-radius: 6rpx;
    overflow: hidden;
    
    .progress-fill {
      height: 100%;
      background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
      border-radius: 6rpx;
      transition: width 0.3s ease;
    }
  }
}

.menu-list {
  background: #fff;
  margin: 0 30rpx;
  border-radius: 20rpx;
  overflow: hidden;
  box-shadow: 0 8rpx 20rpx rgba(0, 0, 0, 0.1);
  
  .menu-item {
    display: flex;
    align-items: center;
    padding: 30rpx;
    border-bottom: 1px solid #f0f0f0;
    
    &:last-child {
      border-bottom: none;
    }
    
    &:active {
      background: #f9f9f9;
    }
    
    .menu-icon {
      font-size: 40rpx;
      margin-right: 20rpx;
    }
    
    .menu-title {
      flex: 1;
      font-size: 30rpx;
      color: #333;
    }
    
    .menu-desc {
      font-size: 24rpx;
      color: #999;
      margin-right: 10rpx;
    }
    
    .menu-arrow {
      font-size: 28rpx;
      color: #ccc;
    }
  }
}
</style>
