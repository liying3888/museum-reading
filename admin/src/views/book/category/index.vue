<template>
    <div class="book-category">
        <!-- 行内新增（04 §4：≤10 字） -->
        <el-card class="!border-none" shadow="never">
            <div class="flex items-center">
                <el-input
                    class="w-[240px] mr-3"
                    v-model="newName"
                    maxlength="10"
                    show-word-limit
                    placeholder="新分类名称（≤10 字）"
                    @keyup.enter="handleAdd"
                />
                <el-button v-perms="['book:cate:add']" type="primary" @click="handleAdd">
                    添加分类
                </el-button>
            </div>
        </el-card>

        <el-card class="!border-none mt-4" shadow="never">
            <el-table size="large" v-loading="loading" :data="lists">
                <el-table-column label="分类名称" min-width="160">
                    <template #default="{ row }">
                        <span class="cell-title">{{ row.name }}</span>
                    </template>
                </el-table-column>
                <el-table-column label="书目计数" prop="bookCount" min-width="100" />
                <el-table-column label="排序" prop="sort" min-width="80" />
                <el-table-column label="状态" min-width="90">
                    <template #default="{ row }">
                        <span :class="row.status === 1 ? 'dot on' : 'dot off'"></span>
                        {{ row.status === 1 ? '启用' : '停用' }}
                    </template>
                </el-table-column>
                <el-table-column label="创建时间" min-width="110">
                    <template #default="{ row }">
                        {{ timeFormat(row.createTime) }}
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="140" fixed="right">
                    <template #default="{ row }">
                        <el-button v-perms="['book:cate:edit']" type="primary" link @click="openRename(row)">
                            改名
                        </el-button>
                        <el-button
                            v-if="row.status === 1"
                            v-perms="['book:cate:del']"
                            class="btn-fail"
                            link
                            @click="handleDisable(row)"
                        >
                            停用
                        </el-button>
                        <el-button v-else v-perms="['book:cate:edit']" type="primary" link @click="handleEnable(row)">
                            启用
                        </el-button>
                    </template>
                </el-table-column>
                <template #empty>
                    <el-empty description="还没有分类，先添加一个吧" />
                </template>
            </el-table>
        </el-card>

        <!-- 改名弹层 -->
        <el-dialog v-model="renameVisible" title="分类改名" width="380px">
            <el-form @submit.prevent>
                <el-form-item label="分类名称">
                    <el-input v-model="renameName" maxlength="10" show-word-limit />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="renameVisible = false">取消</el-button>
                <el-button type="primary" @click="handleRename">保存</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script lang="ts" setup name="bookCategory">
import { bookCateLists, bookCateAdd, bookCateEdit, bookCateDelete } from '@/api/book'
import feedback from '@/utils/feedback'

const lists = ref<any[]>([])
const loading = ref(false)

const getLists = async () => {
    loading.value = true
    try {
        const res = await bookCateLists()
        lists.value = res?.lists ?? []
    } finally {
        loading.value = false
    }
}

// —— 行内新增 ——
const newName = ref('')
const handleAdd = async () => {
    const name = newName.value.trim()
    if (!name) return feedback.msgError('请输入分类名称')
    // sort 取当前最大值+1（length 在有停用分类或 sort 不连续时会撞号）
    const maxSort = lists.value.reduce((m: number, c: any) => Math.max(m, c.sort || 0), 0)
    await bookCateAdd({ name, sort: maxSort + 1 })
    feedback.msgSuccess('添加成功')
    newName.value = ''
    getLists()
}

// —— 改名 ——
const renameVisible = ref(false)
const renameId = ref<number>(0)
const renameName = ref('')
const openRename = (row: any) => {
    renameId.value = row.id
    renameName.value = row.name
    renameVisible.value = true
}
const handleRename = async () => {
    const name = renameName.value.trim()
    if (!name) return feedback.msgError('请输入分类名称')
    await bookCateEdit({ id: renameId.value, name })
    feedback.msgSuccess('改名成功')
    renameVisible.value = false
    getLists()
}

// —— 停用/启用（停用阻拦在后端：分类下还有书时提示先迁移，04 §4）——
const handleDisable = async (row: any) => {
    await feedback.confirm(`确定停用「${row.name}」？停用后新增书籍不可选该分类。`)
    await bookCateDelete({ id: row.id })
    feedback.msgSuccess('已停用')
    getLists()
}

const handleEnable = async (row: any) => {
    await bookCateEdit({ id: row.id, status: 1 })
    feedback.msgSuccess('已启用')
    getLists()
}

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
/* 与书籍管理页同源（04 §6） */
.book-category {
    .cell-title {
        font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
        font-weight: 600;
        color: #2c2a26;
    }
    .dot {
        display: inline-block;
        width: 8px;
        height: 8px;
        border-radius: 50%;
        margin-right: 6px;
        &.on {
            background: #3d7a50;
        }
        &.off {
            background: #a8a296;
        }
    }
    .btn-fail {
        color: #a84234;
    }
}
</style>
