<template>
    <view class="library-page">
        <view class="page-head">
            <text class="head-title">书库</text>
        </view>

        <!-- 吸顶筛选组：搜索（防抖500ms）+分类 chips（02 §2.1） -->
        <view class="filter-bar">
            <view class="search-box">
                <view class="s-icon">
                    <view class="ring" />
                    <view class="handle" />
                </view>
                <input
                    v-model="keyword"
                    class="s-input"
                    type="text"
                    placeholder="搜书名 / 作者"
                    placeholder-class="s-ph"
                    confirm-type="search"
                    @input="onKeyword"
                    @confirm="onSearch"
                />
                <view v-if="keyword" class="s-clear" @tap="clearKeyword">✕</view>
            </view>
            <scroll-view class="chips" scroll-x :show-scrollbar="false">
                <view class="chips-row">
                    <view
                        class="chip"
                        :class="{ on: activeCategoryId === null }"
                        @tap="switchCategory(null)"
                    >
                        全部
                    </view>
                    <view
                        v-for="c in categories"
                        :key="c.id"
                        class="chip"
                        :class="{ on: activeCategoryId === c.id }"
                        @tap="switchCategory(c.id)"
                    >
                        {{ c.name }}
                    </view>
                </view>
            </scroll-view>
        </view>

        <view v-if="loadFailed" class="fail-bar" @tap="resetList">
            加载失败，点击重试
        </view>

        <view class="book-list">
            <!-- 首屏骨架（02 §0.2 状态四件套·加载） -->
            <block v-if="firstLoading">
                <view v-for="n in 3" :key="'sk' + n" class="book-card sk-card">
                    <view class="sk-cover" />
                    <view class="sk-lines">
                        <view class="sk-line w60" />
                        <view class="sk-line w40" />
                        <view class="sk-tag" />
                    </view>
                </view>
            </block>
            <block v-else>
                <view
                    v-for="b in books"
                    :key="b.id"
                    class="book-card"
                    @tap="goDetail(b.id)"
                >
                    <view class="card-cover">
                        <book-cover :cover="b.cover || ''" :title="b.title" :book-id="b.id" />
                    </view>
                    <view class="card-info">
                        <text class="b-title">{{ b.title }}</text>
                        <text class="b-author">{{ b.author }}</text>
                        <view v-if="b.category_name" class="b-cat">
                            {{ b.category_name }}
                        </view>
                        <view class="b-meta">
                            <view v-if="b.rating" class="b-rating">
                                <svg-icon name="f-star-fill" :size="22" color="var(--gold)" />
                                <text>{{ b.rating }}</text>
                            </view>
                            <text class="b-pages">共 {{ b.pages }} 页</text>
                        </view>
                    </view>
                </view>

                <empty-state
                    v-if="!books.length"
                    icon="search"
                    text="没有找到相关书籍"
                    action-text="清除筛选"
                    @action="clearFilter"
                />
                <view v-if="books.length && finished" class="list-end">
                    没有更多了
                </view>
                <view v-if="loadingMore" class="list-end">加载中…</view>
            </block>
        </view>

        <tabbar />
    </view>
</template>

<script lang="ts" setup>
// 书库页 —— S3 批 3 第一页（02 §2 + board02 + 06 §5 示样）
// 主任务：找到下一本要读的书（S1 选书死锁的解）
// 排序=后端上架时间倒序+录入序 tie-break（06 §5 round7 口径）；chips 数据源=B1 首屏 categories 聚合键
import { ref } from 'vue'
import { onLoad, onReachBottom } from '@dcloudio/uni-app'
import { bookList } from '@/api/book'
import { navigateTo } from '@/utils/util'

const PAGE_SIZE = 15

const keyword = ref('')
const activeCategoryId = ref<number | null>(null)
const categories = ref<{ id: number; name: string }[]>([])
const books = ref<any[]>([])
const pageNo = ref(1)
const total = ref(0)
const firstLoading = ref(true)
const loadingMore = ref(false)
const loadFailed = ref(false)

const finished = ref(false)

let debounceTimer: ReturnType<typeof setTimeout> | null = null

// 输入防抖 500ms（02 §2.1），confirm 键立即搜
function onKeyword() {
    if (debounceTimer) clearTimeout(debounceTimer)
    debounceTimer = setTimeout(() => resetList(), 500)
}
function onSearch() {
    if (debounceTimer) clearTimeout(debounceTimer)
    resetList()
}
function clearKeyword() {
    if (debounceTimer) clearTimeout(debounceTimer)
    keyword.value = ''
    resetList()
}
function switchCategory(id: number | null) {
    if (activeCategoryId.value === id) return
    activeCategoryId.value = id
    resetList()
}
function clearFilter() {
    keyword.value = ''
    activeCategoryId.value = null
    resetList()
}

async function resetList() {
    pageNo.value = 1
    books.value = []
    total.value = 0
    finished.value = false
    firstLoading.value = true
    await fetchPage()
}

async function fetchPage() {
    if (loadingMore.value) return
    loadingMore.value = true
    loadFailed.value = false
    try {
        const res: any = await bookList({
            page_no: pageNo.value,
            page_size: PAGE_SIZE,
            category_id: activeCategoryId.value ?? undefined,
            keyword: keyword.value.trim() || undefined
        })
        const lists: any[] = res.lists || []
        if (pageNo.value === 1) {
            books.value = lists
            if (res.categories) categories.value = res.categories
        } else {
            books.value = books.value.concat(lists)
        }
        total.value = res.count || 0
        finished.value = books.value.length >= total.value
    } catch (e) {
        loadFailed.value = true // 失败保留旧数据/空态，不白屏（02 §0.2）
    } finally {
        loadingMore.value = false
        firstLoading.value = false
    }
}

