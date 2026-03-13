import request from '@/utils/request'

// 书籍列表
export function bookList(params: any) {
  return request.get({ url: '/api/front/books/list', data: params })
}

// 书籍详情
export function bookDetail(id: number) {
  return request.get({ url: '/api/front/books/detail', data: { id } })
}

// 书架列表
export function bookshelfList(params: any) {
  return request.get({ url: '/api/front/bookshelf/list', data: params })
}

// 加入书架
export function bookshelfAdd(bookId: number) {
  return request.post({ url: '/api/front/bookshelf/add', data: { book_id: bookId } })
}

// 更新进度
export function bookshelfProgress(bookId: number, currentPage: number) {
  return request.post({ url: '/api/front/bookshelf/progress', data: { book_id: bookId, current_page: currentPage } })
}

// 开始阅读
export function readingStart(bookId?: number) {
  return request.post({ url: '/api/front/reading/start', data: { book_id: bookId } })
}

// 结束阅读
export function readingEnd(recordId: number) {
  return request.post({ url: '/api/front/reading/end', data: { record_id: recordId } })
}

// 阅读统计
export function readingStats() {
  return request.get({ url: '/api/front/reading/stats' })
}
