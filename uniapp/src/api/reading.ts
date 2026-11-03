import request from '@/utils/request'

// 阅读计时域+每日目标——契约 R1-R6/G1/G2（docs/S3/01 §2.3/§2.4）
// url 不带 /api 前缀（拦截器统一拼）

/**
 * R1 开始阅读（幂等版：存在未结束会话则返回该会话 resumed=true，不新建不串页码——02 §5.4）
 * book_id 缺省=自由阅读
 */
export function readingStart(bookId?: number) {
    return request.post({ url: '/front/reading/start', data: { book_id: bookId } })
}

/** R2 暂停（真暂停：paused_at=now，时长停走——02 §0.3） */
export function readingPause(recordId: number) {
    return request.post({ url: '/front/reading/pause', data: { record_id: recordId } })
}

/** R3 继续（paused_total 累计后清 paused_at） */
export function readingResume(recordId: number) {
    return request.post({ url: '/front/reading/resume', data: { record_id: recordId } })
}

/**
 * R4 结束（真暂停结算：duration=end−start−Σ暂停段）
 * 返回成果数据 { duration, today_total, streak_days, goal, goal_gap, auto_closed }
 */
export function readingEnd(recordId: number) {
    return request.post({ url: '/front/reading/end', data: { record_id: recordId } })
}

/** R5 当前进行中会话（首页/书架「回到计时」联动+onShow 恢复态；无会话返回 null） */
export function readingSession() {
    return request.get({ url: '/front/reading/session' })
}

/** 阅读记录分页（记录管理列表；返 auto_closed/book_title/duration） */
export function readingRecords(params: { page_no?: number; page_size?: number }) {
    return request.get({ url: '/front/reading/records', data: params })
}

/** R6 删除异常记录（二次确认后调；今日/本周累计天然重算） */
export function readingRecordDelete(recordId: number) {
    return request.post({
        url: '/front/reading/record/delete',
        data: { record_id: recordId }
    })
}

/** G1 每日目标读取（无配置兜底 30 分钟） */
export function readingGoalGet() {
    return request.get({ url: '/front/reading/goal' })
}

/** G2 每日目标设置（10-240 分钟，越界后端拒） */
export function readingGoalSet(minutes: number) {
    return request.post({ url: '/front/reading/goal', data: { minutes } })
}
