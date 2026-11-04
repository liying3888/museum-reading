<template>
    <view class="detail-page">
        <nav-bar title="书籍详情" />

        <!-- 加载骨架 -->
        <view v-if="loading" class="sk-wrap">
            <view class="sk-block head-sk" />
            <view class="sk-block" />
        </view>

        <block v-else-if="book">
            <!-- 头区：封面 200×280 + 书目信息（02 §3.1） -->
            <view class="head card">
                <view class="head-cover">
                    <book-cover :cover="book.cover || ''" :title="book.title" :book-id="book.id" />
                </view>
                <view class="head-info">
                    <text class="d-title">{{ book.title }}</text>
                    <text class="d-author">{{ book.author }}</text>
                    <text v-if="book.publisher" class="d-pub">{{ book.publisher }}</text>
                    <view v-if="book.category_name" class="d-cat">
                        {{ book.category_name }}
                    </view>
                    <view v-if="book.rating" class="d-rating">
                        <svg-icon name="f-star-fill" :size="24" color="var(--gold)" />
                        <text>{{ book.rating }}</text>
                    </view>
                </view>
            </view>

            <!-- 进度区（已在架时显示；更新入口唯一在底栏，此处无按钮——02 §3.1 round7） -->
            <view v-if="shelf" class="card progress-card">
                <view class="p-head">
                    <text class="p-label">阅读进度</text>
                    <text class="p-percent">{{ shelf.progress }}%</text>
                </view>
                <view class="p-bar">
                    <view class="p-fill" :style="{ width: shelf.progress + '%' }" />
                </view>
                <text class="p-page">第 {{ shelf.current_page }} 页 / 共 {{ book.pages }} 页</text>
            </view>

            <!-- 简介区：默认收起 3 行 + 展开/收起（02 §3.1） -->
            <view class="card intro-card">
                <text class="sec-title">简介</text>
                <text class="intro-text" :class="{ clamp: !introExpanded }">
                    {{ book.intro || '暂无简介' }}
                </text>
                <view v-if="introLong" class="intro-toggle" @tap="introExpanded = !introExpanded">
                    {{ introExpanded ? '收起' : '展开' }}
                </view>
            </view>
        </block>

        <!-- 书籍不存在 / 加载失败 -->
        <empty-state
            v-else
            icon="book"
            text="书籍走丢了，稍后再试试"
            action-text="重新加载"
            @action="loadAll"
        />

        <!-- 底部操作栏（四态，02 §3.1.4） -->
        <view v-if="book" class="bottom-bar">
            <block v-if="!shelf">
                <view class="btn primary grow" @tap="onAddShelf">加入书架</view>
            </block>
            <block v-else-if="shelf.status === 0">
                <view class="btn primary grow" @tap="onStartRead">开始阅读</view>
                <view class="btn ghost" @tap="showRemove = true">移出书架</view>
            </block>
            <block v-else-if="shelf.status === 1">
                <view class="btn primary grow" @tap="goTimer">继续阅读</view>
                <view class="btn ghost" @tap="showProgress = true">更新进度</view>
            </block>
            <block v-else>
                <view class="btn primary grow" @tap="onReread">重读</view>
                <view class="btn text" @tap="goShelfRead">查看书架</view>
            </block>
        </view>

        <!-- 移出书架二次确认（弹层确认类必带影响说明行 ink-2——03 §5） -->
        <view v-if="showRemove" class="mask" @tap="showRemove = false" />
        <view v-if="showRemove" class="popup center-popup">
            <text class="pp-title">移出书架</text>
            <text class="pp-desc">移出后「{{ book?.title }}」不再出现在你的书架，历史阅读记录仍会保留。</text>
            <view class="pp-btns">
                <view class="btn ghost grow" @tap="showRemove = false">取消</view>
                <view class="btn fail-solid grow" @tap="onRemove">确认移出</view>
            </view>
        </view>

        <!-- 标记已读确认（更新进度达总页数时，02 §3.2） -->
        <view v-if="showFinish" class="mask" @tap="showFinish = false" />
        <view v-if="showFinish" class="popup center-popup">
            <text class="pp-title">标记为已读？</text>
            <text class="pp-desc">已读到最后一页，将记录完成日期并归入书架「已读」。</text>
            <view class="pp-btns">
                <view class="btn ghost grow" @tap="showFinish = false">暂不</view>
                <view class="btn primary grow" @tap="onMarkFinish">标记已读</view>
            </view>
        </view>

        <!-- 更新进度抽屉（步进器，02 §3.2） -->
        <view v-if="showProgress" class="mask" @tap="showProgress = false" />
        <view v-if="showProgress" class="popup sheet-popup">
            <text class="pp-title">更新进度</text>
            <text class="pp-desc">当前读到第 {{ shelf?.current_page }} 页，共 {{ book?.pages }} 页</text>
            <view class="stepper">
                <view class="step-btn" @tap="stepPage(-10)">-10</view>
                <view class="step-btn" @tap="stepPage(-1)">-1</view>
                <view class="step-input">
                    <input
                        v-model="pageInput"
                        type="number"
                        placeholder="0"
                        placeholder-class="s-ph"
                    />
                </view>
                <view class="step-btn" @tap="stepPage(1)">+1</view>
                <view class="step-btn" @tap="stepPage(10)">+10</view>
            </view>
            <view class="pp-btns">
                <view class="btn ghost grow" @tap="showProgress = false">取消</view>
                <view class="btn primary grow" @tap="onSaveProgress">保存</view>
            </view>
        </view>
    </view>
