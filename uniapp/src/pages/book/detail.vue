<template>
  <view class="container">
    <!-- 书籍信息 -->
    <view class="book-header">
      <image :src="book.cover || '/static/default-cover.png'" class="cover" mode="aspectFill" />
      <view class="book-info">
        <text class="title">{{ book.title }}</text>
        <text class="author">{{ book.author }}</text>
        <view class="rating">
          <text class="score">{{ book.rating }}</text>
          <text class="count">({{ book.ratingCount }}人评价)</text>
        </view>
      </view>
    </view>
    
    <!-- 进度条（如果在书架中） -->
    <view v-if="bookshelf" class="progress-section">
      <view class="progress-header">
        <text class="progress-title">阅读进度</text>
        <text class="progress-percent">{{ bookshelf.progress }}%</text>
      </view>
      <view class="progress-bar">
        <view class="progress-fill" :style="{ width: bookshelf.progress + '%' }"></view>
      </view>
      <text class="page-info">第 {{ bookshelf.currentPage }} 页 / 共 {{ book.pages }} 页</text>
    </view>
    
    <!-- 简介 -->
    <view class="section">
      <text class="section-title">内容简介</text>
      <text class="intro">{{ book.intro }}</text>
    </view>
    
    <!-- 操作按钮 -->
    <view class="actions">
      <view v-if="!bookshelf" class="btn-add" @click="addToBookshelf">
        <text>📚 加入书架</text>
      </view>
      <view v-else class="btn-reading" @click="startReading">
        <text>📖 继续阅读</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { bookDetail } from '@/api/book'
import { bookshelfList, bookshelfAdd } from '@/api/book'

const book = ref<any>({})
const bookshelf = ref<any>(null)
const bookId = ref(0)

const loadDetail = async () => {
  try {
    const res = await bookDetail(bookId.value)
    if (res.data) {
      book.value = res.data
    }
  } catch (e) {
    console.error(e)
  }
}

const checkBookshelf = async () => {
  try {
    const res = await bookshelfList({ status: -1 }) // -1表示全部
    if (res.data?.lists) {
      const found = res.data.lists.find((item: any) => item.bookId === bookId.value)
      if (found) {
        bookshelf.value = found
      }
    }
  } catch (e) {
    console.error(e)
  }
}

const addToBookshelf = async () => {
  try {
    await bookshelfAdd(bookId.value)
    uni.showToast({ title: '已加入书架', icon: 'success' })
    checkBookshelf()
  } catch (e) {
    uni.showToast({ title: '操作失败', icon: 'none' })
  }
}

const startReading = () => {
  uni.navigateTo({
    url: `/pages/reading/timer?bookId=${bookId.value}&bookTitle=${book.value.title}`
  })
}

onLoad((options: any) => {
  if (options.id) {
    bookId.value = parseInt(options.id)
    loadDetail()
    checkBookshelf()
  }
})
</script>

<style lang="scss" scoped>
.container {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 120rpx;
}

.book-header {
  display: flex;
  background: white;
  padding: 40rpx 30rpx;
  
  .cover {
    width: 200rpx;
    height: 280rpx;
    border-radius: 10rpx;
    background: #f0f0f0;
  }
  
  .book-info {
    flex: 1;
    margin-left: 30rpx;
    display: flex;
    flex-direction: column;
    
    .title {
      font-size: 36rpx;
      font-weight: bold;
      color: #333;
      margin-bottom: 15rpx;
    }
    
    .author {
      font-size: 28rpx;
      color: #666;
      margin-bottom: 20rpx;
    }
    
    .rating {
      display: flex;
      align-items: baseline;
      
      .score {
        font-size: 48rpx;
        font-weight: bold;
        color: #ff9500;
        margin-right: 10rpx;
      }
      
      .count {
        font-size: 24rpx;
        color: #999;
      }
    }
  }
}

.progress-section {
  background: white;
  margin: 20rpx 0;
  padding: 30rpx;
  
  .progress-header {
    display: flex;
    justify-content: space-between;
    margin-bottom: 20rpx;
    
    .progress-title {
      font-size: 28rpx;
      color: #666;
    }
    
    .progress-percent {
      font-size: 32rpx;
      font-weight: bold;
      color: #667eea;
    }
  }
  
  .progress-bar {
    height: 16rpx;
    background: #eee;
    border-radius: 8rpx;
    overflow: hidden;
    margin-bottom: 15rpx;
    
    .progress-fill {
      height: 100%;
      background: linear-gradient(to right, #667eea, #764ba2);
      transition: width 0.3s;
    }
  }
  
  .page-info {
    font-size: 26rpx;
    color: #999;
  }
}

.section {
  background: white;
  padding: 30rpx;
  margin-bottom: 20rpx;
  
  .section-title {
    font-size: 32rpx;
    font-weight: bold;
    color: #333;
    display: block;
    margin-bottom: 20rpx;
  }
  
  .intro {
    font-size: 28rpx;
    color: #666;
    line-height: 1.8;
  }
}

.actions {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: white;
  padding: 20rpx 30rpx;
  box-shadow: 0 -2rpx 10rpx rgba(0, 0, 0, 0.05);
  
  .btn-add, .btn-reading {
    background: linear-gradient(to right, #667eea, #764ba2);
    border-radius: 50rpx;
    padding: 30rpx;
    text-align: center;
    
    text {
      color: white;
      font-size: 32rpx;
      font-weight: bold;
    }
  }
}
</style>
