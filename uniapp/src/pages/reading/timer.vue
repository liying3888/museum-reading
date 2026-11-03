<template>
    <view class="timer-page" :class="{ night: phase !== 'ready' }">
        <!-- ready 态：开始前 -->
        <block v-if="phase === 'ready'">
            <view class="t-state ghost">准备开始</view>
            <text class="t-book">{{ bookTitle || '自由阅读' }}</text>
            <view class="t-clock ghost-num">{{ display }}</view>
            <view class="t-hint">开始后计时 暂停即同步暂停</view>
            <view class="btns">
                <view class="btn warm primary-big" @tap="onStart">开始阅读</view>
            </view>
        </block>

        <!-- 异书重入提示（round4 口径：A 会话态分支文案） -->
        <block v-else-if="phase === 'conflict'">
            <view class="t-state">{{ conflictSession.paused_at > 0 ? '已暂停' : '计时中' }}</view>
            <text class="t-book">《{{ conflictSession.book_title || '自由阅读' }}》</text>
            <view class="t-clock">{{ fmt(conflictNet) }}</view>
            <view class="conflict-tip">
                {{ conflictSession.paused_at > 0
                    ? `《${conflictSession.book_title}》有未结束的已暂停计时`
                    : `《${conflictSession.book_title}》计时仍在进行` }}
            </view>
            <view class="btns">
                <view class="btn outline" @tap="onBackToA">回到《{{ conflictSession.book_title }}》</view>
                <view class="btn warm" @tap="onAskFinishA">结束并开新书</view>
            </view>
        </block>

        <!-- running / paused 态 -->
        <block v-else>
            <view class="t-state">{{ phase === 'paused' ? '已暂停' : '计时中' }}</view>
            <text class="t-book">{{ bookTitle || '自由阅读' }}</text>
            <view class="t-clock">{{ display }}</view>
            <text v-if="bookId" class="t-pages">已读 {{ currentPage }} 页</text>
            <view v-if="phase === 'paused'" class="t-hint">已暂停 计时已同步暂停</view>
            <view v-else class="t-hint">计时中 可切后台不打断</view>
            <view class="btns">
                <view v-if="phase === 'running'" class="btn warm primary-big" @tap="onPause">暂停</view>
                <view v-else class="btn warm primary-big" @tap="onResume">继续</view>
                <view class="btn outline" @tap="askEnd">结束</view>
            </view>
        </block>

        <!-- 120 分钟提醒（02 §0.3 兜底①：单次达 2 小时询问是否继续） -->
        <view v-if="showRemind" class="mask" />
        <view v-if="showRemind" class="popup center-popup">
            <text class="pp-title">已读 2 小时，要继续吗</text>
            <text class="pp-desc">长时间连续阅读建议稍作休息。</text>
            <view class="pp-btns">
                <view class="btn ghost grow" @tap="onRemindEnd">结束</view>
                <view class="btn primary grow" @tap="showRemind = false">继续</view>
            </view>
        </view>

        <!-- 结束确认弹层（防误触，02 §5.1；A 会话链式收尾场景文案区分） -->
        <view v-if="showEndAsk" class="mask" @tap="showEndAsk = false" />
        <view v-if="showEndAsk" class="popup center-popup">
            <text class="pp-title">{{ endTarget === 'A' ? `结束《${conflictSession?.book_title}》的计时？` : '结束本次阅读？' }}</text>
            <text class="pp-desc">{{ endTarget === 'A' ? '该计时时长将记入阅读记录，随后开始你选的新书。' : '结束后本次时长将记入今日统计，可随时再开始。' }}</text>
            <view class="pp-btns">
                <view class="btn ghost grow" @tap="showEndAsk = false">继续读</view>
                <view class="btn primary grow" @tap="onEnd">确认结束</view>
            </view>
        </view>

        <!-- 成果弹层（02 §5.3：夜底纸笺卡） -->
        <view v-if="result" class="mask result-mask" />
        <view v-if="result" class="popup result-card">
            <view v-if="result.goal_achieved" class="r-seal stamp">今日目标<br />达成</view>
            <text class="r-title">本次阅读</text>
            <view class="r-duration">{{ fmtHuman(result.duration) }}</view>
            <view v-if="result.auto_closed" class="r-note">长时间未操作已自动结束</view>
            <view class="r-lines">
                <view class="r-line"><text>今日累计</text><text>{{ fmtHuman(result.today_total) }}</text></view>
                <view class="r-line"><text>连续打卡</text><text>{{ result.streak_days }} 天</text></view>
                <view class="r-line">
                    <text>{{ result.goal_achieved ? '超出目标' : '距今日目标' }}</text>
                    <text>{{ fmtHuman(result.goal_achieved
                        ? Math.max(0, result.today_total - result.goal * 60)
                        : result.goal_gap) }}</text>
                </view>
            </view>
            <view class="btn warm grow" @tap="leaveBack">好的</view>
            <text class="r-auto">{{ autoLeft }}s 后自动返回</text>
        </view>
    </view>
