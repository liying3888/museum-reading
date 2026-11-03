<template>
    <view class="index-page">
        <!-- 页首区：衬线标题 + 日期（02 §1.1） -->
        <view class="page-head">
            <text class="head-title">荣河读书</text>
            <text class="head-date">{{ dateText }}</text>
        </view>

        <!-- 今日目标钤印卡：点卡身 → 统计页（02 §1.1②） -->
        <view class="goal-card card" @tap="goStats">
            <view class="card-hint">查看统计</view>

            <!-- 未登录：引导登录（02 §1.3），书单等公开区块照常 -->
            <block v-if="!isLogin">
                <view class="goal-login">
                    <text class="gl-text">登录后记录你的阅读</text>
                    <view class="btn primary sm" @tap.stop="goLogin">登录</view>
                </view>
            </block>

            <block v-else>
                <view class="goal-body">
                    <!-- 目标环：朱砂圆环进度 + 内嵌分钟数（完成态环满+盖印） -->
                    <view class="ring-box">
                        <view class="ring" :class="{ full: goalDone }">
                            <view class="ring-fill" :style="ringStyle" />
                            <view class="ring-inner">
                                <text class="r-min">{{ todayMinutes }}</text>
                                <text class="r-unit">分钟</text>
                                <text class="r-goal">目标 {{ goalMinutes }}</text>
                            </view>
                        </view>
                        <view v-if="goalDone" class="seal stamp">今日已阅</view>
                    </view>
                    <view class="goal-side">
                        <view class="side-item">
                            <text class="s-num">{{ statsFail ? '--' : consecutiveDays }}</text>
                            <text class="s-label">连续打卡 / 天</text>
                        </view>
                        <view class="side-item">
                            <text class="s-num">{{ statsFail ? '--' : weekDays }}</text>
                            <text class="s-label">本周阅读 / 天</text>
                        </view>
                    </view>
                </view>
                <!-- 失败降级：目标卡「--」+ 重试小按钮（02 §1.2） -->
                <view v-if="statsFail" class="goal-retry" @tap.stop="loadStats">重试</view>
            </block>
        </view>

        <!-- 继续阅读卡：仅在读 >0 时显示；多本时点卡身弹快选抽屉（02 §1.1③） -->
        <view v-if="reading.length > 0" class="continue-card card" @tap="onContinueCard">
            <view class="ct-cover">
                <book-cover :cover="latest.book?.cover || ''" :title="latest.book?.title || ''" :book-id="latest.book_id" />
            </view>
            <view class="ct-info">
                <text class="ct-title">{{ latest.book?.title }}</text>
                <text class="ct-page">第 {{ latest.current_page }} 页 / 共 {{ latest.book?.pages }} 页</text>
                <view class="ct-bar">
                    <view class="ct-fill" :style="{ width: latestPercent + '%' }" />
                </view>
                <view class="ct-foot">
                    <view class="btn primary sm" @tap.stop="goTimer(latest)">继续阅读</view>
                    <text class="ct-all" @tap.stop="goShelfReading">全部在读</text>
                </view>
            </view>
        </view>

        <!-- 本周书单区：横滚书卡（02 §1.1④，S1 假活动/笔记模块删除） -->
        <view class="week-zone">
            <view class="zone-head">
                <text class="zone-title">本周书单</text>
                <text class="zone-more" @tap="goLibrary">更多</text>
            </view>
            <scroll-view class="week-scroll" scroll-x :show-scrollbar="false">
                <view class="week-row">
                    <view v-for="b in weekBooks" :key="b.id" class="week-item" @tap="goDetail(b.id)">
                        <view class="w-cover">
                            <book-cover :cover="b.cover || ''" :title="b.title" :book-id="b.id" />
                        </view>
                        <text class="w-title">{{ b.title }}</text>
                        <text class="w-author">{{ b.author }}</text>
                    </view>
                    <view v-if="booksFail" class="week-fail" @tap="loadWeekBooks">加载失败，点击重试</view>
                </view>
            </scroll-view>
        </view>

        <tabbar />
    </view>
</template>

<script lang="ts" setup>
// 首页 —— S3 批 3 第三页（02 §1 + board01 + 06 §5）
// 主任务：回答「我今天读了吗」+ 最快继续阅读入口（修 S1 选书死锁/假数据模块/继续阅读指错书）
// 目标分钟单一数据源=后端用户配置（overview.goal，我的页可改——02 §0.1）
import { ref, computed } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import { bookList } from '@/api/book'
import { bookshelfList } from '@/api/bookshelf'
import { statsOverview } from '@/api/stats'
import { navigateTo } from '@/utils/util'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const dateText = ref('')
const todayMinutes = ref(0)
const goalMinutes = ref(30)
const consecutiveDays = ref(0)
const weekDays = ref(0)
const statsFail = ref(false)
const reading = ref<any[]>([])
const weekBooks = ref<any[]>([])
const booksFail = ref(false)

