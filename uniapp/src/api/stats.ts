import request from '@/utils/request'

/**
 * 获取今日统计
 */
export function getTodayStats() {
  return request.get('/front/stats/today')
}

/**
 * 获取本周统计
 */
export function getWeekStats() {
  return request.get('/front/stats/week')
}

/**
 * 获取本月统计
 */
export function getMonthStats() {
  return request.get('/front/stats/month')
}

/**
 * 获取累计统计
 */
export function getTotalStats() {
  return request.get('/front/stats/total')
}

/**
 * 获取打卡记录
 */
export function getCheckinRecords() {
  return request.get('/front/stats/checkin')
}

/**
 * 获取统计概览
 */
export function getStatsOverview() {
  return request.get('/front/stats/overview')
}
