import request from '@/utils/request'

// 统计域——契约 T1/T2+四基础接口（docs/S3/01 §2.4）
// url 不带 /api 前缀（拦截器统一拼）

/** 今日统计（分钟） */
export function statsToday() {
    return request.get({ url: '/front/stats/today' })
}

/** 本周统计（分钟） */
export function statsWeek() {
    return request.get({ url: '/front/stats/week' })
}

/** 本月统计（分钟） */
export function statsMonth() {
    return request.get({ url: '/front/stats/month' })
}

/** 累计统计（小时/天数） */
export function statsTotal() {
    return request.get({ url: '/front/stats/total' })
}

/**
 * T1 打卡记录（月参数化：月历增量请求）
 * @param month 'yyyy-MM'，缺省当月；日期数组为东八区日界（修 S1 UTC 串日）
 */
export function statsCheckin(month?: string) {
    return request.get({ url: '/front/stats/checkin', data: month ? { month } : {} })
}

/**
 * T2 统计概览（改造版：today/week/month/total + goal 键）
 * 首页目标环/统计页三格一次拿全
 */
export function statsOverview() {
    return request.get({ url: '/front/stats/overview' })
}

// ---------- 过渡 shim（存量页旧 import 兼容，批 3 页面重写时随引用消失移除）----------
export const getTodayStats = statsToday
export const getTotalStats = statsTotal
export const getStatsOverview = statsOverview
