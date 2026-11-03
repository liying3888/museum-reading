<template>
    <view class="book-cover" :class="{ ph: !hasCover }" :style="phStyle">
        <image v-if="hasCover" class="cover-img" :src="cover" mode="aspectFill" @error="imgError = true" />
        <block v-else>
            <view class="spine" />
            <view class="fore-edge" />
            <text class="ph-title">{{ title }}</text>
        </block>
    </view>
</template>

<script lang="ts" setup>
/**
 * 封面组件（03 §4 书笺式占位）——书库/详情/首页/书架四页共用
 * 有图显图；无图=黛蓝三档底色（档色按书稳定：bookId 取模三，相邻错档）+
 * 左脊条（宽约9% 深色）+ 底书口条（高约13% 白18%）+ 暖白衬线书名。
 * 图片加载失败（运营录入坏链/图床失效）@error 兜底转占位，不留白块。
 * 尺寸由外层容器给定（组件 100% 填充）：书库 160×220 / 详情 200×280。
 */
import { computed, ref, watch } from 'vue'

const props = defineProps<{
    cover?: string
    title: string
    bookId?: number
}>()

const imgError = ref(false)
watch(
    () => props.cover,
    () => (imgError.value = false) // 封面来源变化时重置，避免旧错误粘住新图
)

const hasCover = computed(() => !!props.cover && !imgError.value)

// 三档顺序对齐 06 §5 示样映射：明朝(id 序0) #2F4154 / 三体(1) #3A4E63 / 百年孤独(2) #243342
const TONES = ['#2F4154', '#3A4E63', '#243342']

const phStyle = computed(() => {
    if (hasCover.value) return ''
    const tone = TONES[(props.bookId ?? 0) % 3]
    return `background: ${tone};`
})
</script>

<style lang="scss" scoped>
.book-cover {
    position: relative;
    width: 100%;
    height: 100%;
    border-radius: 4rpx;
    overflow: hidden;

    .cover-img {
        width: 100%;
        height: 100%;
    }

    .spine {
        position: absolute;
        left: 0;
        top: 0;
        bottom: 0;
        width: 9%;
        background: rgba(0, 0, 0, 0.22);
    }

    .fore-edge {
        position: absolute;
        left: 0;
        right: 0;
        bottom: 0;
        height: 13%;
        background: rgba(255, 255, 255, 0.18);
    }

    .ph-title {
        position: absolute;
        left: 9%;
        right: 8rpx;
        top: 16%;
        bottom: 13%;
        display: flex;
        align-items: center;
        justify-content: center;
        text-align: center;
        color: var(--night-ink);
        font-family: var(--font-serif);
        font-size: 22rpx;
        line-height: 1.5;
        overflow: hidden;
    }
}
</style>