</template>

<script lang="ts" setup>
// 书籍详情 —— S3 批 3 第二页（02 §3 + board03 + 06 §5 三体示样）
// 数据=books/detail + bookshelf/list 全量在架判定（修 S1 status:-1 传参错误）；
// 底栏四态：不在架/想读/在读/已读；更新入口唯一在底栏（round7 口径）
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { bookDetail } from '@/api/book'
import {
    bookshelfList,
    bookshelfAdd,
    bookshelfProgress,
    bookshelfStatus,
    bookshelfDelete
} from '@/api/bookshelf'
import { navigateTo } from '@/utils/util'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const bookId = ref(0)
const book = ref<any>(null)
const shelf = ref<any>(null) // 在架行（全量列表中 book_id 匹配项；null=不在架）
const loading = ref(true)

const introExpanded = ref(false)
const showRemove = ref(false)
const showFinish = ref(false)
const showProgress = ref(false)
const pageInput = ref('0')

// 简介超 3 行才给展开钮（60 字近似阈值，避免测量查询）
const introLong = computed(() => (book.value?.intro || '').length > 60)

async function loadAll() {
    loading.value = true
    try {
        book.value = await bookDetail(bookId.value)
        if (userStore.isLogin) {
            await loadShelf()
        }
    } catch (e) {
        book.value = null // 失败走空态，不白屏
    } finally {
        loading.value = false
    }
}

async function loadShelf() {
    try {
        const res: any = await bookshelfList({ page_no: 1, page_size: 200 })
        shelf.value =
            (res.lists || []).find((item: any) => item.book_id === bookId.value) || null
        if (shelf.value) pageInput.value = String(shelf.value.current_page || 0)
    } catch (e) {
        shelf.value = null
    }
}

function requireLogin(): boolean {
    if (userStore.isLogin) return true
    uni.showToast({ title: '请先登录', icon: 'none' })
    setTimeout(() => {
        navigateTo({ path: '/pages/login/login' }) // 底座登录页，登录成功回跳
    }, 600)
    return false
}

// —— 底栏四态 ——

async function onAddShelf() {
    if (!requireLogin()) return
    try {
        await bookshelfAdd(bookId.value)
        uni.showToast({ title: '已加入书架（想读）', icon: 'none' })
        await loadShelf() // 本地刷在架态，不整页刷新（02 §3.2）
    } catch (e) {
        /* 拦截器已 toast */
    }
}

async function onStartRead() {
    // 想读→在读：progress(current_page=1) 自动转在读（02 §4.1 动线）+ 跳计时
    try {
        await bookshelfProgress(bookId.value, 1)
        goTimer()
    } catch (e) {
        /* 拦截器已 toast */
    }
}

async function onReread() {
    // 重读：回在读态+进度清零（finish_time 由 status 接口清除）+ 跳计时
    try {
        await bookshelfStatus(bookId.value, 1)
        await bookshelfProgress(bookId.value, 0)
        goTimer()
    } catch (e) {
        /* 拦截器已 toast */
    }
}