onLoad(() => {
    resetList()
})

onReachBottom(() => {
    if (firstLoading.value || loadFailed.value || finished.value) return
    pageNo.value += 1
    fetchPage()
})

function goDetail(id: number) {
    navigateTo({ path: '/pages/book/detail', query: { id } })
}
</script>

<style lang="scss" scoped>
.library-page {
    min-height: 100vh;
    background: var(--paper);
    padding-bottom: 220rpx; // 自绘 tabbar 留白
}

.page-head {
    padding: calc(var(--status-bar-height, 0px) + 24rpx) 32rpx 8rpx;

    .head-title {
        font-family: var(--font-serif);
        font-size: 44rpx;
        color: var(--ink);
        font-weight: 600;
    }
}

.filter-bar {
    position: sticky;
    top: 0;
    z-index: 10;
    background: var(--paper);
    padding: 16rpx 32rpx 16rpx;
}

.search-box {
    display: flex;
    align-items: center;
    gap: 16rpx;
    height: 80rpx;
    padding: 0 24rpx;
    background: var(--card);
    border: 1px solid var(--line);
    border-radius: 40rpx;

    .s-icon {
        position: relative;
        width: 32rpx;
        height: 32rpx;
        flex-shrink: 0;

        .ring {
            position: absolute;
            left: 2rpx;
            top: 2rpx;
            width: 20rpx;
            height: 20rpx;
            border: 2rpx solid var(--ink-3);
            border-radius: 50%;
        }
        .handle {
            position: absolute;
            right: 0;
            bottom: 2rpx;
            width: 12rpx;
            height: 2rpx;
            background: var(--ink-3);
            transform: rotate(45deg);
            transform-origin: right center;
        }
    }

    .s-input {
        flex: 1;
        min-width: 0;
        font-size: 28rpx;
        color: var(--ink);
    }
    :deep(.s-ph) {
        color: var(--ink-3); // placeholder 属占位层，枯墨合规（03 §1.2）
    }
    .s-clear {
        flex-shrink: 0;
        width: 40rpx;
        height: 40rpx;
        display: flex;
        align-items: center;
        justify-content: center;
        color: var(--ink-3);
        font-size: 24rpx;
    }
}

.chips {
    margin-top: 20rpx;
    white-space: nowrap;

    .chips-row {
        display: flex;
        gap: 16rpx;
        padding-bottom: 4rpx;
    }
    .chip {
        flex-shrink: 0;
        padding: 10rpx 28rpx;
        border-radius: 32rpx;
        background: var(--paper-deep);
        color: var(--ink-2);
        font-size: 24rpx;

        &.on {
            background: var(--cinnabar-soft);
            color: var(--cinnabar);
        }
    }
}

.fail-bar {
    margin: 16rpx 32rpx 0;
    padding: 16rpx 24rpx;
    border-radius: 8rpx;
    background: rgba(184, 134, 59, 0.12);
    color: var(--warn);
    font-size: 24rpx;
    text-align: center;
}

.book-list {
    padding: 8rpx 32rpx 0;
}

.book-card {
    display: flex;
    gap: 24rpx;
    background: var(--card);
    border: 1px solid var(--line);
    border-radius: 12rpx;
    box-shadow: var(--card-shadow);
    padding: 24rpx;
    margin-top: 24rpx;

    .card-cover {
        width: 160rpx;
        height: 220rpx;
        flex-shrink: 0;
    }

    .card-info {
        flex: 1;
        min-width: 0;
        display: flex;
        flex-direction: column;

        .b-title {
            font-family: var(--font-serif);
            font-size: 34rpx;
            line-height: 1.4;
            color: var(--ink);
            font-weight: 600;
            display: -webkit-box;
            -webkit-box-orient: vertical;
            -webkit-line-clamp: 2;
            overflow: hidden;
        }
        .b-author {
            margin-top: 8rpx;
            font-size: 24rpx;
            color: var(--ink-2);
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
        }
        .b-cat {
            align-self: flex-start;
            margin-top: 16rpx;
            padding: 4rpx 16rpx;
            border-radius: 8rpx;
            background: var(--cinnabar-soft);
            color: var(--cinnabar);
            font-size: 22rpx;
        }
        .b-meta {
            margin-top: auto;
            display: flex;
            align-items: baseline;
            gap: 24rpx;

            .b-rating {
                display: flex;
                align-items: center;
                gap: 4rpx;
                font-size: 24rpx;
                color: var(--gold);
            }
            .b-pages {
                font-size: 24rpx;
                color: var(--ink-2);
            }
        }
    }
}

/* 骨架卡（灰阶占位） */
.sk-card {
    .sk-cover {
        width: 160rpx;
        height: 220rpx;
        flex-shrink: 0;
        border-radius: 8rpx;
        background: var(--paper-deep);
        animation: sk-pulse 1.2s ease-in-out infinite;
    }
    .sk-lines {
        flex: 1;
        display: flex;
        flex-direction: column;
        gap: 20rpx;
        padding-top: 8rpx;

        .sk-line {
            height: 24rpx;
            border-radius: 4rpx;
            background: var(--paper-deep);
            animation: sk-pulse 1.2s ease-in-out infinite;
        }
        .w60 {
            width: 60%;
        }
        .w40 {
            width: 40%;
        }
        .sk-tag {
            width: 96rpx;
            height: 32rpx;
            border-radius: 8rpx;
            background: var(--paper-deep);
            animation: sk-pulse 1.2s ease-in-out infinite;
        }
    }
}

@keyframes sk-pulse {
    0%,
    100% {
        opacity: 1;
    }
    50% {
        opacity: 0.55;
    }
}

.list-end {
    padding: 32rpx 0;
    text-align: center;
    font-size: 24rpx;
    color: var(--ink-3);
}
</style>
