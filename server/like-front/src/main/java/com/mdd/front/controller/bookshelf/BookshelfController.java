package com.mdd.front.controller.bookshelf;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdd.common.core.AjaxResult;
import com.mdd.front.LikeFrontThreadLocal;
import com.mdd.common.entity.books.Book;
import com.mdd.common.entity.books.BookCategory;
import com.mdd.common.entity.books.Bookshelf;
import com.mdd.common.mapper.books.BookCategoryMapper;
import com.mdd.common.mapper.books.BookMapper;
import com.mdd.common.mapper.books.BookshelfMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;

/**
 * 书架域——契约 S1-S3+既有 add/progress/status（docs/S3/01 §2.2，leader 2026-11-04 过闸）
 */
@RestController
@RequestMapping("api/front/bookshelf")
@Api(tags = "书架管理")
public class BookshelfController {

    @Resource
    private BookshelfMapper bookshelfMapper;

    @Resource
    private BookMapper bookMapper;

    @Resource
    private BookCategoryMapper bookCategoryMapper;

    /**
     * S1 书架列表（改造版）
     * 排序=pinned DESC, update_time DESC（02 §4.1：置顶优先，其余倒序）
     * status 不传=全量（详情页在架判定用，修 S1 status:-1 传参错误）
     */
    @GetMapping("/list")
    @ApiOperation(value = "我的书架")
    public AjaxResult list(@RequestParam(defaultValue = "1") Integer pageNo,
                          @RequestParam(defaultValue = "15") Integer pageSize,
                          @RequestParam(required = false) Integer status) {
        Integer userId = LikeFrontThreadLocal.getUserId();

        QueryWrapper<Bookshelf> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        if (status != null) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("pinned", "update_time");

        IPage<Bookshelf> page = bookshelfMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);

        // 联表 book 信息（批量查，修逐行 selectById 的 N+1）
        Map<Integer, Book> books = new HashMap<>();
        Map<Integer, String> categoryNames = new HashMap<>();
        Set<Integer> categoryIds = new HashSet<>();
        for (Bookshelf item : page.getRecords()) {
            if (!books.containsKey(item.getBookId())) {
                Book book = bookMapper.selectById(item.getBookId());
                if (book != null) {
                    books.put(item.getBookId(), book);
                    if (book.getCategoryId() != null) {
                        categoryIds.add(book.getCategoryId());
                    }
                }
            }
        }
        if (!categoryIds.isEmpty()) {
            for (BookCategory cate : bookCategoryMapper.selectBatchIds(categoryIds)) {
                categoryNames.put(cate.getId(), cate.getName());
            }
        }

        // 契约 §2.2 S1 出参：snake_case+嵌套 book（与 Books/Reading 域手写 Map 同模式，
        // VO 直接序列化是 camelCase 平铺——违背契约，已弃）
        List<Map<String, Object>> lists = new ArrayList<>();
        for (Bookshelf bookshelf : page.getRecords()) {
            Book book = books.get(bookshelf.getBookId());
            if (book == null) {
                continue; // 书目已被删除：书架行不展示（阅读记录仍保留在 reading 域）
            }
            Map<String, Object> bookInfo = new HashMap<>();
            bookInfo.put("id", book.getId());
            bookInfo.put("title", book.getTitle());
            bookInfo.put("author", book.getAuthor());
            bookInfo.put("cover", book.getCover());
            bookInfo.put("pages", book.getPages());
            bookInfo.put("category_id", book.getCategoryId());
            bookInfo.put("category_name", categoryNames.get(book.getCategoryId()));

            Map<String, Object> item = new HashMap<>();
            item.put("id", bookshelf.getId());
            item.put("book_id", bookshelf.getBookId());
            item.put("status", bookshelf.getStatus());
            item.put("current_page", bookshelf.getCurrentPage());
            item.put("progress", bookshelf.getProgress());
            item.put("pinned", bookshelf.getPinned());
            item.put("finish_time", bookshelf.getFinishTime());
            item.put("update_time", bookshelf.getUpdateTime());
            item.put("book", bookInfo);
            lists.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("lists", lists);
        result.put("count", page.getTotal());
        return AjaxResult.success(result);
    }

    @PostMapping("/add")
    @ApiOperation(value = "加入书架")
    public AjaxResult add(@RequestBody Map<String, Integer> params) {
        Integer bookId = params.get("book_id");
        if (bookId == null) {
            return AjaxResult.failed("书籍ID不能为空");
        }

        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            return AjaxResult.failed("书籍不存在");
        }

        Integer userId = LikeFrontThreadLocal.getUserId();

        QueryWrapper<Bookshelf> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("book_id", bookId);
        Bookshelf existing = bookshelfMapper.selectOne(wrapper);

        if (existing != null) {
            return AjaxResult.failed("该书籍已在书架中");
        }

        int now = (int) (System.currentTimeMillis() / 1000);
        Bookshelf bookshelf = new Bookshelf();
        bookshelf.setUserId(userId);
        bookshelf.setBookId(bookId);
        bookshelf.setStatus(0); // 想读
        bookshelf.setCurrentPage(0);
        bookshelf.setProgress(0);
        bookshelf.setPinned(0);
        bookshelf.setCreateTime(now);
        bookshelf.setUpdateTime(now);
        bookshelfMapper.insert(bookshelf);

