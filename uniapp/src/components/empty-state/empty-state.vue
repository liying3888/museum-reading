<template>
    <view class="empty-state" :class="mode">
        <view class="es-icon">
            <view v-if="icon === 'search'" class="ic-search">
                <view class="ring" />
                <view class="handle" />
            </view>
            <view v-else-if="icon === 'seal'" class="ic-seal">
                <view class="face" />
            </view>
            <view v-else class="ic-book">
                <view class="spine-line" />
                <view class="text-line t1" />
                <view class="text-line t2" />
            </view>
        </view>
        <view class="es-body">
            <text class="es-text">{{ text }}</text>
            <text v-if="subText" class="es-sub">{{ subText }}</text>
        </view>
        <view v-if="actionText" class="es-action" :class="mode" @tap="$emit('action')">
            {{ actionText }}
        </view>
    </view>
</template>

<script lang="ts" setup>
/**
 * 空态组件（03 §6 组件基线·两式并存口径）——round2 首期：极简线性图标点缀，全幅插画留迭代
 * mode="page"  整页空态：居中竖排，上下留白≥160rpx（使用页兜底高度）
 * mode="inline" 列表内嵌空态卡（board11 式）：图标左+文案右+右侧动作按钮横排
 * icon: book | search | seal（缺省 book）——线性描边简笔（stroke 1.5px 网格风的 CSS 等价物）
 */
withDefaults(
    defineProps<{
        mode?: 'page' | 'inline'
        icon?: 'book' | 'search' | 'seal'
        text: string
        subText?: string
        actionText?: string
    }>(),
    { mode: 'page', icon: 'book' }
)

defineEmits(['action'])
</script>

<style lang="scss" scoped>
/* 线性简笔统一：枯墨描边（空态属标注层，ink-3 合规——03 §1.2 边界条款） */
.empty-state {
    &.page {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: 160rpx 32rpx;
        gap: 24rpx;

        .es-body {
            display: flex;
            flex-direction: column;
            align-items: center;
            gap: 8rpx;
        }
        .es-action {
            margin-top: 8rpx;
            padding: 0 48rpx;
            height: 72rpx;
            line-height: 72rpx;
            border-radius: 12rpx;
            background: var(--indigo);
            color: #ffffff;
            font-size: 28rpx;
        }
    }

    &.inline {
        display: flex;
        align-items: center;
        gap: 16rpx;
        padding: 24rpx;

        .es-body {
            flex: 1;
            min-width: 0;
            display: flex;
            flex-direction: column;
            gap: 4rpx;
        }
        .es-sub {
            text-align: left;
        }
        .es-action {
            flex-shrink: 0;
            padding: 0 24rpx;
            height: 56rpx;
            line-height: 56rpx;
            border-radius: 8rpx;
            border: 1px solid var(--indigo);
            color: var(--indigo);
            font-size: 24rpx;
        }
    }
}

.es-icon {
    flex-shrink: 0;
    width: 48rpx;
    height: 48rpx;
    position: relative;
}

.es-text {
    font-size: 28rpx;
    color: var(--ink-2);
    text-align: center;
}

.es-sub {
    font-size: 24rpx;
    color: var(--ink-3);
    text-align: center;
}

/* —— 线性简笔（48rpx 盒内作画，描边 2rpx≈1.5px）—— */
/* 书：描边框+脊线+两行页线 */
.ic-book {
    position: absolute;
    left: 8rpx;
    top: 4rpx;
    width: 32rpx;
    height: 40rpx;
    border: 2rpx solid var(--ink-3);
    border-radius: 4rpx;

    .spine-line {
        position: absolute;
        left: 8rpx;
        top: 6rpx;
        bottom: 6rpx;
        width: 2rpx;
        background: var(--ink-3);
    }
    .text-line {
        position: absolute;
        left: 14rpx;
        width: 12rpx;
        height: 2rpx;
        background: var(--ink-3);
        &.t1 {
            top: 12rpx;
        }
        &.t2 {
            top: 20rpx;
        }
    }
}

/* 放大镜：圆环+斜柄 */
.ic-search {
    .ring {
        position: absolute;
        left: 4rpx;
        top: 4rpx;
        width: 28rpx;
        height: 28rpx;
        border: 2rpx solid var(--ink-3);
        border-radius: 50%;
    }
    .handle {
        position: absolute;
        right: 4rpx;
        bottom: 4rpx;
        width: 16rpx;
        height: 2rpx;
        background: var(--ink-3);
        transform: rotate(45deg);
        transform-origin: right center;
    }
}

/* 印：圆印面+中心印文 */
.ic-seal {
    .face {
        position: absolute;
        left: 6rpx;
        top: 6rpx;
        width: 36rpx;
        height: 36rpx;
        border: 2rpx solid var(--ink-3);
        border-radius: 50%;

        &::after {
            content: '';
            position: absolute;
            left: 50%;
            top: 50%;
            width: 12rpx;
            height: 12rpx;
            transform: translate(-50%, -50%);
            background: var(--ink-3);
        }
    }
}
</style>