</template>

<script lang="ts" setup>
// 阅读计时页 —— S3 批 3 第五页（02 §5 + board10 三态）
// 全屏夜读沉浸（night 底+暖白字）；真暂停口径（02 §0.3）：显示时间=服务端 net_duration，
// 本地 setInterval 仅秒级跳动渲染；onShow 重算修 S1 切后台漂移
// 异书重入（02 §5.4 round4）：不静默进 B 书计时、不自动结束 A；提供「结束并开新书」出口
import { ref, computed, onUnmounted } from 'vue'
import { onLoad, onShow, onUnload } from '@dcloudio/uni-app'
import {
    readingStart,
    readingPause,
    readingResume,
    readingEnd,
    readingSession
} from '@/api/reading'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const phase = ref<'ready' | 'running' | 'paused' | 'conflict' | 'result'>('ready')
const bookId = ref(0)
const bookTitle = ref('')
const currentPage = ref(0)
const recordId = ref(0)
const serverNet = ref(0) // 服务端权威净时长（秒）
const syncAt = ref(0) // 上次同步的本地时刻（running 中本地加秒）
const tick = ref(0) // 触发重渲
const showEndAsk = ref(false)
const showRemind = ref(false)
const result = ref<any>(null)
const autoLeft = ref(8)
const conflictSession = ref<any>(null)

let timer: ReturnType<typeof setInterval> | null = null
let autoTimer: ReturnType<typeof setInterval> | null = null
let toastOnLeave = false // 离开时 running/paused 的保持提示

// 显示时长：running=serverNet+本地流逝；paused=serverNet 冻结
const display = computed(() => {
    void tick.value
    let net = serverNet.value
    if (phase.value === 'running' && syncAt.value) {
        net += Math.floor((Date.now() - syncAt.value) / 1000)
    }
    return fmt(net)
})

const conflictNet = computed(() => conflictSession.value?.net_duration ?? 0)

function fmt(sec: number): string {
    const h = Math.floor(sec / 3600)
    const m = Math.floor((sec % 3600) / 60)
    const s = sec % 60
    const p = (n: number) => String(n).padStart(2, '0')
    return `${p(h)}:${p(m)}:${p(s)}`
}

function fmtHuman(sec: number): string {
    const h = Math.floor(sec / 3600)
    const m = Math.floor((sec % 3600) / 60)
    if (h > 0) return `${h} 小时 ${m} 分`
    return `${m} 分`
}

function syncFromServer(session: any) {
    recordId.value = session.record_id
    serverNet.value = session.net_duration ?? 0
    syncAt.value = Date.now()
    phase.value = session.paused_at > 0 ? 'paused' : 'running'
}

function startTicker() {
    stopTicker()
    timer = setInterval(() => {
        tick.value++ // 仅触发重渲（02 §0.3：本地跳动不作为时长依据）
        // 120 分钟提醒（02 §0.3 兜底①；240 自动结束后端权威，双通道不依赖前端在场）
        if (phase.value === 'running' && !showRemind.value && displaySeconds() >= 7200) {
            showRemind.value = true
        }
    }, 1000)
}

function stopTicker() {
    if (timer) clearInterval(timer)
    timer = null
}

async function resync() {
    // onShow 重算（修 S1 漂移）：拉 session 权威 net_duration
    try {
        const s: any = await readingSession()
        if (s && s.record_id) {
            if (phase.value === 'running' || phase.value === 'paused') {
                serverNet.value = s.net_duration ?? 0
                syncAt.value = Date.now()
            }
        }
    } catch (e) {
        /* 静默重试下次 onShow */
    }
}

// —— 动作 ——