        return AjaxResult.success();
    }

    /**
     * 更新进度（修正版：想读+页数>0 自动转在读；达到总页数不自动转已读——
     * 转已读必须前端确认弹层后走 /status 显式切换，02 §3.2）
     */
    @PostMapping("/progress")
    @ApiOperation(value = "更新进度")
    public AjaxResult updateProgress(@RequestBody Map<String, Integer> params) {
        Integer bookId = params.get("book_id");
        Integer currentPage = params.get("current_page");

        if (bookId == null || currentPage == null) {
            return AjaxResult.failed("参数错误");
        }

        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            return AjaxResult.failed("书籍不存在");
        }

        Integer userId = LikeFrontThreadLocal.getUserId();

        QueryWrapper<Bookshelf> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("book_id", bookId);
        Bookshelf bookshelf = bookshelfMapper.selectOne(wrapper);

        if (bookshelf == null) {
            return AjaxResult.failed("该书籍不在书架中");
        }

        // 页码夹取 [0, 总页数]（输入范围契约 02 §3.2：0~总页数）
        int pages = book.getPages() != null ? book.getPages() : 0;
        if (currentPage < 0) {
            currentPage = 0;
        }
        if (pages > 0 && currentPage > pages) {
            currentPage = pages;
        }

        int progress = 0;
        if (pages > 0) {
            progress = (int) ((currentPage * 100.0) / pages);
        }

        bookshelf.setCurrentPage(currentPage);
        bookshelf.setProgress(progress);
        // 想读→在读（「开始阅读」动线：current_page 从 0 起调本接口，02 §4.1）
        if (bookshelf.getStatus() == 0 && currentPage > 0) {
            bookshelf.setStatus(1);
        }
        bookshelf.setUpdateTime((int) (System.currentTimeMillis() / 1000));

        bookshelfMapper.updateById(bookshelf);

        Map<String, Object> result = new HashMap<>();
        result.put("progress", bookshelf.getProgress());
        result.put("status", bookshelf.getStatus());
        return AjaxResult.success(result);
    }

    @PostMapping("/status")
    @ApiOperation(value = "更新状态")
    public AjaxResult updateStatus(@RequestBody Map<String, Integer> params) {
        Integer bookId = params.get("book_id");
        Integer status = params.get("status");

        if (bookId == null || status == null) {
            return AjaxResult.failed("参数错误");
        }

        Integer userId = LikeFrontThreadLocal.getUserId();

        QueryWrapper<Bookshelf> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("book_id", bookId);
        Bookshelf bookshelf = bookshelfMapper.selectOne(wrapper);

        if (bookshelf == null) {
            return AjaxResult.failed("该书籍不在书架中");
        }

        bookshelf.setStatus(status);
        bookshelf.setUpdateTime((int) (System.currentTimeMillis() / 1000));

        if (status == 2) {
            bookshelf.setFinishTime((int) (System.currentTimeMillis() / 1000));
        } else {
            bookshelf.setFinishTime(null); // 重读/回退时清读完日
        }

        bookshelfMapper.updateById(bookshelf);

        return AjaxResult.success();
    }

    /**
     * S2 移出书架（物理删书架行；la_reading_record 阅读记录保留——记录是用户资产，02 §3.2）
     */
    @PostMapping("/delete")
    @ApiOperation(value = "移出书架")
    public AjaxResult delete(@RequestBody Map<String, Integer> params) {
        Integer bookId = params.get("book_id");
        if (bookId == null) {
            return AjaxResult.failed("书籍ID不能为空");
        }

        Integer userId = LikeFrontThreadLocal.getUserId();

        QueryWrapper<Bookshelf> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("book_id", bookId);
        int deleted = bookshelfMapper.delete(wrapper);

        if (deleted == 0) {
            return AjaxResult.failed("该书籍不在书架中");
        }
        return AjaxResult.success();
    }

    /**
     * S3 置顶切换（长按卡片置顶，02 §4.1；即时生效）
     */
    @PostMapping("/pin")
    @ApiOperation(value = "置顶/取消置顶")
    public AjaxResult pin(@RequestBody Map<String, Integer> params) {
        Integer bookId = params.get("book_id");
        Integer pinned = params.get("pinned");

        if (bookId == null || pinned == null || (pinned != 0 && pinned != 1)) {
            return AjaxResult.failed("参数错误");
        }

        Integer userId = LikeFrontThreadLocal.getUserId();

        QueryWrapper<Bookshelf> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("book_id", bookId);
        Bookshelf bookshelf = bookshelfMapper.selectOne(wrapper);

        if (bookshelf == null) {
            return AjaxResult.failed("该书籍不在书架中");
        }

        bookshelf.setPinned(pinned);
        bookshelf.setUpdateTime((int) (System.currentTimeMillis() / 1000));
        bookshelfMapper.updateById(bookshelf);

        return AjaxResult.success();
    }
}
