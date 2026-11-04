<template>
    <view class="nav-bar">
        <view class="nav-inner">
            <view class="nav-back" @tap="goBack">
                <svg-icon name="f-back" :size="26" color="var(--ink)" />
            </view>
            <text class="nav-title">{{ title }}</text>
            <view class="nav-right"><slot name="right" /></view>
        </view>
    </view>
</template>

<script lang="ts" setup>
/**
 * 自绘导航（03 §6 组件基线）：高 88rpx，左返回键（f-back sprite，热区≥88rpx）
 * +居中标题 34rpx 衬线——修 S1 统计页困死（无系统返回可用的场景全页统一自绘）
 * 返回兜底：无上级页面（reLaunch 进入）时回首页，不困死
 */
const props = defineProps<{ title?: string }>()

function goBack() {
    const pages = getCurrentPages()
    if (pages.length > 1) {
        uni.navigateBack({})
    } else {
        uni.reLaunch({ url: '/pages/index/index' })
    }
}
</script>

<style lang="scss" scoped>
.nav-bar {
    position: sticky;
    top: 0;
    z-index: 20;
    background: var(--paper);
    padding-top: var(--status-bar-height, 0px);
}

.nav-inner {
    position: relative;
    height: 88rpx;
    display: flex;
    align-items: center;
    justify-content: center;
}

.nav-back {
    position: absolute;
    left: 0;
    top: 0;
    width: 88rpx;
    height: 88rpx;
    display: flex;
    align-items: center;
    justify-content: center;
}

.nav-title {
    font-family: var(--font-serif);
    font-size: 34rpx;
    color: var(--ink);
    font-weight: 600;
}

.nav-right {
    position: absolute;
    right: 24rpx;
}
</style>
