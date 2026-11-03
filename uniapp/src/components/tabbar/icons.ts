/**
 * TabBar 底块风图标 —— S3 前端地基（docs/design/03-视觉方向.md §4 定稿）
 *
 * 规格：圆角小底块内反白图形；形色随态两制（round7 勘误口径）——
 *   未选：枯墨底 #A8A296 + 淡墨形 #6B675E
 *   选中：黛蓝底 #2F4154 + 纯白形 #FFFFFF
 * 语义（01 §3）：首页=卷轴 / 书库=立册 / 书架=书堆 / 我的=印章人
 * 实现：24×24 SVG（底块 rx5+形 stroke1.5 圆角端点）→ data-uri（H5/小程序
 * image/background 均可渲染；批 3 真机若遇兼容问题降级 PNG @2x/@3x）
 */

const svg = (block: string, shape: string, stroke: string) =>
    `data:image/svg+xml,${encodeURIComponent(
        `<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24">` +
        `<rect width="24" height="24" rx="5" fill="${block}"/>` +
        `<g fill="none" stroke="${stroke}" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">${shape}</g>` +
        `</svg>`
    )}`

// 四枚形（居中区 5.5~18.5，stroke 1.5 圆角端点）
const SHAPES = {
    // 首页=卷轴：左右轴杆+纸面横纹
    scroll:
        '<path d="M7.5 6.5 v11"/><path d="M16.5 6.5 v11"/>' +
        '<path d="M9.2 9 h5.6"/><path d="M9.2 12 h5.6"/><path d="M9.2 15 h3.4"/>',
    // 书库=立册：竖立书身+书脊线+封面题签线
    book:
        '<rect x="8" y="5.5" width="8" height="13" rx="1.5"/>' +
        '<path d="M10.6 5.5 v13"/><path d="M12.6 9 h2"/>',
    // 书架=书堆：三条横书错位叠放（末条略短）
    stack:
        '<rect x="6" y="6.5" width="12" height="3.4" rx="1"/>' +
        '<rect x="6" y="10.8" width="12" height="3.4" rx="1"/>' +
        '<rect x="8.2" y="15.1" width="7.6" height="3.4" rx="1"/>',
    // 我的=印章人：圆头+肩身弧
    person:
        '<circle cx="12" cy="8.6" r="2.7"/>' +
        '<path d="M6.8 18.4 c0-3.4 2.3-5.2 5.2-5.2 s5.2 1.8 5.2 5.2"/>'
} as const

// 未选（枯墨底+淡墨形）/ 选中（黛蓝底+纯白形）
const OFF = { block: '#A8A296', stroke: '#6B675E' }
const ON = { block: '#2F4154', stroke: '#FFFFFF' }

export const TABBAR_ICONS = {
    scroll: { off: svg(OFF.block, SHAPES.scroll, OFF.stroke), on: svg(ON.block, SHAPES.scroll, ON.stroke) },
    book: { off: svg(OFF.block, SHAPES.book, OFF.stroke), on: svg(ON.block, SHAPES.book, ON.stroke) },
    stack: { off: svg(OFF.block, SHAPES.stack, OFF.stroke), on: svg(ON.block, SHAPES.stack, ON.stroke) },
    person: { off: svg(OFF.block, SHAPES.person, OFF.stroke), on: svg(ON.block, SHAPES.person, ON.stroke) }
}
