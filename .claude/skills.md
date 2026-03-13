# Claude Code 开发技能

## Java Spring Boot 开发

### Controller 模板
```java
@RestController
@RequestMapping("api/admin/books")
@Api(tags = "书籍管理")
public class BookController {
    @Resource
    private IBookService bookService;
    
    @GetMapping("/list")
    @ApiOperation(value = "书籍列表")
    public AjaxResult list(@RequestParam(defaultValue = "1") Integer pageNo,
                          @RequestParam(defaultValue = "15") Integer pageSize) {
        // 使用 MyBatis-Plus 分页查询
        IPage<Book> page = bookService.page(new Page<>(pageNo, pageSize));
        return AjaxResult.success(page);
    }
}
```

### Service 模板
```java
@Service
public class BookServiceImpl extends ServiceImpl<BookMapper, Book> implements IBookService {
    // 业务逻辑
}
```

### Mapper 模板
```java
@Mapper
public interface BookMapper extends BaseMapper<Book> {
    // 自定义查询方法
}
```

## Vue 3 + TypeScript 开发

### 页面模板
```vue
<template>
  <view class="container">
    <!-- 页面内容 -->
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { apiFunction } from '@/api/module'

const data = ref<any[]>([])

const loadData = async () => {
  const res = await apiFunction()
  if (res.data) {
    data.value = res.data.lists
  }
}

onLoad(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
.container {
  padding: 20rpx;
}
</style>
```

### API 调用模板
```typescript
import request from '@/utils/request'

export function getList(params: any) {
  return request.get({ url: '/api/module/list', data: params })
}

export function getDetail(id: number) {
  return request.get({ url: '/api/module/detail', data: { id } })
}
```

## uni-app 小程序开发

### 页面跳转
```typescript
// 保留当前页面，跳转到新页面
uni.navigateTo({ url: '/pages/detail?id=123' })

// 关闭当前页面，跳转到新页面
uni.redirectTo({ url: '/pages/login' })

// 跳转到 tabBar 页面
uni.switchTab({ url: '/pages/index' })

// 返回上一页
uni.navigateBack({ delta: 1 })
```

### 数据缓存
```typescript
// 保存数据
uni.setStorageSync('key', data)

// 读取数据
const data = uni.getStorageSync('key')

// 删除数据
uni.removeStorageSync('key')
```

### 提示框
```typescript
// Toast
uni.showToast({ title: '操作成功', icon: 'success' })

// Loading
uni.showLoading({ title: '加载中...' })
uni.hideLoading()

// Modal
uni.showModal({
  title: '提示',
  content: '确定删除？',
  success: (res) => {
    if (res.confirm) {
      // 确认操作
    }
  }
})
```

## MyBatis-Plus 查询

### 条件查询
```java
QueryWrapper<Book> wrapper = new QueryWrapper<>();
wrapper.eq("status", 1)
       .like("title", keyword)
       .orderByDesc("create_time");
IPage<Book> page = bookService.page(new Page<>(1, 15), wrapper);
```

### 关联查询
```java
// 在 Mapper 中定义
@Select("SELECT b.*, bs.current_page " +
        "FROM la_book b " +
        "LEFT JOIN la_bookshelf bs ON b.id = bs.book_id " +
        "WHERE bs.user_id = #{userId}")
List<BookVO> selectUserBooks(@Param("userId") Integer userId);
```

## 常用 Git 操作

```bash
# 查看状态
git status

# 添加所有文件
git add .

# 提交
git commit -m "feat: 功能描述"

# 推送
git push origin feature/museum-reading

# 拉取
git pull origin feature/museum-reading

# 查看日志
git log --oneline -10
```

## Docker 常用命令

```bash
# 查看容器
docker ps

# 查看日志
docker logs likeadmin-java-mysql

# 进入容器
docker exec -it likeadmin-java-mysql bash

# 执行SQL
docker exec -i likeadmin-java-mysql mysql -uroot -proot likeadmin_java < sql/file.sql
```
