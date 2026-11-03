<template>
    <view class="center-page">
        <view class="hero">
            <!-- 用户卡（02 §7.1） -->
            <view class="user-card" @tap="!isLogin && goLogin()">
                <template v-if="isLogin">
                    <image class="avatar" :src="userInfo.avatar" mode="aspectFill" />
                    <view class="u-main">
                        <text class="nickname">{{ userInfo.nickname || '书友' }}</text>
                        <text class="sn">读者号 {{ userInfo.sn }}</text>
                    </view>
                    <view class="edit-btn" @tap.stop="goEdit">编辑资料</view>
                </template>
                <template v-else>
                    <view class="avatar guest">客</view>
                    <view class="u-main">
                        <text class="nickname">未登录</text>
                        <text class="sn">登录后记录你的阅读</text>
                    </view>
                    <view class="edit-btn">去登录</view>
                </template>
            </view>

            <!-- 三格摘要：整卡跳统计（02 §7.2） -->
            <view v-if="isLogin" class="sum-card" @tap="goStats">
                <view class="s-item">
                    <text class="s-num">{{ totalHuman }}</text>
                    <text class="s-label">累计时长</text>
                </view>
                <view class="s-divider" />
                <view class="s-item">
                    <text class="s-num">{{ total.finished_books ?? '--' }}</text>
                    <text class="s-label">已读书籍</text>
                </view>
                <view class="s-divider" />
                <view class="s-item">
                    <text class="s-num">{{ total.consecutive_days ?? '--' }}</text>
                    <text class="s-label">连续打卡</text>
                </view>
            </view>
        </view>

        <block v-if="isLogin">
            <!-- 今日目标行（02 §7.3：迷你环+改目标弹层） -->
            <view class="card goal-card">
                <view class="g-ring" :style="ringStyle">
                    <view class="g-ring-inner">
                        <text class="g-pct">{{ goalPct }}%</text>
                    </view>
                </view>
                <view class="g-body">
                    <text class="g-title">今日目标</text>
                    <text class="g-sub">{{ todayMin }} / {{ goal }} 分钟</text>
                </view>
                <view class="g-edit" @tap="openGoal">修改</view>
            </view>

            <!-- 菜单列表（02 §7.4） -->
            <view class="card menu-card">
                <view v-for="m in menus" :key="m.label" class="m-row" @tap="onMenu(m)">
                    <text class="m-label">{{ m.label }}</text>
                    <view class="m-arrow" />
                </view>
            </view>
        </block>

        <view v-else class="login-block">
            <empty-state mode="page" icon="seal" text="登录后管理你的阅读与账户" action-text="去登录" @action="goLogin" />
        </view>

        <!-- 目标设置弹层（02 §7.3：步进 10，10-240） -->
        <view v-if="showGoal" class="mask" @tap="showGoal = false">
            <view class="goal-pop" @tap.stop>
                <text class="gp-title">每日阅读目标</text>
                <view class="gp-stepper">
                    <view class="gp-btn" :class="{ off: goalDraft <= 10 }" @tap="stepGoal(-10)">−</view>
                    <view class="gp-val">
                        <text class="gp-num">{{ goalDraft }}</text>
                        <text class="gp-unit">分钟</text>
                    </view>
                    <view class="gp-btn" :class="{ off: goalDraft >= 240 }" @tap="stepGoal(10)">＋</view>
                </view>
                <view class="gp-quick">
                    <text
                        v-for="q in [30, 60, 90]"
                        :key="q"
                        class="gp-tag"
                        :class="{ on: goalDraft === q }"
                        @tap="goalDraft = q"
                    >{{ q }} 分</text>
                </view>
                <view class="gp-actions">
                    <view class="gp-cancel" @tap="showGoal = false">取消</view>
                    <view class="gp-save" :class="{ loading: goalSaving }" @tap="saveGoal">
                        {{ goalSaving ? '保存中…' : '保存' }}
                    </view>
                </view>
            </view>
        </view>
    </view>
</template>

<script lang="ts" setup>
// 我的页 —— S3 批 3 第七页（02 §7 + board07）
// 顶栏四 Tab 之外的个人中心；底座功能页（设置/协议/客服）直接复用
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { statsOverview } from '@/api/stats'
import { readingGoalGet, readingGoalSet } from '@/api/reading'
import { navigateTo } from '@/utils/util'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const isLogin = computed(() => userStore.isLogin)
const userInfo = computed(() => userStore.userInfo || {})

const total = ref<any>({})
const today = ref<any>({})
const goal = ref(30)

