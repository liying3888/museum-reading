import request from '@/utils/request'

// 开始计时
export function readingStart(bookId?: number) {
  return request.post({ url: '/api/front/reading/start', data: { book_id: bookId } })
}

// 结束计时
export function readingEnd(recordId: number) {
  return request.post({ url: '/api/front/reading/end', data: { record_id: recordId } })
}

// 阅读统计
export function readingStats() {
  return request.get({ url: '/api/front/reading/stats' })
}

// 阅读记录
export function readingRecords(params: any) {
  return request.get({ url: '/api/front/reading/records', data: params })
}
