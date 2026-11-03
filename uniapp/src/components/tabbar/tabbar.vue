<template>
    <u-tabbar
        v-if="showTabbar"
        v-model="current"
        :list="tabbarList"
        bgcolor="#F8F5EF"
        active-color="#2F4154"
        inactive-color="#6B675E"
        :border-top="true"
        :hide-tab-bar="false"
        @change="handleChange"
    ></u-tabbar>
</template>

<script lang="ts" setup>
// TabBar 四项 —— S3 前端地基（docs/design/01 §3 定稿）
// 改造自底座原版（appStore 后端配置驱动→内置四项静态配置）：
// 236 全新 init 库无 tabbar 配置数据，且设计定稿四项为产品结构（首页/书库/书架/我的，
// 统计不占 Tab 位），不走后端风格配置。图标=底块风 SVG data-uri（icons.ts）。
import { TABBAR_ICONS } from './icons'
import { navigateTo } from '@/utils/util'
import { computed, ref } from 'vue'

const current = ref()

const TABS = [
    { path: '/pages/index/index', text: '首页', icon: TABBAR_ICONS.scroll },
    { path: '/pages/library/index', text: '书库', icon: TABBAR_ICONS.book },
    { path: '/pages/bookshelf/index', text: '书架', icon: TABBAR_ICONS.stack },
    { path: '/pages/user/center', text: '我的', icon: TABBAR_ICONS.person }
]

const tabbarList = computed(() =>
    TABS.map((item) => ({
        iconPath: item.icon.off,
        selectedIconPath: item.icon.on,
        text: item.text,
        pagePath: item.path,
        link: { path: item.path }
    }))
)

const showTabbar = computed(() => {
    const currentPages = getCurrentPages()
    const currentPage = currentPages[currentPages.length - 1]
    const index = tabbarList.value.findIndex((item: any) => {
        return item.pagePath === '/' + currentPage.route
    })
    return index >= 0
})

const handleChange = (index: number) => {
    const selectTab = tabbarList.value[index]
    navigateTo(selectTab.link, 'reLaunch')
}
</script>
