<template>
  <view class="container">
    <!-- 计时器 -->
    <view class="timer-display">
      <text class="time">{{ formatTime }}</text>
      <text v-if="bookTitle" class="book-title">《{{ bookTitle }}》</text>
    </view>
    
    <!-- 控制按钮 -->
    <view class="controls">
      <view v-if="status === 'ready'" class="btn-start" @click="start">
        <text class="btn-text">▶ 开始阅读</text>
      </view>
      
      <view v-if="status === 'running'" class="btn-group">
        <view class="btn-pause" @click="pause">
          <text class="btn-text">⏸ 暂停</text>
        </view>
        <view class="btn-stop" @click="stop">
          <text class="btn-text">⏹ 结束</text>
        </view>
      </view>
      
      <view v-if="status === 'paused'" class="btn-group">
        <view class="btn-resume" @click="resume">
          <text class="btn-text">▶ 继续</text>
        </view>
        <view class="btn-stop" @click="stop">
          <text class="btn-text">⏹ 结束</text>
        </view>
      </view>
    </view>
    
    <!-- 本次成果 -->
    <view v-if="showResult" class="result-modal">
      <view class="result-card">
        <text class="result-title">🎉 本次阅读完成！</text>
        <text class="result-duration">{{ formatDuration }}</text>
        <view class="result-stats">
          <text>今日累计：{{ todayTotal }} 分钟</text>
        </view>
        <view class="result-btn" @click="confirmResult">
          <text>确定</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onUnmounted } from 'vue'
import { readingStart, readingEnd, readingStats } from '@/api/book'

const status = ref<'ready' | 'running' | 'paused'>('ready')
const seconds = ref(0)
const recordId = ref<number | null>(null)
const bookId = ref<number | null>(null)
const bookTitle = ref('')
const showResult = ref(false)
const todayTotal = ref(0)
const startTime = ref<number | null>(null)

let timer: any = null

const formatTime = computed(() => {
  const h = Math.floor(seconds.value / 3600)
  const m = Math.floor((seconds.value % 3600) / 60)
  const s = seconds.value % 60
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
})

const formatDuration = computed(() => {
  const m = Math.floor(seconds.value / 60)
  const s = seconds.value % 60
  return `${m} 分 ${s} 秒`
})

const start = async () => {
  try {
    const res = await readingStart(bookId.value || undefined)
    if (res.data) {
      recordId.value = res.data.record_id
      startTime.value = Date.now()
      status.value = 'running'
      timer = setInterval(() => {
        seconds.value++
      }, 1000)
    }
  } catch (e) {
    uni.showToast({ title: '启动失败', icon: 'none' })
  }
}

const pause = () => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
  status.value = 'paused'
}

const resume = () => {
  status.value = 'running'
  timer = setInterval(() => {
    seconds.value++
  }, 1000)
}

const stop = async () => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
  
  if (recordId.value) {
    try {
      const res = await readingEnd(recordId.value)
      if (res.data) {
        todayTotal.value = Math.round(res.data.today_total / 60)
        showResult.value = true
      }
    } catch (e) {
      uni.showToast({ title: '保存失败', icon: 'none' })
    }
  }
}

const confirmResult = () => {
  uni.navigateBack()
}

onLoad((options: any) => {
  if (options.bookId) {
    bookId.value = parseInt(options.bookId)
  }
  if (options.bookTitle) {
    bookTitle.value = options.bookTitle
  }
})

onUnload(() => {
  if (timer) {
    clearInterval(timer)
  }
})
</script>

<style lang="scss" scoped>
.container {
  min-height: 100vh;
  background: linear-gradient(to bottom, #667eea, #764ba2);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.timer-display {
  text-align: center;
  margin-bottom: 100rpx;
  
  .time {
    font-size: 120rpx;
    color: white;
    font-weight: bold;
    letter-spacing: 4rpx;
    display: block;
  }
  
  .book-title {
    font-size: 32rpx;
    color: rgba(255, 255, 255, 0.8);
    margin-top: 20rpx;
    display: block;
  }
}

.controls {
  width: 100%;
  padding: 0 40rpx;
  
  .btn-start {
    background: white;
    border-radius: 100rpx;
    padding: 40rpx;
    text-align: center;
    
    .btn-text {
      color: #667eea;
      font-size: 36rpx;
      font-weight: bold;
    }
  }
  
  .btn-group {
    display: flex;
    gap: 40rpx;
    
    view {
      flex: 1;
      background: rgba(255, 255, 255, 0.2);
      border-radius: 100rpx;
      padding: 40rpx;
      text-align: center;
      
      .btn-text {
        color: white;
        font-size: 32rpx;
        font-weight: bold;
      }
    }
    
    .btn-stop {
      background: rgba(255, 255, 255, 0.9);
      
      .btn-text {
        color: #ff4757;
      }
    }
  }
}

.result-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  
  .result-card {
    background: white;
    border-radius: 30rpx;
    padding: 60rpx;
    width: 80%;
    text-align: center;
    
    .result-title {
      font-size: 36rpx;
      font-weight: bold;
      display: block;
      margin-bottom: 40rpx;
    }
    
    .result-duration {
      font-size: 60rpx;
      font-weight: bold;
      color: #667eea;
      display: block;
      margin-bottom: 30rpx;
    }
    
    .result-stats {
      font-size: 28rpx;
      color: #666;
      margin-bottom: 40rpx;
    }
    
    .result-btn {
      background: #667eea;
      border-radius: 50rpx;
      padding: 30rpx;
      color: white;
      font-size: 32rpx;
    }
  }
}
</style>
