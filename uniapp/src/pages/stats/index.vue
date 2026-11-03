<template>
    <view class="stats-page">
        <nav-bar title="阅读统计" />

        <!-- 未登录整页引导（02 §6.2） -->
        <view v-if="!isLogin" class="login-block">
            <empty-state mode="page" icon="seal" text="登录后回望你的阅读" action-text="去登录" @action="goLogin" />
        </view>

        <block v-else>
            <!-- 三格统计条（02 §6.1②） -->
            <view class="grid3 card">
                <view class="g-item">
                    <text class="g-num">{{ todayMin }}</text>
                    <text class="g-label">今日 / 分钟</text>
                </view>
                <view class="g-item">
                    <text class="g-num">{{ weekMin }}</text>
                    <text class="g-label">本周 / 分钟</text>
                </view>
                <view class="g-item">
                    <text class="g-num">{{ totalHuman }}</text>
                    <text class="g-label">累计</text>
                </view>
            </view>

            <!-- 本周柱状图（02 §6.1③：统一分钟/当日朱砂/未来日「—」） -->
            <view class="card chart-card">
                <text class="sec-title">本周阅读</text>
                <view class="chart">
                    <view v-for="(d, i) in weekBars" :key="i" class="bar-col">
                        <text class="bar-val" :class="{ dim: d.future }">{{ d.label }}</text>
                        <view class="bar-track">
                            <view
                                v-if="!d.future"
                                class="bar-fill"
                                :class="{ today: d.isToday }"
                                :style="{ height: d.height + '%' }"
                            />
                        </view>
                        <text class="bar-day" :class="{ today: d.isToday }">{{ d.name }}</text>
                    </view>
                </view>
                <view v-if="weekMax === 0" class="chart-empty">本周暂无记录</view>
            </view>

            <!-- 打卡月历（02 §6.1④：钤印式） -->
            <view class="card calendar-card">
                <view class="cal-head">
                    <view class="cal-arrow" @tap="shiftMonth(-1)">
                        <view class="a-left" :class="{ off: !canPrev }" />
                    </view>
                    <text class="cal-title">{{ monthTitle }}</text>
                    <view class="cal-arrow" @tap="shiftMonth(1)">
                        <view class="a-right" :class="{ off: !canNext }" />
                    </view>
                </view>
                <view class="cal-week">
                    <text v-for="w in ['一','二','三','四','五','六','日']" :key="w" class="cw">周{{ w }}</text>
                </view>
                <view class="cal-grid">
                    <view v-for="(cell, i) in monthCells" :key="i" class="cal-cell">
                        <view v-if="cell && cell.read" class="stamp-cell">{{ cell.day }}</view>
                        <text v-else-if="cell" class="cal-day" :class="{ future: cell.future }">{{ cell.day }}</text>
                    </view>
                </view>
                <text class="cal-note">已读日盖朱砂印 · 连续打卡 {{ consecutive }} 天</text>
            </view>

            <!-- 明细行（02 §6.1⑤） -->
            <view class="card detail-card">
                <view class="d-row"><text>阅读天数</text><text class="d-num">{{ total.read_days ?? '--' }}</text></view>
                <view class="d-row"><text>在读书籍</text><text class="d-num">{{ total.reading_books ?? '--' }}</text></view>
                <view class="d-row"><text>读完书籍</text><text class="d-num">{{ total.finished_books ?? '--' }}</text></view>
                <view class="d-row"><text>书架总数</text><text class="d-num">{{ total.total_books ?? '--' }}</text></view>
            </view>
        </block>
    </view>
</template>

<script lang="ts" setup>
// 阅读统计页 —— S3 批 3 第六页（02 §6 + board06）
// 不占 Tab 位；入口=首页目标卡点击+我的菜单。修 S1：无返回键困死/UTC 串时区/px 混用
import { ref, computed } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import { statsOverview, statsCheckin } from '@/api/stats'
import { navigateTo } from '@/utils/util'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const isLogin = computed(() => userStore.isLogin)

const overview = ref<any>({})
const today = computed(() => overview.value.today || {})
const week = computed(() => overview.value.week || {})
const total = computed(() => overview.value.total || {})
const goal = computed(() => overview.value.goal ?? 30)
const consecutive = computed(() => total.value.consecutive_days ?? 0)