const todayMin = computed(() => today.value.duration_minutes ?? 0)
const goalPct = computed(() => Math.min(100, Math.round((todayMin.value / (goal.value || 30)) * 100)))
const totalHuman = computed(() => {
    const sec = total.value.duration_seconds
    if (sec == null) return '--'
    const h = Math.floor(sec / 3600)
    const m = Math.floor((sec % 3600) / 60)
    return h > 0 ? `${h}时${m}分` : `${m}分`
})
const ringStyle = computed(() => ({
    background: `conic-gradient(var(--cinnabar) ${goalPct.value * 3.6}deg, var(--paper-deep) 0deg)`
}))

// 菜单（底座页复用：user_set/as_us/customer_service/agreement）
const menus = [
    { label: '我的书架', to: '/pages/bookshelf/index' },
    { label: '阅读统计', to: '/pages/stats/index' },
    { label: '关于我们', to: '/pages/as_us/as_us' },
    { label: '联系客服', to: '/pages/customer_service/customer_service' },
    { label: '用户协议', to: '/pages/agreement/agreement?type=service' },
    { label: '隐私政策', to: '/pages/agreement/agreement?type=privacy' },
    { label: '设置', to: '/pages/user_set/user_set' }
]

function onMenu(m: any) {
    navigateTo({ path: m.to })
}

// —— 目标弹层 ——
const showGoal = ref(false)
const goalDraft = ref(30)
const goalSaving = ref(false)

function openGoal() {
    goalDraft.value = goal.value
    showGoal.value = true
}

function stepGoal(delta: number) {
    const next = goalDraft.value + delta
    if (next < 10 || next > 240) return
    goalDraft.value = next
}

async function saveGoal() {
    if (goalSaving.value) return
    goalSaving.value = true
    try {
        await readingGoalSet(goalDraft.value)
        goal.value = goalDraft.value
        showGoal.value = false
        uni.showToast({ title: '目标已更新', icon: 'success' })
    } catch (e) {
        uni.showToast({ title: '保存失败，稍后再试', icon: 'none' })
    } finally {
        goalSaving.value = false
    }
}

function goLogin() {
    navigateTo({ path: '/pages/login/login' })
}
function goEdit() {
    navigateTo({ path: '/pages/user_data/user_data' })
}
function goStats() {
    navigateTo({ path: '/pages/stats/index' })
}

async function loadAll() {
    if (!userStore.isLogin) return
    userStore.getUser().catch(() => {})
    try {
        const res: any = await statsOverview()
        total.value = res.total || {}
        today.value = res.today || {}
        goal.value = res.goal ?? 30
    } catch (e) {
        /* 概览失败保持占位 */
    }
}

onShow(() => {
    loadAll()
})
</script>

<style lang="scss" scoped>
.center-page {
    min-height: 100vh;
    background: var(--paper);
    padding-bottom: 64rpx;
}

/* 顶部：宣纸深一度区托用户卡（board07） */
.hero {
    background: var(--paper-deep);
    padding: 0 32rpx 32rpx;
}

.user-card {
    display: flex;
    align-items: center;
    gap: 24rpx;
    padding: 48rpx 8rpx 32rpx;

    .avatar {
        width: 112rpx;
        height: 112rpx;
        border-radius: 50%;
        border: 2rpx solid var(--line);
        background: var(--card);

        &.guest {
            display: flex;
            align-items: center;
            justify-content: center;
            font-family: var(--font-serif);
            font-size: 40rpx;
            color: var(--ink-3);
        }
    }
    .u-main {
        flex: 1;
        min-width: 0;
        display: flex;
        flex-direction: column;
        gap: 8rpx;

        .nickname {
            font-family: var(--font-serif);
            font-size: 36rpx;
            color: var(--ink);
            font-weight: 600;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }
        .sn {
            font-size: 22rpx;
            color: var(--ink-3);
        }
    }
    .edit-btn {
        flex-shrink: 0;
        padding: 0 24rpx;
        height: 56rpx;
        line-height: 56rpx;
        border-radius: 28rpx;
        border: 1px solid var(--line);
        background: var(--card);
        color: var(--ink-2);
        font-size: 24rpx;
    }
}

/* 三格摘要 */
.sum-card {
    display: flex;
    align-items: center;
    background: var(--card);
    border: 1px solid var(--line);
    border-radius: 12rpx;
    box-shadow: var(--card-shadow);
    padding: 32rpx 16rpx;

    .s-item {
        flex: 1;
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 8rpx;

        .s-num {
            font-family: var(--font-serif);
            font-size: 36rpx;
            color: var(--ink);
            font-weight: 600;
        }
        .s-label {
            font-size: 22rpx;
            color: var(--ink-3);
        }
    }
    .s-divider {
        width: 1rpx;
        height: 48rpx;
        background: var(--line);
    }
}

