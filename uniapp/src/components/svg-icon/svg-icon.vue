<template>
    <view class="svg-icon" :style="boxStyle">
        <view class="svg-icon-shape" :style="shapeStyle" />
    </view>
</template>

<script lang="ts" setup>
/**
 * SVG 图标组件（03 §4 图标体系·17 枚 sprite 的运行时载体）
 * 17 枚=TabBar 4（tab-* 底块风 fill 形）+菜单 7（m-* 线性 stroke 1.5）+功能 6（f-* 线性）
 * 呈现：CSS mask+backgroundColor 控色（currentColor 在 mask 下不可用，色由本组件注入）
 * 跨端：h5/小程序 webview 渲染均支持 -webkit-mask；skyline 未验证（底座 webview 渲染，批 7 真机核）
 */
import { computed } from 'vue'

// 17 枚+实心星变体：vite 资产 import（dev 服务/构建哈希/小程序拷贝三端路径统一）
import tabScroll from '@/static/svg/tab-scroll.svg'
import tabBook from '@/static/svg/tab-book.svg'
import tabStack from '@/static/svg/tab-stack.svg'
import tabSealman from '@/static/svg/tab-sealman.svg'
import mShelf from '@/static/svg/m-shelf.svg'
import mChart from '@/static/svg/m-chart.svg'
import mMuseum from '@/static/svg/m-museum.svg'
import mChat from '@/static/svg/m-chat.svg'
import mDoc from '@/static/svg/m-doc.svg'
import mShield from '@/static/svg/m-shield.svg'
import mGear from '@/static/svg/m-gear.svg'
import fPlay from '@/static/svg/f-play.svg'
import fPlus from '@/static/svg/f-plus.svg'
import fSearch from '@/static/svg/f-search.svg'
import fBack from '@/static/svg/f-back.svg'
import fStar from '@/static/svg/f-star.svg'
import fStarFill from '@/static/svg/f-star-fill.svg'

const ICONS: Record<string, string> = {
    'tab-scroll': tabScroll,
    'tab-book': tabBook,
    'tab-stack': tabStack,
    'tab-sealman': tabSealman,
    'm-shelf': mShelf,
    'm-chart': mChart,
    'm-museum': mMuseum,
    'm-chat': mChat,
    'm-doc': mDoc,
    'm-shield': mShield,
    'm-gear': mGear,
    'f-play': fPlay,
    'f-plus': fPlus,
    'f-search': fSearch,
    'f-back': fBack,
    'f-star': fStar,
    'f-star-fill': fStarFill
}

const props = withDefaults(
    defineProps<{
        name: string // svg 文件名（不含 .svg）：tab-scroll|tab-book|tab-stack|tab-sealman|m-*7|f-*6（f-star-fill 实心变体）
        size?: number // rpx，缺省 40（≈20px）
        color?: string // 形色，缺省 var(--ink)
    }>(),
    { size: 40, color: '' }
)

const boxStyle = computed(() => ({
    width: props.size + 'rpx',
    height: props.size + 'rpx'
}))

const shapeStyle = computed(() => {
    const color = props.color || 'var(--ink)'
    const src = ICONS[props.name]
    const url = src ? `url("${src}")` : 'none'
    return {
        '-webkit-mask-image': url,
        'mask-image': url,
        '-webkit-mask-size': '100% 100%',
        'mask-size': '100% 100%',
        '-webkit-mask-repeat': 'no-repeat',
        'mask-repeat': 'no-repeat',
        'background-color': color
    } as Record<string, string>
})
</script>

<style lang="scss" scoped>
.svg-icon {
    flex-shrink: 0;
    display: inline-flex;

    .svg-icon-shape {
        width: 100%;
        height: 100%;
    }
}
</style>