const todayMin = computed(() => today.value.duration_minutes ?? '--')
const weekMin = computed(() => week.value.duration_minutes ?? '--')
const totalHuman = computed(() => {
    const sec = total.value.duration_seconds
    if (sec == null) return '--'
    const h = Math.floor(sec / 3600)
    const m = Math.floor((sec % 3600) / 60)
    return h > 0 ? `${h}时${m}分` : `${m}分`
})

// —— 本周柱图（统一分钟，02 §6.1③）——
const weekBars = computed(() => {
    const daily: Record<string, number> = week.value.daily_stats || {}
    const names = ['一', '二', '三', '四', '五', '六', '日']
    // week.start_date=周一（后端契约）；按周一起排 7 天
    const start = week.value.start_date ? new Date(week.value.start_date + 'T00:00:00') : null
    const now = new Date()
    const bars: any[] = []
    const max = Math.max(0, ...Object.values(daily).map(Number))
    for (let i = 0; i < 7; i++) {
        const d = start ? new Date(start.getTime() + i * 86400000) : null
        const key = d ? `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}` : ''
        const minutes = Math.round((Number(daily[key] || 0)) / 60)
        const isToday = d ? d.toDateString() === now.toDateString() : false
        const future = d ? d.getTime() > new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime() : false
        bars.push({
            name: names[i],
            minutes,
            label: future ? '—' : (minutes > 0 ? String(minutes) : ''),
            height: max > 0 ? Math.max(6, Math.round((minutes / max) * 100)) : 0,
            isToday,
            future
        })
    }
    return bars
})

const weekMax = computed(() => Math.max(0, ...weekBars.value.map((b: any) => b.minutes)))

// —— 打卡月历 ——
const calYear = ref(new Date().getFullYear())
const calMonth = ref(new Date().getMonth() + 1) // 1-12
const readDates = ref<Set<string>>(new Set())

const monthTitle = computed(() => `${calYear.value} 年 ${calMonth.value} 月 已读 ${readDays.value} 天`)
const readDays = computed(() => {
    const prefix = `${calYear.value}-${String(calMonth.value).padStart(2, '0')}`
    return [...readDates.value].filter((d) => d.startsWith(prefix)).length
})

// 月历网格：周一开头，前置空位补 null
const monthCells = computed(() => {
    const first = new Date(calYear.value, calMonth.value - 1, 1)
    const daysInMonth = new Date(calYear.value, calMonth.value, 0).getDate()
    const lead = (first.getDay() + 6) % 7 // 周一=0
    const prefix = `${calYear.value}-${String(calMonth.value).padStart(2, '0')}`
    const now = new Date()
    const cells: any[] = []
    for (let i = 0; i < lead; i++) cells.push(null)
    for (let d = 1; d <= daysInMonth; d++) {
        const key = `${prefix}-${String(d).padStart(2, '0')}`
        const cellDate = new Date(calYear.value, calMonth.value - 1, d)
        cells.push({
            day: d,
            read: readDates.value.has(key),
            future: cellDate.getTime() > new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
        })
    }
    return cells
})

const canPrev = computed(() => {
    // 范围：注册月~当月（首期以 2026-10 库内有数据月起，保守到 2026-01）
    return calYear.value > 2026 || (calYear.value === 2026 && calMonth.value > 1)
})
const canNext = computed(() => {
    const now = new Date()
    return calYear.value < now.getFullYear() || (calYear.value === now.getFullYear() && calMonth.value < now.getMonth() + 1)
})

function shiftMonth(delta: number) {
    let m = calMonth.value + delta
    let y = calYear.value
    if (m < 1) { m = 12; y-- }
    if (m > 12) { m = 1; y++ }
    calMonth.value = m
    calYear.value = y
    loadCheckin()
}

async function loadCheckin() {
    try {
        const res: any = await statsCheckin(`${calYear.value}-${String(calMonth.value).padStart(2, '0')}`)
        readDates.value = new Set(res.dates || [])
    } catch (e) {
        readDates.value = new Set()
    }
}

async function loadAll() {
    if (!userStore.isLogin) return
    try {
        overview.value = await statsOverview()
    } catch (e) {
        overview.value = {}
    }
    loadCheckin()
}

function goLogin() {
    navigateTo({ path: '/pages/login/login' })
}

onShow(() => {
    loadAll()
})

onPullDownRefresh(() => {
    loadAll()
    setTimeout(() => uni.stopPullDownRefresh(), 600)
})
</script>

<style lang="scss" scoped>
.stats-page {
    min-height: 100vh;
    background: var(--paper);
    padding: 16rpx 32rpx 64rpx;
}