function goTimer() {
    navigateTo({
        path: '/pages/reading/timer',
        query: { bookId: bookId.value, bookTitle: book.value?.title || '' }
    })
}

function goShelfRead() {
    uni.reLaunch({ url: '/pages/bookshelf/index?tab=2' })
}

async function onRemove() {
    showRemove.value = false
    try {
        await bookshelfDelete(bookId.value)
        shelf.value = null
        uni.showToast({ title: '已移出书架', icon: 'none' })
    } catch (e) {
        /* 拦截器已 toast */
    }
}

// —— 更新进度 ——

function stepPage(delta: number) {
    const pages = book.value?.pages || 0
    let next = (parseInt(pageInput.value, 10) || 0) + delta
    next = Math.max(0, Math.min(pages, next))
    pageInput.value = String(next)
}

async function onSaveProgress() {
    const pages = book.value?.pages || 0
    let page = parseInt(pageInput.value, 10)
    if (isNaN(page) || page < 0) page = 0
    if (page > pages) page = pages
    try {
        await bookshelfProgress(bookId.value, page)
        showProgress.value = false
        await loadShelf()
        if (page >= pages && pages > 0 && shelf.value?.status !== 2) {
            showFinish.value = true // 达总页数→确认转已读（02 §3.2）
        } else {
            uni.showToast({ title: '进度已更新', icon: 'none' })
        }
    } catch (e) {
        /* 拦截器已 toast */
    }
}

async function onMarkFinish() {
    showFinish.value = false
    try {
        await bookshelfStatus(bookId.value, 2)
        await loadShelf()
        uni.showToast({ title: '已标记为已读', icon: 'none' })
    } catch (e) {
        /* 拦截器已 toast */
    }
}

onLoad((options: any) => {
    bookId.value = Number(options?.id || 0)
    if (!bookId.value) {
        book.value = null
        loading.value = false
        return
    }
    loadAll()
})
</script>

<style lang="scss" scoped>
.detail-page {
    min-height: 100vh;
    background: var(--paper);
    padding: 16rpx 32rpx 240rpx; // 底部留底栏高
}

.card {
    background: var(--card);
    border: 1px solid var(--line);
    border-radius: 12rpx;
    box-shadow: var(--card-shadow);
    padding: 24rpx;
    margin-top: 24rpx;
}

/* 头区 */
.head {
    display: flex;
    gap: 32rpx;

    .head-cover {
        width: 200rpx;
        height: 280rpx;
        flex-shrink: 0;
    }
    .head-info {
        flex: 1;
        min-width: 0;
        display: flex;
        flex-direction: column;

        .d-title {
            font-family: var(--font-serif);
            font-size: 36rpx;
            line-height: 1.4;
            color: var(--ink);
            font-weight: 600;
        }
        .d-author {
            margin-top: 12rpx;
            font-size: 26rpx;
            color: var(--ink-2);
        }
        .d-pub {
            margin-top: 8rpx;
            font-size: 24rpx;
            color: var(--ink-2);
        }
        .d-cat {
            align-self: flex-start;
            margin-top: 16rpx;
            padding: 4rpx 16rpx;
            border-radius: 8rpx;
            background: var(--cinnabar-soft);
            color: var(--cinnabar);
            font-size: 22rpx;
        }
        .d-rating {
            margin-top: auto;
            display: flex;
            align-items: center;
            gap: 8rpx;
            color: var(--gold);
            font-size: 28rpx;
        }
    }
}

/* 进度区 */
.progress-card {
    .p-head {
        display: flex;
        align-items: baseline;
        justify-content: space-between;

        .p-label {
            font-size: 28rpx;
            color: var(--ink);
            font-weight: 600;
        }
        .p-percent {
            font-family: var(--font-serif);
            font-size: 34rpx;
            color: var(--cinnabar);
        }
    }
    .p-bar {
        margin-top: 16rpx;
        height: 8rpx;
        border-radius: 4rpx;
        background: var(--paper-deep);
        overflow: hidden;

        .p-fill {
            height: 100%;
            border-radius: 4rpx;
            background: var(--cinnabar);
        }
    }
    .p-page {
        display: block;
        margin-top: 12rpx;
        font-size: 24rpx;
        color: var(--ink-2);
    }
}