async function onStart() {
    try {
        const s: any = await readingStart(bookId.value || undefined)
        // 幂等返回现存会话：异书（A≠B）走 conflict 分支（02 §5.4）
        if (s.resumed && s.book_id && bookId.value && s.book_id !== bookId.value) {
            conflictSession.value = s
            phase.value = 'conflict'
            return
        }
        if (s.book_id) bookId.value = s.book_id
        bookTitle.value = s.book_title || bookTitle.value
        syncFromServer(s)
        startTicker()
    } catch (e) {
        /* start 失败停留 ready+Toast（拦截器已发）——修 S1「点了没反应」 */
    }
}

function onBackToA() {
    const s = conflictSession.value
    bookId.value = s.book_id
    bookTitle.value = s.book_title
    conflictSession.value = null
    syncFromServer(s)
    startTicker()
}

function onAskFinishA() {
    // 「结束并开新书」：先走 A 结束确认，确认后链式收尾跳过 A 成果弹层（round4）
    endTarget.value = 'A'
    showEndAsk.value = true
}

const endTarget = ref<'self' | 'A'>('self')

async function onEnd() {
    showEndAsk.value = false
    const targetA = endTarget.value === 'A'
    const rid = targetA ? conflictSession.value?.record_id : recordId.value
    try {
        const r: any = await readingEnd(rid)
        stopTicker()
        if (targetA) {
            // 链式收尾：跳过 A 成果（round4），原地以 B 书发起 start
            conflictSession.value = null
            endTarget.value = 'self'
            phase.value = 'ready'
            await onStart()
            return
        }
        result.value = r
        phase.value = 'result'
        autoLeft.value = 8
        autoTimer = setInterval(() => {
            autoLeft.value--
            if (autoLeft.value <= 0) leaveBack()
        }, 1000)
    } catch (e) {
        // 结束失败停留当前态+重试提示（02 §5.4），记录不丢
        uni.showToast({ title: '结束失败，请重试', icon: 'none' })
        phase.value = serverNet.value >= 0 && syncAt.value ? 'running' : 'ready'
    }
}

async function onPause() {
    try {
        await readingPause(recordId.value)
        // 冻结：以服务端暂停口径为准（暂停后不再累计）
        serverNet.value = displaySeconds()
        syncAt.value = 0
        phase.value = 'paused'
    } catch (e) {
        /* 拦截器已 toast */
    }
}

async function onResume() {
    try {
        await readingResume(recordId.value)
        syncAt.value = Date.now()
        phase.value = 'running'
    } catch (e) {
        /* 拦截器已 toast */
    }
}

function displaySeconds(): number {
    const parts = display.value.split(':').map(Number)
    return parts[0] * 3600 + parts[1] * 60 + parts[2]
}

function leaveBack() {
    if (autoTimer) clearInterval(autoTimer)
    uni.navigateBack({
        fail: () => uni.reLaunch({ url: '/pages/index/index' })
    })
}

function askEnd() {
    endTarget.value = 'self'
    showEndAsk.value = true
}

function onRemindEnd() {
    showRemind.value = false
    onEnd()
}

onLoad((options: any) => {
    bookId.value = Number(options?.bookId || 0)
    bookTitle.value = decodeURIComponent(options?.bookTitle || '')
    // 「已读 n 页」数据源：书架行 current_page（带 bookId 时）
    if (bookId.value) {
        import('@/api/bookshelf')
            .then(({ bookshelfList }) => bookshelfList({ page_no: 1, page_size: 200 }))
            .then((res: any) => {
                const row = (res.lists || []).find((x: any) => x.book_id === bookId.value)
                if (row) currentPage.value = row.current_page || 0
            })
            .catch(() => {})
    }
    // 进入即查悬置会话：有则恢复当前态（02 §5.4 悬置重入）
    readingSession()
        .then((s: any) => {
            if (s && s.record_id) {
                if (bookId.value && s.book_id && s.book_id !== bookId.value) {
                    conflictSession.value = s
                    phase.value = 'conflict'
                    return
                }
                bookId.value = s.book_id || bookId.value
                bookTitle.value = s.book_title || bookTitle.value
                syncFromServer(s)
                startTicker()
            }
        })
        .catch(() => {})
})

onShow(() => {
    if (phase.value === 'running') resync()
})

onUnload(() => {
    // running/paused 离开：不弹确认，后台保持当前态（02 §5.1）
    if (phase.value === 'running' || phase.value === 'paused') {
        toastOnLeave = true
    }
    stopTicker()
    if (autoTimer) clearInterval(autoTimer)
})

onUnmounted(() => {
    if (toastOnLeave) {
        uni.showToast({ title: '计时仍在进行', icon: 'none' })
    }
})
</script>

