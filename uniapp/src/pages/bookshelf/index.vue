<template>
    <view class="shelf-page">
        <!-- 页首 -->
        <view class="page-head">
            <text class="head-title">书架</text>
        </view>

        <!-- 未登录引导卡（02 §0.2：Tab 中书架需登录） -->
        <view v-if="!isLogin" class="login-card card">
            <text class="lg-text">登录后管理你的书与进度</text>
            <view class="btn primary sm" @tap="goLogin">登录</view>
        </view>

        <block v-else>
            <!-- 三段 Tab 吸顶：计数角标+黛蓝下划条（02 §4.1①，默认「在读」） -->
            <view class="tabs">
                <view
                    v-for="(t, i) in TAB_DEFS"
                    :key="t.status"
                    class="tab-item"
                    :class="{ on: activeTab === i }"
                    @tap="switchTab(i)"
                >
                    <text class="t-name">{{ t.name }}</text>
                    <text class="t-count">{{ counts[i] }}</text>
                    <view class="t-bar" />
                </view>
            </view>

            <!-- 在读 Tab 专属找书件：搜索+长按置顶（02 §4.1②） -->
            <view v-if="activeTab === 1" class="shelf-tools">
                <view class="search-box">
                    <view class="s-icon"><view class="ring" /><view class="handle" /></view>
                    <input
                        class="s-input"
                        v-model="keyword"
                        placeholder="搜书架内书名"
                        placeholder-class="s-ph"
                        confirm-type="search"
                    />
                    <view v-if="keyword" class="s-clear" @tap="keyword = ''">✕</view>
                </view>
                <text class="pin-tip">长按卡片可置顶</text>
            </view>

            <!-- 书卡列表（单列，02 §4.1③） -->
            <view class="shelf-list">
                <!-- 想读卡 -->
                <view
                    v-for="item in shown"
                    :key="item.book_id"
                    class="book-card card"
                    @longpress="onLongPress(item)"
                >
                    <view class="bc-cover">
                        <book-cover
                            :cover="item.book?.cover || ''"
                            :title="item.book?.title || ''"
                            :book-id="item.book_id"
                        />
                        <view v-if="item.pinned" class="pin-badge">顶</view>
                    </view>
                    <view class="bc-info">
                        <text class="b-title">{{ item.book?.title }}</text>
                        <text class="b-author">{{ item.book?.author }}</text>

                        <!-- 在读卡：进度条+页码行 -->
                        <block v-if="activeTab === 1">
                            <view class="b-bar">
                                <view class="b-fill" :style="{ width: item.progress + '%' }" />
                            </view>
                            <text class="b-page">第 {{ item.current_page }} 页 / 共 {{ item.book?.pages }} 页</text>
                        </block>

                        <!-- 已读卡：读完日期 -->
                        <text v-if="activeTab === 2" class="b-done">
                            读完于 {{ finishDate(item) }}
                        </text>

                        <view class="b-btns">
                            <block v-if="activeTab === 0">
                                <view class="btn text sm" @tap.stop="onRemoveAsk(item)">移出</view>
                                <view class="btn primary sm" @tap.stop="onStartRead(item)">开始阅读</view>
                            </block>
                            <block v-else-if="activeTab === 1">
                                <view class="btn ghost sm" @tap.stop="onUpdateProgress(item)">更新进度</view>
                                <view class="btn primary sm" @tap.stop="goTimer(item)">继续阅读</view>
                            </block>
                            <block v-else>
                                <view class="btn text sm" @tap.stop="goDetail(item)">详情</view>
                                <view class="btn ghost sm" @tap.stop="onReread(item)">重读</view>
                            </block>
                        </view>
                    </view>
                </view>

                <!-- 空态（分 Tab 文案，02 §4.2） -->
                <empty-state
                    v-if="!loading && shown.length === 0"
                    mode="inline"
                    :icon="activeTab === 0 ? 'search' : 'book'"
                    :text="EMPTY_TEXT[activeTab]"
                    :action-text="activeTab === 0 ? '去书库逛逛' : ''"
                    @action="goLibrary"
                />

                <view v-if="loading" class="loading-tip">加载中…</view>
            </view>
        </block>

        <!-- 移出二次确认（02 §4.2：二次确认+影响说明） -->
        <view v-if="removeTarget" class="mask" @tap="removeTarget = null" />
        <view v-if="removeTarget" class="popup center-popup">
            <text class="pp-title">移出书架</text>
            <text class="pp-desc">移出后「{{ removeTarget.book?.title }}」不再出现在你的书架，历史阅读记录仍会保留。</text>
            <view class="pp-btns">
                <view class="btn ghost grow" @tap="removeTarget = null">取消</view>
                <view class="btn fail-solid grow" @tap="onRemoveConfirm">确认移出</view>
            </view>
        </view>

        <tabbar />
    </view>
