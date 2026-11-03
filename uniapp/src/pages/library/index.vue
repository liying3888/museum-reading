<template>
    <view class="library-page">
        <view class="placeholder">书库页（批 3 对照 board02 落码）</view>
    </view>
</template>

<script lang="ts" setup>
// 书库页占位 —— S3 批 1 前端地基（tabbar 四项路由先行）
// 批 3 落码：搜索防抖+分类 chips+书笺卡列表+分页（docs/design/02 §2、board02）
</script>

<style lang="scss" scoped>
.library-page {
    min-height: 100vh;
    background: var(--paper);
}
.placeholder {
    padding: 200rpx 32rpx;
    text-align: center;
    color: var(--ink-3);
    font-size: 24rpx;
}
</style>