const isLogin = computed(() => userStore.isLogin)
const goalDone = computed(() => todayMinutes.value >= goalMinutes.value && goalMinutes.value > 0)
const latest = computed(() => reading.value[0] || null)
const latestPercent = computed(() => {
    if (!latest.value) return 0
    const pages = latest.value.book?.pages || 0
    if (!pages) return 0
    return Math.min(100, Math.round(((latest.value.current_page || 0) / pages) * 100))
})

// 目标环：conic-gradient 朱砂弧（满环走满色）
const ringStyle = computed(() => {
    const pct = goalMinutes.value > 0
        ? Math.min(100, Math.round((todayMinutes.value / goalMinutes.value) * 100))
        : 0
    return `background: conic-gradient(var(--cinnabar) ${pct * 3.6}deg, var(--paper-deep) 0deg);`
})

const WEEK_CN = ['日', '一', '二', '三', '四', '五', '六']

function initDate() {
    const d = new Date()
    dateText.value = `${d.getMonth() + 1}月${d.getDate()}日 星期${WEEK_CN[d.getDay()]}`
}

async function loadStats() {
    if (!userStore.isLogin) return
    try {
        const res: any = await statsOverview()
        todayMinutes.value = res.today?.duration_minutes ?? 0
        goalMinutes.value = res.goal ?? 30
        consecutiveDays.value = res.total?.consecutive_days ?? 0
        const daily: Record<string, number> = res.week?.daily_stats || {}
        weekDays.value = Object.values(daily).filter((v: any) => Number(v) > 0).length
        statsFail.value = false
    } catch (e) {
        statsFail.value = true // 目标卡降级「--」+重试（02 §1.2）
    }
}

async function loadReading() {
    if (!userStore.isLogin) {
        reading.value = []
        return
    }
    try {
        const res: any = await bookshelfList({ page_no: 1, page_size: 20, status: 1 })
        reading.value = res.lists || []
    } catch (e) {
        reading.value = [] // 失败不显示继续阅读卡（无旧数据可保）
    }
}

async function loadWeekBooks() {
    try {
        const res: any = await bookList({ page_no: 1, page_size: 6 })
        weekBooks.value = res.lists || []
        booksFail.value = false
    } catch (e) {
        booksFail.value = true
    }
}

function refreshAll() {
    loadStats()
    loadReading()
    loadWeekBooks()
}

function onContinueCard() {
    // 多本在读：点卡身弹「在读快选」抽屉（round1 修订：update_time 单一取本常指错书）
    if (reading.value.length > 1) {
        uni.showActionSheet({
            itemList: reading.value.slice(0, 6).map((r: any) => r.book?.title || '未命名'),
            success: (res) => {
                const picked = reading.value[res.tapIndex]
                if (picked) goTimer(picked)
            }
        })
        return
    }
    goTimer(latest.value)
}

function goTimer(book: any) {
    navigateTo({
        path: '/pages/reading/timer',
        query: { bookId: book.book_id, bookTitle: book.book?.title || '' }
    })
}

function goShelfReading() {
    uni.reLaunch({ url: '/pages/bookshelf/index?tab=1' })
}

function goStats() {
    navigateTo({ path: '/pages/stats/index' })
}

function goLibrary() {
    uni.reLaunch({ url: '/pages/library/index' })
}

function goDetail(id: number) {
    navigateTo({ path: '/pages/book/detail', query: { id } })
}

function goLogin() {
    navigateTo({ path: '/pages/login/login' })
}

initDate()

onShow(() => {
    refreshAll() // 计时页返回即见最新数字（02 §1.2）
})

onPullDownRefresh(() => {
    refreshAll()
    setTimeout(() => uni.stopPullDownRefresh(), 600)
})
</script>

<style lang="scss" scoped>
.index-page {
    min-height: 100vh;
    background: var(--paper);
    padding: 0 32rpx 220rpx;
}

.page-head {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    padding: calc(var(--status-bar-height, 0px) + 32rpx) 8rpx 8rpx;

    .head-title {
        font-family: var(--font-serif);
        font-size: 44rpx;
        color: var(--ink);
        font-weight: 600;
    }
    .head-date {
        font-size: 24rpx;
        color: var(--ink-2);
    }
}

.card {
    position: relative;
    background: var(--card);
    border: 1px solid var(--line);
    border-radius: 12rpx;
    box-shadow: var(--card-shadow);
    padding: 32rpx;
    margin-top: 24rpx;
}

/* 目标钤印卡 */
.goal-card {
    .card-hint {
        position: absolute;
        top: 20rpx;
        right: 24rpx;
        font-size: 22rpx;
        color: var(--ink-3);
    }
    .goal-login {
        padding: 40rpx 0;
        display: flex;
        align-items: center;
        justify-content: space-between;

        .gl-text {
            font-size: 28rpx;
            color: var(--ink-2);
        }
    }
    .goal-body {
        display: flex;
        align-items: center;
        gap: 48rpx;
    }
    .goal-retry {
        margin-top: 16rpx;
        align-self: flex-start;
        font-size: 24rpx;
        color: var(--indigo);
    }
}

