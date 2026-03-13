<template>
  <view class="container">
    <!-- 顶部 -->
    <view class="header">
      <text class="title">📚 荣河读书</text>
    </view>
    
    <!-- 今日目标 -->
    <view class="goal-card">
      <text class="goal-title">今日阅读目标</text>
      <view class="goal-progress">
        <text class="current">已读 {{ todayMinutes }} 分钟</text>
        <text class="target">目标 {{ goalMinutes }} 分钟</text>
      </view>
      <view class="progress-bar">
        <view class="progress-fill" :style="{ width: progressPercent + '%' }"></view>
      </view>
      <text class="percent">{{ progressPercent }}%</text>
    </view>
    
    <!-- 开始阅读按钮 -->
    <view class="start-btn" @click="startReading">
      <text class="btn-text">📖 开始阅读</text>
    </view>
    
    <!-- 继续阅读 -->
    <view v-if="currentBook" class="continue-card" @click="continueReading">
      <text class="continue-title">📖 继续阅读</text>
      <view class="book-info">
        <text class="book-title">《{{ currentBook.title }}》</text>
        <text class="book-author">{{ currentBook.author }}</text>
        <text class="book-progress">第{{ currentBook.currentPage }}页 / {{ currentBook.pages }}页</text>
      </view>
    </view>
    
    <!-- 近期活动 -->
    <view class="section">
      <text class="section-title">📅 近期活动</text>
      <view v-if="activities.length > 0" class="activity-card" v-for="item in activities" :key="item.id">
        <text class="activity-title">{{ item.title }}</text>
        <text class="activity-time">{{ item.time }}</text>
        <text class="activity-users">已报名 {{ item.currentUsers }}/{{ item.maxUsers }}人</text>
      </view>
      <view v-else class="empty">
        <text>暂无活动</text>
      </view>
    </view>
    
    <!-- 最新笔记 -->
    <view class="section">
      <text class="section-title">📝 最新笔记</text>
      <view v-if="notes.length > 0" class="note-card" v-for="item in notes" :key="item.id">
        <text class="note-title">{{ item.title }}</text>
        <text class="note-author">by {{ item.author }} · {{ item.time }}</text>
        <text class="note-content">{{ item.content }}</text>
      </view>
      <view v-else class="empty">
        <text>暂无笔记</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { readingStats, bookshelfList } from '@/api/book'

const todayMinutes = ref(0)
const goalMinutes = ref(30)
const currentBook = ref<any>(null)
const activities = ref<any[]>([])
const notes = ref<any[]>([])

const progressPercent = computed(() => {
  return Math.min(100, Math.round((todayMinutes.value / goalMinutes.value) * 100))
})

const startReading = () => {
  uni.navigateTo({ url: '/pages/reading/timer' })
}

const continueReading = () => {
  if (currentBook.value) {
    uni.navigateTo({ 
      url: `/pages/reading/timer?bookId=${currentBook.value.bookId}` 
    })
  }
}

const loadData = async () => {
  try {
    const statsRes = await readingStats()
    if (statsRes.data) {
      todayMinutes.value = Math.round(statsRes.data.today / 60)
      goalMinutes.value = statsRes.data.goal || 30
    }
    
    const bookshelfRes = await bookshelfList({ status: 1 })
    if (bookshelfRes.data?.lists?.length > 0) {
      currentBook.value = bookshelfRes.data.lists[0]
    }
  } catch (e) {
    console.error(e)
  }
}

onLoad(() => {
  loadData()
})

onShow(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
.container {
  padding: 20rpx 30rpx;
  background: #f5f5f5;
  min-height: 100vh;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 0;
  .title {
    font-size: 36rpx;
    font-weight: bold;
  }
}

.goal-card {
  background: white;
  border-radius: 20rpx;
  padding: 40rpx;
  margin: 20rpx 0;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.05);
  
  .goal-title {
    font-size: 32rpx;
    color: #666;
    display: block;
    text-align: center;
    margin-bottom: 20rpx;
  }
  
  .goal-progress {
    display: flex;
    justify-content: space-between;
    margin-bottom: 20rpx;
    
    .current {
      font-size: 36rpx;
      font-weight: bold;
      color: #333;
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
      transition: width 0.3s;
    }
  }
  
  .percent {
    display: block;
    text-align: center;
    margin-top: 10rpx;
    font-size: 28rpx;
    color: #666;
  }
}

.start-btn {
  background: linear-gradient(to right, #667eea, #764ba2);
  border-radius: 50rpx;
  padding: 40rpx;
  text-align: center;
  margin: 40rpx 0;
  box-shadow: 0 8rpx 30rpx rgba(102, 126, 234, 0.4);
  
  .btn-text {
    color: white;
    font-size: 36rpx;
    font-weight: bold;
  }
}

.continue-card {
  background: white;
  border-radius: 20rpx;
  padding: 30rpx;
  margin: 20rpx 0;
  
  .continue-title {
    font-size: 32rpx;
    font-weight: bold;
    display: block;
    margin-bottom: 20rpx;
  }
  
  .book-info {
    .book-title {
      font-size: 30rpx;
      color: #333;
      display: block;
      margin-bottom: 10rpx;
    }
    
    .book-author {
      font-size: 26rpx;
      color: #999;
      display: block;
      margin-bottom: 10rpx;
    }
    
    .book-progress {
      font-size: 26rpx;
      color: #666;
    }
  }
}

.section {
  margin: 40rpx 0;
  
  .section-title {
    font-size: 32rpx;
    font-weight: bold;
    display: block;
    margin-bottom: 20rpx;
  }
  
  .empty {
    background: white;
    border-radius: 20rpx;
    padding: 60rpx;
    text-align: center;
    color: #999;
  }
}
</style>