.login-block {
    padding-top: 80rpx;
}

.card {
    background: var(--card);
    border: 1px solid var(--line);
    border-radius: 12rpx;
    box-shadow: var(--card-shadow);
    padding: 32rpx;
    margin-top: 24rpx;
}

.sec-title {
    font-family: var(--font-serif);
    font-size: 30rpx;
    color: var(--ink);
    font-weight: 600;
}

/* 三格统计条 */
.grid3 {
    display: flex;

    .g-item {
        flex: 1;
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 8rpx;

        .g-num {
            font-family: var(--font-serif);
            font-size: 44rpx;
            color: var(--ink);
            font-weight: 600;
        }
        .g-label {
            font-size: 22rpx;
            color: var(--ink-3);
        }
    }
}

/* 本周柱图 */
.chart-card {
    .chart {
        margin-top: 24rpx;
        display: flex;
        align-items: flex-end;
        gap: 12rpx;
        height: 280rpx;
    }
    .bar-col {
        flex: 1;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: flex-end;
        height: 100%;

        .bar-val {
            font-size: 20rpx;
            color: var(--ink-2);
            height: 30rpx;

            &.dim {
                color: var(--ink-2);
            }
        }
        .bar-track {
            width: 36rpx;
            flex: 1;
            display: flex;
            align-items: flex-end;
            border-radius: 8rpx 8rpx 0 0;
            background: var(--paper-deep);
            overflow: hidden;

            .bar-fill {
                width: 100%;
                border-radius: 8rpx 8rpx 0 0;
                background: var(--indigo); // 黛蓝柱

                &.today {
                    background: var(--cinnabar); // 当日朱砂高亮
                }
            }
        }
        .bar-day {
            margin-top: 12rpx;
            font-size: 22rpx;
            color: var(--ink-2);

            &.today {
                color: var(--cinnabar);
                font-weight: 600;
            }
        }
    }
    .chart-empty {
        margin-top: 16rpx;
        text-align: center;
        font-size: 24rpx;
        color: var(--ink-3);
    }
}

/* 打卡月历 */
.calendar-card {
    .cal-head {
        display: flex;
        align-items: center;
        justify-content: space-between;

        .cal-title {
            font-family: var(--font-serif);
            font-size: 30rpx;
            color: var(--ink);
            font-weight: 600;
        }
        .cal-arrow {
            width: 72rpx;
            height: 72rpx;
            display: flex;
            align-items: center;
            justify-content: center;

            .a-left,
            .a-right {
                width: 18rpx;
                height: 18rpx;
                border-left: 3rpx solid var(--ink-2);
                border-bottom: 3rpx solid var(--ink-2);
            }
            .a-left {
                transform: rotate(45deg);
            }
            .a-right {
                transform: rotate(-135deg);
            }
            .off {
                opacity: 0.25;
            }
        }
    }
    .cal-week {
        margin-top: 24rpx;
        display: flex;

        .cw {
            flex: 1;
            text-align: center;
            font-size: 22rpx;
            color: var(--ink-3);
        }
    }
    .cal-grid {
        margin-top: 12rpx;
        display: flex;
        flex-wrap: wrap;

        .cal-cell {
            width: 14.28%;
            height: 76rpx;
            display: flex;
            align-items: center;
            justify-content: center;

            .cal-day {
                font-size: 26rpx;
                color: var(--ink-2);

                &.future {
                    color: var(--ink-3);
                    opacity: 0.5;
                }
            }
        }
    }
    .stamp-cell {
        width: 52rpx;
        height: 52rpx;
        border-radius: 8rpx;
        background: var(--cinnabar);
        color: #ffffff;
        font-size: 24rpx;
        display: flex;
        align-items: center;
        justify-content: center;
        font-family: var(--font-serif);
    }
    .cal-note {
        display: block;
        margin-top: 20rpx;
        font-size: 22rpx;
        color: var(--ink-3);
        text-align: center;
    }
}

/* 明细行 */
.detail-card {
    .d-row {
        display: flex;
        justify-content: space-between;
        align-items: baseline;
        padding: 18rpx 8rpx;
        font-size: 26rpx;
        color: var(--ink-2);

        & + .d-row {
            border-top: 1rpx solid var(--line);
        }
        .d-num {
            font-family: var(--font-serif);
            font-size: 30rpx;
            color: var(--ink);
        }
    }
}
</style>
