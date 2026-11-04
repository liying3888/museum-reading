<template>
    <div class="book-lists">
        <!-- 搜索卡（板 09：书名/作者+分类+状态） -->
        <el-card class="!border-none" shadow="never">
            <el-form class="mb-[-16px]" :model="queryParams" :inline="true">
                <el-form-item label="书名">
                    <el-input
                        class="w-[200px]"
                        v-model="queryParams.title"
                        clearable
                        placeholder="书名模糊搜索"
                        @keyup.enter="resetPage"
                    />
                </el-form-item>
                <el-form-item label="作者">
                    <el-input
                        class="w-[180px]"
                        v-model="queryParams.author"
                        clearable
                        placeholder="作者模糊搜索"
                        @keyup.enter="resetPage"
                    />
                </el-form-item>
                <el-form-item label="分类">
                    <el-select class="w-[160px]" v-model="queryParams.categoryId">
                        <el-option label="全部" value />
                        <el-option
                            v-for="item in optionsData.cates"
                            :key="item.id"
                            :label="item.name"
                            :value="item.id"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item label="状态">
                    <el-select class="w-[140px]" v-model="queryParams.status">
                        <el-option label="全部" value />
                        <el-option label="上架" :value="1" />
                        <el-option label="下架" :value="0" />
                    </el-select>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="resetPage">查询</el-button>
                    <el-button @click="resetParams">重置</el-button>
                </el-form-item>
            </el-form>
        </el-card>

        <!-- 表格卡（板 09：八列+新增主按钮+分页 10/页） -->
        <el-card class="!border-none mt-4" shadow="never">
            <div>
                <el-button v-perms="['book:add']" type="primary" class="mb-4" @click="openDrawer()">
                    <template #icon>
                        <icon name="el-icon-Plus" />
                    </template>
                    新增书籍
                </el-button>
            </div>

            <el-table size="large" v-loading="pager.loading" :data="pager.lists">
                <el-table-column label="封面" min-width="72">
                    <template #default="{ row }">
                        <image-contain
                            v-if="row.cover"
                            :src="row.cover"
                            :width="44"
                            :height="60"
                            :preview-src-list="[row.cover]"
                            preview-teleported
                            fit="contain"
                        />
                        <!-- 缺省=黛蓝书脊占位（03 §4，与读者端同源） -->
                        <div v-else class="cover-slot">
                            <span>{{ row.title?.slice(0, 4) }}</span>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column label="书名" min-width="180" show-overflow-tooltip>
                    <template #default="{ row }">
                        <span class="cell-title">{{ row.title }}</span>
                    </template>
                </el-table-column>
                <el-table-column label="作者" prop="author" min-width="120" show-overflow-tooltip />
                <el-table-column label="分类" min-width="100">
                    <template #default="{ row }">
                        <el-tag
                            v-if="cateName(row.categoryId)"
                            class="!border-none cate-tag"
                            effect="plain"
                        >
                            {{ cateName(row.categoryId) }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="页数" prop="pages" min-width="80" />
                <el-table-column label="评分" min-width="90">
                    <template #default="{ row }">
                        <span v-if="row.rating" class="cell-rating">
                            ★ {{ row.rating }}
                        </span>
                        <span v-else class="text-muted">—</span>
                    </template>
                </el-table-column>
                <el-table-column label="状态" min-width="90">
                    <template #default="{ row }">
                        <span :class="row.status === 1 ? 'dot on' : 'dot off'"></span>
                        {{ row.status === 1 ? '上架' : '下架' }}
                    </template>
                </el-table-column>
                <el-table-column label="上架时间" min-width="110">
                    <template #default="{ row }">
                        {{ timeFormat(row.createTime) }}
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="130" fixed="right">
                    <template #default="{ row }">
                        <el-button v-perms="['book:edit']" type="primary" link @click="openDrawer(row.id)">
                            编辑
                        </el-button>
                        <el-button
                            v-perms="['book:delete']"
                            class="btn-fail"
                            link
                            @click="handleDelete(row)"
                        >
                            删除
                        </el-button>
                    </template>
                </el-table-column>

                <!-- 空态（板 12 帧 C） -->
                <template #empty>
                    <div class="empty-box">
                        <div class="empty-cover"></div>
                        <p class="empty-title">还没有上架书籍</p>
                        <p class="empty-desc">上架第一本书后，读者即可在书库中浏览</p>
                        <el-button v-perms="['book:add']" type="primary" @click="openDrawer()">
                            新增第一本书
                        </el-button>
                    </div>
                </template>
            </el-table>
            <div class="flex justify-end mt-4">
                <pagination v-model="pager" :page-size="10" @change="getLists" />
            </div>
        </el-card>

        <!-- 新增/编辑抽屉（板 12 帧 A：九字段） -->
        <el-drawer
            v-model="drawerVisible"
            :title="editId ? '编辑书籍' : '新增书籍'"
            size="480px"
            :close-on-click-modal="false"
        >
            <el-form ref="formRef" :model="formData" :rules="rules" label-width="84px">
                <el-form-item label="书名" prop="title">
                    <el-input v-model="formData.title" maxlength="50" show-word-limit placeholder="请输入书名" />
                </el-form-item>
                <el-form-item label="作者" prop="author">
                    <el-input v-model="formData.author" maxlength="30" show-word-limit placeholder="请输入作者" />
                </el-form-item>
                <el-form-item label="出版社" prop="publisher">
                    <el-input v-model="formData.publisher" maxlength="50" placeholder="选填" />
                </el-form-item>
                <el-form-item label="分类" prop="categoryId">
                    <el-select class="w-full" v-model="formData.categoryId" placeholder="请选择分类">
                        <el-option
                            v-for="item in optionsData.cates"
                            :key="item.id"
                            :label="item.name"
                            :value="item.id"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item label="总页数" prop="pages">
                    <el-input-number v-model="formData.pages" :min="1" :max="9999" />
                </el-form-item>
                <el-form-item label="评分" prop="rating">
                    <el-input-number
                        v-model="formData.rating"
                        :min="0"
                        :max="5"
                        :step="0.1"
                        :precision="1"
                        placeholder="默认不展示"
                    />
                    <div class="form-tip">留空则读者端不展示评分</div>
                </el-form-item>
                <el-form-item label="封面" prop="cover">
                    <div>
                        <material-picker v-model="formData.cover" :limit="1" />
                        <div class="form-tip">建议 200×280；缺失时读者端显示书脊占位</div>
                    </div>
                </el-form-item>
                <el-form-item label="简介" prop="intro">
                    <el-input
                        v-model="formData.intro"
                        type="textarea"
                        :rows="4"
                        maxlength="500"
                        show-word-limit
                        placeholder="选填，读者端 3 行折叠展示"
                    />
                </el-form-item>
                <el-form-item label="状态" prop="status">
                    <div>
                        <el-switch
                            v-model="formData.status"
                            :active-value="1"
                            :inactive-value="0"
                            active-text="上架"
                            inactive-text="下架"
                        />
                        <div class="form-tip">下架书籍读者端不可见</div>
                    </div>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="drawerVisible = false">取消</el-button>
                <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
            </template>
        </el-drawer>
    </div>
</template>

<script lang="ts" setup name="bookLists">
import {
    bookLists,
    bookDetail,
    bookAdd,
    bookEdit,
    bookDelete,
    bookCateLists
} from '@/api/book'
import { useDictOptions } from '@/hooks/useDictOptions'
import { usePaging } from '@/hooks/usePaging'
import feedback from '@/utils/feedback'

const queryParams = reactive({
    title: '',
    author: '',
    categoryId: '',
    status: ''
})

const { pager, getLists, resetPage, resetParams } = usePaging({
    fetchFun: bookLists,
    params: queryParams,
    size: 10 // 04 §3：每页 10 条
})

const { optionsData } = useDictOptions<{
    cates: any[]
}>({
    cates: {
        api: bookCateLists,
        // bookCateLists 返回 {lists, count} 分页结构，取 lists 作下拉项
        transformData: (data: any) => data.lists ?? []
    }
})

// —— 抽屉表单 ——
const formRef = shallowRef<FormInstance>()
const drawerVisible = ref(false)
const editId = ref<number | null>(null)
const saving = ref(false)
const formData = reactive({
    title: '',
    author: '',
    publisher: '',
    categoryId: '',
    pages: 100,
    rating: undefined as number | undefined,
    cover: '',
    intro: '',
    status: 0
})

const rules: FormRules = {
    title: [{ required: true, message: '请输入书名', trigger: 'blur' }],
    author: [{ required: true, message: '请输入作者', trigger: 'blur' }],
    categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
    pages: [{ required: true, message: '请输入总页数', trigger: 'blur' }]
}

const openDrawer = async (id?: number) => {
    editId.value = id ?? null
    Object.assign(formData, {
        title: '',
        author: '',
        publisher: '',
        categoryId: '',
        pages: 100,
        rating: undefined,
        cover: '',
        intro: '',
        status: 0
    })
    if (id) {
        const data = await bookDetail({ id })
        Object.assign(formData, {
            title: data.title,
            author: data.author,
            publisher: data.publisher || '',
            categoryId: data.categoryId,
            pages: data.pages,
            rating: data.rating ?? undefined,
            cover: data.cover || '',
            intro: data.intro || '',
            status: data.status
        })
    }
    drawerVisible.value = true
    nextTick(() => formRef.value?.clearValidate())
}

const handleSave = async () => {
    // focus-trap 下点保存时 InputNumber 可能未 blur，主动触发 change 让组件 commit 当前输入
    document.activeElement?.dispatchEvent(new Event('change', { bubbles: true }))
    await nextTick()
    await formRef.value?.validate()
    saving.value = true
    try {
        if (editId.value) {
            await bookEdit({ id: editId.value, ...formData })
            feedback.msgSuccess('保存成功')
            drawerVisible.value = false
            getLists() // 编辑态保持当前页原位刷新（04 §3）
        } else {
            await bookAdd(formData)
            feedback.msgSuccess('新增成功')
            drawerVisible.value = false
            pager.page = 1 // 新增跳第一页（04 §3）
            getLists()
        }
    } finally {
        saving.value = false
    }
}

// —— 删除（板 12 帧 B：含书名影响说明）——
const handleDelete = async (row: any) => {
    await feedback.confirm(
        `删除后读者端立即不可见，《${row.title}》的书架与阅读记录将保留。`
    )
    await bookDelete({ id: row.id })
    feedback.msgSuccess('删除成功')
    getLists()
}

// —— 杂项 ——
const cateName = (id: number) => (optionsData.cates || []).find((c: any) => c.id === id)?.name

const timeFormat = (ts: number) => {
    if (!ts) return '—'
    const d = new Date(ts * 1000)
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

onActivated(() => {
    getLists()
})

getLists()
</script>

<style lang="scss" scoped>
/* 04 §6 后台延展：表头底/状态点/书名衬线/分类米底标签——与 C 端 token 同源 */
.book-lists {
    .cell-title {
        font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
        font-weight: 600;
        color: #2c2a26;
    }
    .cell-rating {
        color: #b8935a; // 铜金（评分星，与 C 端 --gold 同源）
    }
    .cate-tag {
        background: #f1ece1; // paper-deep 米底标签
        color: #6b675e;
    }
    .dot {
        display: inline-block;
        width: 8px;
        height: 8px;
        border-radius: 50%;
        margin-right: 6px;
        &.on {
            background: #3d7a50; // 墨绿（上架）
        }
        &.off {
            background: #a8a296; // 枯墨（下架）
        }
    }
    .cover-slot {
        width: 44px;
        height: 60px;
        border-radius: 4px;
        background: #2f4154; // 黛蓝书脊占位
        color: #fff;
        font-size: 11px;
        line-height: 60px;
        text-align: center;
        overflow: hidden;
        padding: 0 4px;
        box-sizing: border-box;
    }
    .btn-fail {
        color: #a84234; // 危险色（03 §1.2 round1 修订）
    }
    .form-tip {
        font-size: 12px;
        color: #a8a296;
        line-height: 18px;
        margin-top: 4px;
    }
    .empty-box {
        padding: 40px 0 24px;
        .empty-cover {
            width: 72px;
            height: 96px;
            margin: 0 auto 16px;
            border: 1.5px dashed #a8a296;
            border-radius: 6px;
            position: relative;
            &::before {
                content: '书';
                position: absolute;
                inset: 0;
                display: flex;
                align-items: center;
                justify-content: center;
                font-family: 'Songti SC', 'STSong', serif;
                font-size: 28px;
                color: #a8a296;
            }
        }
        .empty-title {
            font-size: 15px;
            color: #2c2a26;
            margin: 0 0 4px;
        }
        .empty-desc {
            font-size: 13px;
            color: #a8a296;
            margin: 0 0 16px;
        }
    }
}
</style>
