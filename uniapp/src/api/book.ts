import request from '@/utils/request'

// 书籍域（公开接口，无需登录）——契约 B1/B2（docs/S3/01 §2.1）
// 注意：url 不带 /api 前缀（request 拦截器 urlPrefix 已统一拼，S1 双前缀 404 病根）

/**
 * B1 书籍列表（书库页/首页书单）
 * @param params { page_no, page_size, category_id?, keyword? }
 */
export function bookList(params: {
    page_no?: number
    page_size?: number
    category_id?: number
    keyword?: string
}) {
    return request.get({ url: '/front/books/list', data: params })
}

/**
 * B2 书籍详情（详情页头区）
 */
export function bookDetail(id: number) {
    return request.get({ url: '/front/books/detail', data: { id } })
}

// ---------- 过渡 shim（存量页旧 import 兼容，批 3 页面重写时随引用消失移除）----------
import { bookshelfList, bookshelfAdd } from './bookshelf'
import { statsOverview } from './stats'
import { readingStart, readingEnd } from './reading'
export { bookshelfList, bookshelfAdd, readingStart, readingEnd }
/** 旧版阅读统计入口 → 统计概览（旧 reading/stats 端点已由 stats 域 overview 承接） */
export const readingStats = statsOverview
