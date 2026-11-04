import request from '@/utils/request'

// 书籍列表（status 不传=全部）
export function bookLists(params?: any) {
    return request.get({ url: '/book/list', params })
}

// 书籍详情
export function bookDetail(params: any) {
    return request.get({ url: '/book/detail', params })
}

// 添加书籍
export function bookAdd(params: any) {
    return request.post({ url: '/book/add', params })
}

// 编辑书籍（上下架复用）
export function bookEdit(params: any) {
    return request.post({ url: '/book/edit', params })
}

// 删除书籍（软删=下架，读者端不可见、阅读记录保留）
export function bookDelete(params: any) {
    return request.post({ url: '/book/delete', params })
}

// 分类列表（status 不传=全部，含 bookCount 书目计数）
export function bookCateLists(params?: any) {
    return request.get({ url: '/category/list', params })
}

// 分类详情
export function bookCateDetail(params: any) {
    return request.get({ url: '/category/detail', params })
}

// 添加分类
export function bookCateAdd(params: any) {
    return request.post({ url: '/category/add', params })
}

// 编辑分类（改名/排序/启停；停用时后端校验书目迁移）
export function bookCateEdit(params: any) {
    return request.post({ url: '/category/edit', params })
}

// 停用分类（软删 status=0；有书目时后端阻拦）
export function bookCateDelete(params: any) {
    return request.post({ url: '/category/del', params })
}

// 数据看板聚合
export function statisticsOverview(params?: any) {
    return request.get({ url: '/statistics/overview', params })
}
