<template>
    <div class="book-board" v-loading="loading">
        <!-- 四数卡（04 §1 看板口径） -->
        <div class="stat-grid">
            <el-card class="!border-none stat-card" shadow="never">
                <div class="stat-num">{{ data.totalBookCount ?? 0 }}</div>
                <div class="stat-label">书籍总数</div>
            </el-card>
            <el-card class="!border-none stat-card" shadow="never">
                <div class="stat-num">{{ data.bookCount ?? 0 }}</div>
                <div class="stat-label">在架书籍</div>
            </el-card>
            <el-card class="!border-none stat-card" shadow="never">
                <div class="stat-num">{{ data.userCount ?? 0 }}</div>
                <div class="stat-label">读者数</div>
            </el-card>
            <el-card class="!border-none stat-card" shadow="never">
                <div class="stat-num accent">{{ data.todayReadingCount ?? 0 }}</div>
                <div class="stat-label">今日阅读人次</div>
            </el-card>
        </div>

        <!-- 次级指标 -->
        <el-card class="!border-none mt-4" shadow="never">
            <div class="sub-grid">
                <div class="sub-item">
                    <span class="sub-num">{{ data.categoryCount ?? 0 }}</span>
                    <span class="sub-label">分类数</span>
                </div>
                <div class="sub-item">
                    <span class="sub-num">{{ data.bookshelfCount ?? 0 }}</span>
                    <span class="sub-label">书架在藏</span>
                </div>
                <div class="sub-item">
                    <span class="sub-num">{{ data.readingCount ?? 0 }}</span>
                    <span class="sub-label">累计阅读记录</span>
                </div>
            </div>
        </el-card>
    </div>
</template>

<script lang="ts" setup name="bookBoard">
import { statisticsOverview } from '@/api/book'

const loading = ref(false)
const data = ref<any>({})

const getLists = async () => {
    loading.value = true
    try {
        data.value = await statisticsOverview()
    } finally {
        loading.value = false
    }
}

onActivated(() => {
    getLists()
})

getLists()
</script>

<style lang="scss" scoped>
/* 04 §6 书斋 token 同源：衬线数字+黛蓝主数+朱砂点睛（今日人次） */
.book-board {
    .stat-grid {
        display: grid;
        grid-template-columns: repeat(4, 1fr);
        gap: 16px;
    }
    .stat-card {
        text-align: center;
        padding: 8px 0;
        .stat-num {
            font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
            font-size: 34px;
            font-weight: 700;
            color: #2f4154; // 黛蓝
            line-height: 1.4;
        }
        .stat-num.accent {
            color: #b03a2e; // 朱砂点睛（单屏 ≤2 处）
        }
        .stat-label {
            font-size: 13px;
            color: #6b675e;
            margin-top: 4px;
        }
    }
    .sub-grid {
        display: flex;
        justify-content: space-around;
        padding: 8px 0;
        .sub-item {
            text-align: center;
        }
        .sub-num {
            display: block;
            font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
            font-size: 22px;
            font-weight: 600;
            color: #2c2a26;
        }
        .sub-label {
            font-size: 12px;
            color: #a8a296;
        }
    }
}
</style>