/* 目标环：外环 conic 朱砂弧，内圈 paper 白芯 */
.ring-box {
    position: relative;
    width: 200rpx;
    height: 200rpx;
    flex-shrink: 0;
}

.ring {
    width: 200rpx;
    height: 200rpx;
    border-radius: 50%;

    .ring-fill {
        width: 100%;
        height: 100%;
        border-radius: 50%;
        transition: background 0.4s ease;
    }
    &.full .ring-fill {
        background: var(--cinnabar); // 完成态满环
    }
    .ring-inner {
        position: absolute;
        inset: 18rpx;
        border-radius: 50%;
        background: var(--card);
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
    }
}

.r-min {
    font-family: var(--font-serif);
    font-size: 56rpx;
    line-height: 1.1;
    color: var(--ink);
    font-weight: 600;
}
.r-unit {
    font-size: 22rpx;
    color: var(--ink-2);
}
.r-goal {
    margin-top: 6rpx;
    font-size: 22rpx;
    color: var(--ink-3);
}

/* 「今日已阅」朱砂圆印（0.4s 盖印动效） */
.seal {
    position: absolute;
    right: -26rpx;
    top: -18rpx;
    width: 92rpx;
    height: 92rpx;
    border-radius: 50%;
    border: 3rpx solid var(--cinnabar);
    color: var(--cinnabar);
    display: flex;
    align-items: center;
    justify-content: center;
    font-family: var(--font-serif);
    font-size: 22rpx;
    transform: rotate(12deg);
    background: rgba(181, 58, 46, 0.06);
}
.stamp {
    animation: stamp-in 0.4s ease-out;
}
@keyframes stamp-in {
    0% {
        opacity: 0;
        transform: rotate(12deg) scale(1.8);
    }
    100% {
        opacity: 1;
        transform: rotate(12deg) scale(1);
    }
}

.goal-side {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 32rpx;

    .side-item {
        display: flex;
        align-items: baseline;
        gap: 16rpx;
    }
    .s-num {
        font-family: var(--font-serif);
        font-size: 48rpx;
        color: var(--ink);
        font-weight: 600;
    }
    .s-label {
        font-size: 24rpx;
        color: var(--ink-2);
    }
}

/* 继续阅读卡 */
.continue-card {
    display: flex;
    gap: 32rpx;

    .ct-cover {
        width: 120rpx;
        height: 168rpx;
        flex-shrink: 0;
    }
    .ct-info {
        flex: 1;
        min-width: 0;
        display: flex;
        flex-direction: column;

        .ct-title {
            font-family: var(--font-serif);
            font-size: 32rpx;
            color: var(--ink);
            font-weight: 600;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }
        .ct-page {
            margin-top: 8rpx;
            font-size: 24rpx;
            color: var(--ink-2);
        }
        .ct-bar {
            margin-top: 16rpx;
            height: 8rpx;
            border-radius: 4rpx;
            background: var(--paper-deep);
            overflow: hidden;

            .ct-fill {
                height: 100%;
                border-radius: 4rpx;
                background: var(--cinnabar);
            }
        }
        .ct-foot {
            margin-top: 20rpx;
            display: flex;
            align-items: center;
            justify-content: space-between;

            .ct-all {
                font-size: 24rpx;
                color: var(--indigo);
            }
        }
    }
}

/* 本周书单区 */
.week-zone {
    margin-top: 40rpx;

    .zone-head {
        display: flex;
        align-items: baseline;
        justify-content: space-between;
        padding: 0 8rpx;

        .zone-title {
            font-family: var(--font-serif);
            font-size: 34rpx;
            color: var(--ink);
            font-weight: 600;
        }
        .zone-more {
            font-size: 24rpx;
            color: var(--ink-2);
        }
    }
    .week-scroll {
        margin-top: 24rpx;
        width: 100%;
    }
    .week-row {
        display: flex;
        gap: 24rpx;
        padding: 4rpx 4rpx 16rpx;
    }
    .week-item {
        width: 160rpx;
        flex-shrink: 0;
        display: flex;
        flex-direction: column;

        .w-cover {
            width: 160rpx;
            height: 220rpx;
        }
        .w-title {
            margin-top: 12rpx;
            font-size: 26rpx;
            color: var(--ink);
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }
        .w-author {
            margin-top: 4rpx;
            font-size: 22rpx;
            color: var(--ink-2);
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }
    }
    .week-fail {
        flex: 1;
        padding: 40rpx 0;
        text-align: center;
        font-size: 24rpx;
        color: var(--ink-2);
    }
}

/* 按钮基线（03 §4）：主=黛蓝实底白字；小号 64rpx */
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
    &.sm {
        height: 64rpx;
        padding: 0 40rpx;
        font-size: 26rpx;
    }
}
</style>
