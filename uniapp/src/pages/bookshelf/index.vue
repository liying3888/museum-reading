<template>
  <view class="container">
    <!-- 顶部Tab -->
    <view class="tabs">
      <view 
        v-for="(tab, index) in tabs" 
        :key="index"
        :class="['tab-item', { active: currentTab === index }]"
        @click="switchTab(index)"
      >
        <text>{{ tab }}</text>
      </view>
    </view>
    
    <!-- 书籍列表 -->
    <view class="book-list">
      <view 
        v-for="book in books" 
        :key="book.id" 
        class="book-card"
        @click="goToDetail(book)"
      >
        <image :src="book.cover || '/static/default-cover.png'" class="cover" mode="aspectFill" />
        <view class="book-info">
          <text class="title">{{ book.title }}</text>
          <text class="author">{{ book.author }}</text>
          <view class="progress-info">
            <view class="progress-bar">
              <view class="progress-fill" :style="{ width: book.progress + '%' }"></view>
            </view>
            <text class="progress-text">{{ book.progress }}%</text>
          </view>
          <text v-if="book.currentPage" class="page-info">
            第 {{ book.currentPage }} 页 / 共 {{ book.pages }} 页
          </text>
        </view>
      </view>
      
      <view v-if="books.length === 0" class="empty">
        <image src="/static/empty.png" class="empty-icon" />
        <text>{{ emptyText }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { bookshelfList } from '@/api/book'

const tabs = ['想读', '在读', '已读']
const currentTab = ref(1) // 默认显示在读
const books = ref<any[]>([])

const statusMap: Record<number, number> = {
  0: 0, // 想读
  1: 1, // 在读
  2: 2, // 已读
}

const emptyTextMap: Record<number, string> = {
  0: '还没有想读的书籍',
  1: '还没有在读书籍',
  2: '还没有读完的书籍',
}

const emptyText = ref(emptyTextMap[1])

const switchTab = (index: number) => {
  currentTab.value = index
  emptyText.value = emptyTextMap[index]
  loadBooks()
}

const loadBooks = async () => {
  try {
    const res = await bookshelfList({ status: statusMap[currentTab.value] })
    if (res.data?.lists) {
      books.value = res.data.lists
    }
  } catch (e) {
    console.error(e)
  }
}

const goToDetail = (book: any) => {
  uni.navigateTo({
    url: `/pages/book/detail?id=${book.bookId}`
  })
}

onLoad(() => {
  loadBooks()
})
</script>

<style lang="scss" scoped>
.container {
  min-height: 100vh;
  background: #f5f5f5;
}

.tabs {
  display: flex;
  background: white;
  padding: 20rpx 30rpx;
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.05);
  
  .tab-item {
    flex: 1;
    text-align: center;
    padding: 20rpx;
    border-radius: 50rpx;
    font-size: 28rpx;
    color: #666;
    
    &.active {
      background: #667eea;
      color: white;
      font-weight: bold;
    }
  }
}

.book-list {
  padding: 20rpx 30rpx;
}

.book-card {
  display: flex;
  background: white;
  border-radius: 20rpx;
  padding: 30rpx;
  margin-bottom: 20rpx;
  
  .cover {
    width: 160rpx;
    height: 220rpx;
    border-radius: 10rpx;
    background: #f0f0f0;
  }
  
  .book-info {
    flex: 1;
    margin-left: 30rpx;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    
    .title {
      font-size: 32rpx;
      font-weight: bold;
      color: #333;
      margin-bottom: 10rpx;
    }
    
    .author {
      font-size: 26rpx;
      color: #999;
      margin-bottom: 20rpx;
    }
    
    .progress-info {
      display: flex;
      align-items: center;
      
      .progress-bar {
        flex: 1;
        height: 12rpx;
        background: #eee;
        border-radius: 6rpx;
        overflow: hidden;
        margin-right: 20rpx;
        
        .progress-fill {
          height: 100%;
          background: #667eea;
          transition: width 0.3s;
        }
      }
      
      .progress-text {
        font-size: 24rpx;
        color: #666;
      }
    }
    
    .page-info {
      font-size: 24rpx;
      color: #999;
      margin-top: 10rpx;
    }
  }
}

.empty {
  text-align: center;
  padding: 100rpx 0;
  
  .empty-icon {
    width: 200rpx;
    height: 200rpx;
    margin-bottom: 30rpx;
  }
  
  text {
    font-size: 28rpx;
    color: #999;
  }
}
</style>