<style lang="scss" scoped>
/* 夜读沉浸（03 §5）：night 底 + 暖白字；ready 态仍夜底（进入即沉浸） */
.timer-page {
    min-height: 100vh;
    background: var(--night);
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 0 48rpx calc(48rpx + env(safe-area-inset-bottom));
    position: relative;
}

.t-state {
    font-size: 26rpx;
    color: var(--night-ink);
    opacity: 0.55;
    letter-spacing: 4rpx;

    &.ghost {
        opacity: 0.4;
    }
}

.t-book {
    margin-top: 16rpx;
    font-size: 28rpx;
    color: var(--night-ink);
    opacity: 0.65;
    text-align: center;
}

.t-clock {
    margin-top: 48rpx;
    font-family: var(--font-serif);
    font-size: 120rpx;
    line-height: 1.15;
    color: var(--night-ink);
    font-variant-numeric: tabular-nums;

    &.ghost-num {
        opacity: 0.35; // ready 态 00:00:00 弱显
    }
}

.t-pages {
    margin-top: 16rpx;
    font-size: 24rpx;
    color: var(--night-ink);
    opacity: 0.55;
}

.t-hint {
    margin-top: 24rpx;
    font-size: 24rpx;
    color: var(--night-ink);
    opacity: 0.45;
}

.conflict-tip {
    margin-top: 24rpx;
    font-size: 26rpx;
    line-height: 1.7;
    color: var(--night-ink);
    opacity: 0.75;
    text-align: center;
}

.btns {
    margin-top: 96rpx;
    display: flex;
    flex-direction: column;
    gap: 24rpx;
    width: 100%;
}

/* round1 主次：暂停/继续=暖白实底主；结束=反白描边次（终态有确认兜底） */
.btn {
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 12rpx;
    font-size: 30rpx;

    &.warm {
        height: 96rpx;
        background: var(--night-ink);
        color: var(--night);

        &:active {
            opacity: 0.85;
        }
    }
    &.primary-big {
        height: 96rpx;
    }
    &.outline {
        height: 88rpx;
        border: 1rpx solid rgba(244, 239, 230, 0.55);
        color: var(--night-ink);
        background: transparent;
    }
    &.ghost {
        border: 1rpx solid var(--indigo);
        color: var(--indigo);
        background: transparent;
    }
    &.primary {
        background: var(--indigo);
        color: #ffffff;
    }
    &.grow {
        flex: 1;
    }
}

/* 弹层（夜色页上：纸笺卡） */
.mask {
    position: fixed;
    inset: 0;
    z-index: 40;
    background: rgba(0, 0, 0, 0.55);
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

/* 成果卡（02 §5.3） */
.result-card {
    left: 64rpx;
    right: 64rpx;
    top: 50%;
    transform: translateY(-50%);
    text-align: center;

    .r-title {
        display: block;
        font-family: var(--font-serif);
        font-size: 30rpx;
        color: var(--ink);
        font-weight: 600;
    }
    .r-duration {
        margin-top: 16rpx;
        font-family: var(--font-serif);
        font-size: 72rpx;
        color: var(--ink);
        font-weight: 600;
    }
    .r-note {
        margin-top: 8rpx;
        font-size: 24rpx;
        color: var(--warn);
    }
    .r-lines {
        margin-top: 32rpx;
        border-top: 1rpx solid var(--line);
        padding-top: 8rpx;

        .r-line {
            display: flex;
            justify-content: space-between;
            padding: 16rpx 8rpx;
            font-size: 26rpx;
            color: var(--ink-2);

            text:last-child {
                color: var(--ink);
                font-family: var(--font-serif);
            }
        }
    }
    .btn.warm {
        margin-top: 40rpx;
        background: var(--indigo);
        color: #fff;
        height: 88rpx;
    }
    .r-auto {
        display: block;
        margin-top: 20rpx;
        font-size: 22rpx;
        color: var(--ink-3);
    }
    .r-seal {
        position: absolute;
        top: -30rpx;
        right: -14rpx;
        width: 120rpx;
        height: 120rpx;
        border-radius: 50%;
        border: 3rpx solid var(--cinnabar);
        color: var(--cinnabar);
        display: flex;
        align-items: center;
        justify-content: center;
        text-align: center;
        font-family: var(--font-serif);
        font-size: 24rpx;
        line-height: 1.4;
        transform: rotate(12deg);
        background: rgba(181, 58, 46, 0.06);
    }
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
</style>