</template>

<script lang="ts" setup>
// 书架页 —— S3 批 3 第四页（02 §4 + board04）
// 主任务：管理我的书与进度。三段 Tab（默认在读）+在读找书件（搜索+长按置顶）+三态书卡
// 修 S1：圆块 Tab→下划条/状态传参错/无置顶。排序=置顶优先+update_time 倒序（后端 S1）
import { ref, computed } from 'vue'
import { onLoad, onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import {
    bookshelfList,
    bookshelfProgress,
    bookshelfStatus,
    bookshelfDelete,
    bookshelfPin
} from '@/api/bookshelf'
import { navigateTo } from '@/utils/util'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const isLogin = computed(() => userStore.isLogin)

const TAB_DEFS = [
    { name: '想读', status: 0 },
    { name: '在读', status: 1 },
    { name: '已读', status: 2 }
]
const EMPTY_TEXT = [
    '还没有想读的书，去书库逛逛',
    '从书库选一本书开始读吧',
    '读完一本书，就会在这里留下印记'
]

const activeTab = ref(1) // 默认「在读」（02 §4.1①）
const keyword = ref('')
const all = ref<any[]>([]) // 全量一次拉，三 Tab 本地分组（个人书架量级）
const loading = ref(false)
const removeTarget = ref<any>(null)

const counts = computed(() =>
    TAB_DEFS.map((t) => all.value.filter((x) => x.status === t.status).length)
)

// 当前列表：Tab 过滤 +（在读 Tab）置顶优先其余 update_time 倒序（后端序已按此，本地搜索后重排）
const shown = computed(() => {
    let list = all.value.filter((x) => x.status === TAB_DEFS[activeTab.value].status)
    if (keyword.value.trim()) {
        const kw = keyword.value.trim()
        list = list.filter((x) => (x.book?.title || '').includes(kw))
    }
    return list
})

function finishDate(item: any): string {
    if (!item.finish_time) return '—'
    const d = new Date(item.finish_time * 1000)
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

async function loadAll() {
    if (!userStore.isLogin) return
    loading.value = true
    try {
        const res: any = await bookshelfList({ page_no: 1, page_size: 200 })
        all.value = res.lists || []
    } catch (e) {
        all.value = [] // 失败保空态不白屏
    } finally {
        loading.value = false
    }
}

function switchTab(i: number) {
    activeTab.value = i
    keyword.value = ''
}

// —— 动作 ——

function onStartRead(item: any) {
    // progress(1) 自动转在读（02 §4.2）+ 跳计时
    bookshelfProgress(item.book_id, 1)
        .then(() => {
            goTimer(item)
        })
        .catch(() => {})
}

function goTimer(item: any) {
    navigateTo({
        path: '/pages/reading/timer',
        query: { bookId: item.book_id, bookTitle: item.book?.title || '' }
    })
}

function onUpdateProgress(item: any) {
    // 进度更新唯一入口在详情页底栏（round7）——书架入口跳详情
    navigateTo({ path: '/pages/book/detail', query: { id: item.book_id } })
}

function onReread(item: any) {
    Promise.all([bookshelfStatus(item.book_id, 1), bookshelfProgress(item.book_id, 0)])
        .then(() => {
            activeTab.value = 1
            goTimer(item)
        })
        .catch(() => {})
}

function goDetail(item: any) {
    navigateTo({ path: '/pages/book/detail', query: { id: item.book_id } })
}

function onLongPress(item: any) {
    // 长按置顶：即时生效+Toast 确认（round3 P3③，在读 Tab 找书件）
    const next = item.pinned ? 0 : 1
    bookshelfPin(item.book_id, next as 0 | 1)
        .then(() => {
            item.pinned = next
            uni.showToast({ title: next ? '已置顶' : '已取消置顶', icon: 'none' })
        })
        .catch(() => {})
}

function onRemoveAsk(item: any) {
    removeTarget.value = item
}

function onRemoveConfirm() {
    const item = removeTarget.value
    removeTarget.value = null
    bookshelfDelete(item.book_id)
        .then(() => {
            all.value = all.value.filter((x) => x.book_id !== item.book_id)
            uni.showToast({ title: '已移出书架', icon: 'none' })
        })
        .catch(() => {})
}

function goLibrary() {
    uni.reLaunch({ url: '/pages/library/index' })
}

function goLogin() {
    navigateTo({ path: '/pages/login/login' })
}

onLoad((options: any) => {
    // 外部定向：详情「查看书架」tab=2 / 首页「全部在读」tab=1
    const t = Number(options?.tab)
    if (t === 0 || t === 1 || t === 2) activeTab.value = t
})

onShow(() => {
    loadAll() // 详情页改进度/状态后回来即见最新
})

onPullDownRefresh(() => {
    loadAll()
    setTimeout(() => uni.stopPullDownRefresh(), 600)
})
</script>

<style lang="scss" scoped>
.shelf-page {
    min-height: 100vh;
    background: var(--paper);
    padding: 0 32rpx 220rpx;
}

.page-head {
    padding: calc(var(--status-bar-height, 0px) + 24rpx) 8rpx 8rpx;

    .head-title {
        font-family: var(--font-serif);
        font-size: 44rpx;
        color: var(--ink);
        font-weight: 600;
    }
}

.card {
    background: var(--card);
    border: 1px solid var(--line);
    border-radius: 12rpx;
    box-shadow: var(--card-shadow);
}

.login-card {
    margin-top: 24rpx;
    padding: 40rpx 32rpx;
    display: flex;
    align-items: center;
    justify-content: space-between;

    .lg-text {
        font-size: 28rpx;
        color: var(--ink-2);
    }
}

/* 三段 Tab（吸顶） */
.tabs {
    position: sticky;
    top: 0;
    z-index: 20;
    display: flex;
    background: var(--paper);
    padding: 16rpx 0 0;

    .tab-item {
        position: relative;
        flex: 1;
        display: flex;
        align-items: baseline;
        justify-content: center;
        gap: 8rpx;
        padding: 16rpx 0 20rpx;

        .t-name {
            font-size: 30rpx;
            color: var(--ink-2);
        }
        .t-count {
            font-family: var(--font-serif);
            font-size: 26rpx;
            color: var(--ink-3);
        }
        .t-bar {
            position: absolute;
            left: 50%;
            bottom: 0;
            transform: translateX(-50%);
            width: 48rpx;
            height: 6rpx;
            border-radius: 3rpx;
            background: transparent;
        }
        &.on {
            .t-name {
                color: var(--indigo);
                font-weight: 600;
            }
            .t-count {
                color: var(--indigo);
            }
            .t-bar {
                background: var(--indigo); // 黛蓝下划指示条（修 S1 圆块 Tab）
            }
        }
    }
}

/* 在读 Tab 找书件 */
.shelf-tools {
    display: flex;
    align-items: center;
    gap: 16rpx;
    padding: 8rpx 0 16rpx;

    .pin-tip {
        flex-shrink: 0;
        font-size: 22rpx;
        color: var(--ink-3);
    }
}

.search-box {
    flex: 1;
    height: 80rpx;
    display: flex;
    align-items: center;
    gap: 12rpx;
    padding: 0 24rpx;
    background: var(--card);
    border: 1px solid var(--line);
    border-radius: 40rpx;

    .s-icon {
        position: relative;
        width: 26rpx;
        height: 26rpx;
        flex-shrink: 0;

        .ring {
            position: absolute;
            left: 0;
            top: 0;
            width: 18rpx;
            height: 18rpx;
            border: 3rpx solid var(--ink-3);
            border-radius: 50%;
        }
        .handle {
            position: absolute;
            right: 0;
            bottom: 0;
            width: 10rpx;
            height: 3rpx;
            background: var(--ink-3);
            transform: rotate(45deg);
        }
    }
    .s-input {
        flex: 1;
        font-size: 26rpx;
        color: var(--ink);
    }
    .s-clear {
        padding: 8rpx;
        font-size: 24rpx;
        color: var(--ink-3);
    }
}

/* 书卡 */
.shelf-list {
    margin-top: 8rpx;
}

.book-card {
    display: flex;
    gap: 24rpx;
    padding: 24rpx;
    margin-top: 20rpx;

    .bc-cover {
        position: relative;
        width: 140rpx;
        height: 192rpx;
        flex-shrink: 0;

        .pin-badge {
            position: absolute;
            top: -8rpx;
            left: -8rpx;
            width: 36rpx;
            height: 36rpx;
            border-radius: 8rpx;
            background: var(--cinnabar);
            color: #ffffff;
            font-size: 20rpx;
            display: flex;
            align-items: center;
            justify-content: center;
        }
    }
    .bc-info {
        flex: 1;
        min-width: 0;
        display: flex;
        flex-direction: column;

        .b-title {
            font-family: var(--font-serif);
            font-size: 30rpx;
            color: var(--ink);
            font-weight: 600;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }
        .b-author {
            margin-top: 4rpx;
            font-size: 24rpx;
            color: var(--ink-2);
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }
        .b-bar {
            margin-top: 14rpx;
            height: 8rpx;
            border-radius: 4rpx;
            background: var(--paper-deep);
            overflow: hidden;

            .b-fill {
                height: 100%;
                border-radius: 4rpx;
                background: var(--cinnabar);
            }
        }
        .b-page {
            margin-top: 8rpx;
            font-size: 22rpx;
            color: var(--ink-2);
        }
        .b-done {
            margin-top: 12rpx;
            font-size: 22rpx;
            color: var(--ink-3);
        }
        .b-btns {
            margin-top: auto;
            padding-top: 16rpx;
            display: flex;
            gap: 16rpx;
            justify-content: flex-end;
        }
    }
}

.loading-tip {
    padding: 40rpx 0;
    text-align: center;
    font-size: 24rpx;
    color: var(--ink-3);
}

/* 弹层 */
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
        color: var(--ink-2);
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

/* 按钮基线（03 §4）小号 */
.btn {
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 12rpx;

    &.primary {
        background: var(--indigo);
        color: #ffffff;

        &:active {
            background: var(--indigo-deep);
        }
    }
    &.ghost {
        border: 1px solid var(--indigo);
        color: var(--indigo);
        background: transparent;
    }
    &.fail-solid {
        background: var(--fail);
        color: #ffffff;
    }
    &.text {
        color: var(--ink-2);
    }
    &.sm {
        height: 60rpx;
        padding: 0 32rpx;
        font-size: 24rpx;
    }
    &.grow {
        flex: 1;
        height: 88rpx;
        font-size: 28rpx;
    }
}
</style>
