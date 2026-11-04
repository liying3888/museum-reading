<template>
    <view v-if="showTabbar" class="tab-bar">
        <view
            v-for="(item, i) in tabs"
            :key="item.path"
            class="tb-item"
            @tap="switchTab(item)"
        >
            <!-- 底块风（03 §4）：圆角小底块内反白形——未选枯墨底+淡墨形，选中黛蓝底+纯白形 -->
            <view class="tb-block" :class="{ on: current === i }">
                <svg-icon :name="item.icon" :size="40" :color="current === i ? '#ffffff' : 'var(--ink-2)'" />
            </view>
            <text class="tb-text" :class="{ on: current === i }">{{ item.text }}</text>
        </view>
    </view>
</template>

<script lang="ts" setup>
// TabBar 四项 —— 批 4 底块风自绘（弃 u-tabbar，17 枚 sprite 体系统一）
// 语义（01 §3）：首页=卷轴 / 书库=立册 / 书架=书堆 / 我的=印章人
// 形色随态两制（round7 勘误）：未选 ink-2 淡墨形，选中纯白形；仅底色+形色切换，字重不变（round2 口径）
import { computed, ref } from 'vue'
import { navigateTo } from '@/utils/util'

const tabs = [
    { path: '/pages/index/index', text: '首页', icon: 'tab-scroll' },
    { path: '/pages/library/index', text: '书库', icon: 'tab-book' },
    { path: '/pages/bookshelf/index', text: '书架', icon: 'tab-stack' },
    { path: '/pages/user/center', text: '我的', icon: 'tab-sealman' }
]

const current = computed(() => {
    const pages = getCurrentPages()
    const route = pages[pages.length - 1]?.route || ''
    const i = tabs.findIndex((t) => t.path === '/' + route)
    return i
})

const showTabbar = computed(() => current.value >= 0)

function switchTab(item: { path: string }) {
    navigateTo({ path: item.path, type: 'custom', isTab: true }, 'reLaunch')
}
</script>

<style lang="scss" scoped>
.tab-bar {
    position: fixed;
    left: 0;
    right: 0;
    bottom: 0;
    display: flex;
    background: var(--card);
    border-top: 1rpx solid var(--line);
    /* 底部安全区（全面屏手势条） */
    padding-bottom: constant(safe-area-inset-bottom);
    padding-bottom: env(safe-area-inset-bottom);
    z-index: 998;
}

.tb-item {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 6rpx;
    padding: 12rpx 0 8rpx;

    .tb-block {
        width: 56rpx;
        height: 56rpx;
        border-radius: 10rpx;
        background: var(--ink-3);
        display: flex;
        align-items: center;
        justify-content: center;

        &.on {
            background: var(--indigo);
        }
    }
    .tb-text {
        font-size: 20rpx;
        color: var(--ink-2);

        &.on {
            color: var(--indigo);
        }
    }
}
</style>
