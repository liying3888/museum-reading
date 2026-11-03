import request from '@/utils/request'

// 书架域——契约 S1-S3+既有三接口（docs/S3/01 §2.2）
// url 不带 /api 前缀（拦截器统一拼）

/**
 * S1 书架列表（改造版：排序=置顶优先+update_time 倒序；联表 book 信息）
 * @param params { page_no, page_size, status? }——status 不传=全量（详情页在架判定用）
 */
export function bookshelfList(params: {
    page_no?: number
    page_size?: number
    status?: number
}) {
    return request.get({ url: '/front/bookshelf/list', data: params })
}

/** 加入书架（既有，默认想读态） */
export function bookshelfAdd(bookId: number) {
    return request.post({ url: '/front/bookshelf/add', data: { book_id: bookId } })
}

/** 更新进度（既有；达到总页数的转已读确认由前端发起，后端不自动转） */
export function bookshelfProgress(bookId: number, currentPage: number) {
    return request.post({
        url: '/front/bookshelf/progress',
        data: { book_id: bookId, current_page: currentPage }
    })
}

/** 更新状态（既有：0 想读/1 在读/2 已读） */
export function bookshelfStatus(bookId: number, status: number) {
    return request.post({ url: '/front/bookshelf/status', data: { book_id: bookId, status } })
}

/** S2 移出书架（物理删书架行；阅读记录保留） */
export function bookshelfDelete(bookId: number) {
    return request.post({ url: '/front/bookshelf/delete', data: { book_id: bookId } })
}

/** S3 置顶切换（长按卡片置顶，0 取消/1 置顶） */
export function bookshelfPin(bookId: number, pinned: 0 | 1) {
    return request.post({
        url: '/front/bookshelf/pin',
        data: { book_id: bookId, pinned }
    })
}
