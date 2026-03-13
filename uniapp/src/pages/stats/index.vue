<template>
  <view class="stats-page">
    <!-- 顶部标题 -->
    <view class="header">
      <text class="title">阅读统计</text>
    </view>

    <!-- 统计卡片 -->
    <view class="stats-cards">
      <!-- 今日统计 -->
      <view class="card today-card">
        <view class="card-title">今日阅读</view>
        <view class="card-content">
          <text class="big-number">{{ todayStats.duration_minutes || 0 }}</text>
          <text class="unit">分钟</text>
        </view>
        <view class="card-footer">
          <text>阅读 {{ todayStats.read_count || 0 }} 次</text>
          <text>{{ todayStats.book_count || 0 }} 本书</text>
        </view>
      </view>

      <!-- 累计统计 -->
      <view class="card total-card">
        <view class="card-title">累计阅读</view>
        <view class="card-content">
          <text class="big-number">{{ totalStats.duration_hours || 0 }}</text>
          <text class="unit">小时</text>
        </view>
        <view class="card-footer">
          <text>连续{{ totalStats.consecutive_days || 0 }}天</text>
          <text>{{ totalStats.finished_books || 0 }}本已读</text>
        </view>
      </view>
    </view>

    <!-- 本周阅读趋势 -->
    <view class="section">
      <view class="section-title">本周阅读趋势</view>
      <view class="chart-container">
        <view 
          v-for="(value, date) in weekStats.daily_stats" 
          :key="date"
          class="bar-item"
        >
          <view 
            class="bar" 
            :style="{ height: getBarHeight(value) + 'px' }"
          >
            <text class="bar-value">{{ value }}</text>
          </view>
          <text class="bar-label">{{ formatDay(date) }}</text>
        </view>
      </view>
    </view>

    <!-- 打卡日历 -->
    <view class="section">
      <view class="section-title">打卡记录（最近30天）</view>
      <view class="checkin-grid">
        <view 
          v-for="i in 30" 
          :key="i"
          :class="['checkin-item', { active: isChecked(i) }]"
        >
          <text class="day-number">{{ getDayNumber(i) }}</text>
        </view>
      </view>
    </view>

    <!-- 详细数据 -->
    <view class="section">
      <view class="section-title">详细数据</view>
      <view class="detail-list">
        <view class="detail-item">
          <text class="label">本月阅读</text>
          <text class="value">{{ monthStats.duration_hours || 0 }} 小时</text>
        </view>
        <view class="detail-item">
          <text class="label">阅读天数</text>
          <text class="value">{{ totalStats.read_days || 0 }} 天</text>
        </view>
        <view class="detail-item">
          <text class="label">在读书籍</text>
          <text class="value">{{ totalStats.reading_books || 0 }} 本</text>
        </view>
        <view class="detail-item">
          <text class="label">总书籍数</text>
          <text class="value">{{ totalStats.total_books || 0 }} 本</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getStatsOverview } from '@/api/stats'

const todayStats = ref<any>({})
const weekStats = ref<any>({})
const monthStats = ref<any>({})
const totalStats = ref<any>({})
const checkinDates = ref<string[]>([])

// 加载统计数据
const loadStats = async () => {
  try {
    uni.showLoading({ title: '加载中...' })
    const res = await getStatsOverview()
    if (res.code === 200) {
      todayStats.value = res.data.today || {}
      weekStats.value = res.data.week || {}
      monthStats.value = res.data.month || {}
      totalStats.value = res.data.total || {}
      checkinDates.value = res.data.checkin || []
    }
  } catch (e) {
    console.error('加载统计失败', e)
  } finally {
    uni.hideLoading()
  }
}

// 计算柱状图高度
const getBarHeight = (minutes: number) => {
  const maxHeight = 120
  const max = Math.max(...Object.values(weekStats.value.daily_stats || {}), 60)
  return Math.max((minutes / max) * maxHeight, 10)
}

// 格式化日期为星期
const formatDay = (dateStr: string) => {
  const date = new Date(dateStr)
  const days = ['日', '一', '二', '三', '四', '五', '六']
  return '周' + days[date.getDay()]
}

// 判断某天是否打卡
const isChecked = (index: number) => {
  const date = new Date()
  date.setDate(date.getDate() - (30 - index))
  const dateStr = date.toISOString().split('T')[0]
  return checkinDates.value.includes(dateStr)
}

// 获取日期数字
const getDayNumber = (index: number) => {
  const date = new Date()
  date.setDate(date.getDate() - (30 - index))
  return date.getDate()
}

onMounted(() => {
  loadStats()
})
</script>

<style lang="scss" scoped>
.stats-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 40rpx;
  padding-top: 80rpx;
}

.header {
  text-align: center;
  margin-bottom: 40rpx;
  
  .title {
    font-size: 48rpx;
    font-weight: bold;
    color: #fff;
  }
}

.stats-cards {
  display: flex;
  gap: 20rpx;
  margin-bottom: 40rpx;
}

.card {
  flex: 1;
  background: #fff;
  border-radius: 20rpx;
  padding: 30rpx;
  box-shadow: 0 8rpx 20rpx rgba(0, 0, 0, 0.1);
  
  .card-title {
    font-size: 28rpx;
    color: #666;
    margin-bottom: 20rpx;
  }
  
  .card-content {
    display: flex;
    align-items: baseline;
    margin-bottom: 20rpx;
    
    .big-number {
      font-size: 56rpx;
      font-weight: bold;
      color: #333;
      margin-right: 10rpx;
    }
    
    .unit {
      font-size: 24rpx;
      color: #999;
    }
  }
  
  .card-footer {
    display: flex;
    justify-content: space-between;
    font-size: 24rpx;
    color: #999;
  }
}

.section {
  background: #fff;
  border-radius: 20rpx;
  padding: 30rpx;
  margin-bottom: 30rpx;
  box-shadow: 0 8rpx 20rpx rgba(0, 0, 0, 0.1);
  
  .section-title {
    font-size: 32rpx;
    font-weight: bold;
    color: #333;
    margin-bottom: 30rpx;
  }
}

.chart-container {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  height: 200rpx;
  padding: 20rpx 0;
  
  .bar-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    flex: 1;
    
    .bar {
      width: 40rpx;
      background: linear-gradient(180deg, #667eea 0%, #764ba2 100%);
      border-radius: 8rpx 8rpx 0 0;
      display: flex;
      align-items: flex-start;
      justify-content: center;
      position: relative;
      
      .bar-value {
        position: absolute;
        top: -40rpx;
        font-size: 20rpx;
        color: #666;
      }
    }
    
    .bar-label {
      font-size: 22rpx;
      color: #999;
      margin-top: 10rpx;
    }
  }
}

.checkin-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx;
  
  .checkin-item {
    width: 60rpx;
    height: 60rpx;
    border-radius: 10rpx;
    background: #f5f5f5;
    display: flex;
    align-items: center;
    justify-content: center;
    
    &.active {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      
      .day-number {
        color: #fff;
      }
    }
    
    .day-number {
      font-size: 22rpx;
      color: #999;
    }
  }
}

.detail-list {
  .detail-item {
    display: flex;
    justify-content: space-between;
    padding: 20rpx 0;
    border-bottom: 1px solid #f0f0f0;
    
    &:last-child {
      border-bottom: none;
    }
    
    .label {
      font-size: 28rpx;
      color: #666;
    }
    
    .value {
      font-size: 28rpx;
      font-weight: bold;
      color: #333;
    }
  }
}
</style>