/* 简介区 */
.intro-card {
    .sec-title {
        font-family: var(--font-serif);
        font-size: 30rpx;
        color: var(--ink);
        font-weight: 600;
    }
    .intro-text {
        display: block;
        margin-top: 16rpx;
        font-size: 26rpx;
        line-height: 1.8;
        color: var(--ink-2);

        &.clamp {
            display: -webkit-box;
            -webkit-box-orient: vertical;
            -webkit-line-clamp: 3;
            overflow: hidden;
        }
    }
    .intro-toggle {
        margin-top: 12rpx;
        font-size: 24rpx;
        color: var(--indigo);
    }
}

/* 底部操作栏（固定） */
.bottom-bar {
    position: fixed;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 30;
    display: flex;
    gap: 24rpx;
    padding: 16rpx 32rpx calc(16rpx + env(safe-area-inset-bottom));
    background: var(--card);
    border-top: 1px solid var(--line);
}

/* 按钮基线（03 §4）：主=黛蓝实底白字 88rpx；次=描边；危险实底 fail */
.btn {
    height: 88rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 12rpx;
    font-size: 28rpx;

    &.primary {
        background: var(--indigo);
        color: #ffffff;

        &:active {
            background: var(--indigo-deep);
        }
    }
    &.ghost {
        min-width: 200rpx;
        padding: 0 32rpx;
        border: 1px solid var(--indigo);
        color: var(--indigo);
        background: transparent;
    }
    &.fail-solid {
        background: var(--fail);
        color: #ffffff;
    }
    &.text {
        min-width: 160rpx;
        color: var(--indigo);
    }
    &.grow {
        flex: 1;
    }
}

/* 弹层（03 §5：顶圆角 24rpx；确认类带影响说明行） */
.mask {
    position: fixed;
    inset: 0;
    z-index: 40;
    background: rgba(30, 43, 56, 0.45);
}

.popup {
    position: fixed;
    z-index: 50;
    background: var(--card);
    border-radius: 24rpx;
    padding: 40rpx 32rpx calc(32rpx + env(safe-area-inset-bottom));

    .pp-title {
        display: block;
        font-family: var(--font-serif);
        font-size: 32rpx;
        color: var(--ink);
        font-weight: 600;
    }
    .pp-desc {
        display: block;
        margin-top: 16rpx;
        font-size: 26rpx;
        line-height: 1.7;
        color: var(--ink-2); // 影响说明淡墨档（03 round3 定界）
    }
    .pp-btns {
        display: flex;
        gap: 24rpx;
        margin-top: 40rpx;
    }
}

.center-popup {
    left: 64rpx;
    right: 64rpx;
    top: 50%;
    transform: translateY(-50%);
}

.sheet-popup {
    left: 0;
    right: 0;
    bottom: 0;
    border-radius: 24rpx 24rpx 0 0;

    .stepper {
        margin-top: 32rpx;
        display: flex;
        align-items: center;
        gap: 16rpx;

        .step-btn {
            width: 88rpx;
            height: 88rpx;
            display: flex;
            align-items: center;
            justify-content: center;
            border: 1px solid var(--line);
            border-radius: 12rpx;
            background: var(--paper);
            font-size: 26rpx;
            color: var(--ink);

            &:active {
                background: var(--paper-deep);
            }
        }
        .step-input {
            flex: 1;
            height: 88rpx;
            display: flex;
            align-items: center;
            justify-content: center;
            border: 1px solid var(--line);
            border-radius: 12rpx;

            input {
                width: 100%;
                text-align: center;
                font-family: var(--font-serif);
                font-size: 40rpx;
                color: var(--ink);
            }
            :deep(.s-ph) {
                color: var(--ink-3);
            }
        }
    }
}

/* 骨架 */
.sk-wrap {
    .sk-block {
        height: 320rpx;
        border-radius: 12rpx;
        background: var(--paper-deep);
        margin-top: 24rpx;
        animation: sk-pulse 1.2s ease-in-out infinite;

        &.head-sk {
            height: 360rpx;
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
</style>