.card {
    background: var(--card);
    border: 1px solid var(--line);
    border-radius: 12rpx;
    box-shadow: var(--card-shadow);
    padding: 32rpx;
    margin: 24rpx 32rpx 0;
}

/* 今日目标行 */
.goal-card {
    display: flex;
    align-items: center;
    gap: 24rpx;

    .g-ring {
        width: 96rpx;
        height: 96rpx;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;

        .g-ring-inner {
            width: 76rpx;
            height: 76rpx;
            border-radius: 50%;
            background: var(--card);
            display: flex;
            align-items: center;
            justify-content: center;
        }
    }
    .g-pct {
        font-size: 22rpx;
        color: var(--cinnabar);
        font-family: var(--font-serif);
    }
    .g-body {
        flex: 1;
        display: flex;
        flex-direction: column;
        gap: 6rpx;

        .g-title {
            font-size: 28rpx;
            color: var(--ink);
            font-weight: 600;
        }
        .g-sub {
            font-size: 24rpx;
            color: var(--ink-3);
        }
    }
    .g-edit {
        flex-shrink: 0;
        padding: 0 24rpx;
        height: 56rpx;
        line-height: 56rpx;
        border-radius: 28rpx;
        border: 1px solid var(--line);
        color: var(--ink-2);
        font-size: 24rpx;
    }
}

/* 菜单列表 */
.menu-card {
    padding: 8rpx 32rpx;

    .m-row {
        display: flex;
        align-items: center;
        justify-content: space-between;
        height: 96rpx;

        & + .m-row {
            border-top: 1rpx solid var(--line);
        }
        .m-label {
            font-size: 28rpx;
            color: var(--ink-2);
        }
        .m-arrow {
            width: 14rpx;
            height: 14rpx;
            border-top: 3rpx solid var(--ink-3);
            border-right: 3rpx solid var(--ink-3);
            transform: rotate(45deg);
        }
    }
}

.login-block {
    padding-top: 80rpx;
}

/* 目标弹层 */
.mask {
    position: fixed;
    inset: 0;
    background: rgba(38, 32, 26, 0.45);
    z-index: 999;
    display: flex;
    align-items: center;
    justify-content: center;
}

.goal-pop {
    width: 600rpx;
    background: var(--card);
    border-radius: 16rpx;
    padding: 48rpx 40rpx 40rpx;
    display: flex;
    flex-direction: column;
    gap: 32rpx;

    .gp-title {
        text-align: center;
        font-family: var(--font-serif);
        font-size: 32rpx;
        color: var(--ink);
        font-weight: 600;
    }
    .gp-stepper {
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 48rpx;

        .gp-btn {
            width: 72rpx;
            height: 72rpx;
            border-radius: 50%;
            border: 1px solid var(--line);
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 40rpx;
            color: var(--ink-2);

            &.off {
                opacity: 0.3;
            }
        }
        .gp-val {
            min-width: 200rpx;
            text-align: center;
            display: flex;
            align-items: baseline;
            justify-content: center;
            gap: 8rpx;

            .gp-num {
                font-family: var(--font-serif);
                font-size: 64rpx;
                color: var(--ink);
                font-weight: 600;
            }
            .gp-unit {
                font-size: 24rpx;
                color: var(--ink-3);
            }
        }
    }
    .gp-quick {
        display: flex;
        justify-content: center;
        gap: 24rpx;

        .gp-tag {
            padding: 0 28rpx;
            height: 52rpx;
            line-height: 52rpx;
            border-radius: 26rpx;
            border: 1px solid var(--line);
            color: var(--ink-2);
            font-size: 24rpx;

            &.on {
                border-color: var(--cinnabar);
                color: var(--cinnabar);
            }
        }
    }
    .gp-actions {
        display: flex;
        gap: 24rpx;
        margin-top: 8rpx;

        .gp-cancel,
        .gp-save {
            flex: 1;
            height: 80rpx;
            display: flex;
            align-items: center;
            justify-content: center;
            border-radius: 12rpx;
            font-size: 28rpx;
        }
        .gp-cancel {
            border: 1px solid var(--line);
            color: var(--ink-2);
        }
        .gp-save {
            background: var(--cinnabar);
            color: #ffffff;

            &.loading {
                opacity: 0.6;
            }
        }
    }
}
</style>
