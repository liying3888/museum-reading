import request from '@/utils/request'

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

// 更新状态
export function bookshelfStatus(bookId: number, status: number) {
  return request.post({ url: '/api/front/bookshelf/status', data: { book_id: bookId, status } })
}
