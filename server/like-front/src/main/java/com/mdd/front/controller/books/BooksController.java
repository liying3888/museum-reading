package com.mdd.front.controller.books;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdd.common.aop.NotLogin;
import com.mdd.common.core.AjaxResult;
import com.mdd.common.entity.books.Book;
import com.mdd.common.entity.books.BookCategory;
import com.mdd.common.mapper.books.BookCategoryMapper;
import com.mdd.common.mapper.books.BookMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;

/**
 * 书籍域（公开）——契约 B1/B2（docs/S3/01 §2.1，leader 2026-11-04 过闸）
 * 书单公开可看（02 §1.3：未登录首页书单正常展示），仅上架 status=1 可见
 */
@RestController
@RequestMapping("api/front/books")
@Api(tags = "书籍浏览")
public class BooksController {

    @Resource
    private BookMapper bookMapper;

    @Resource
    private BookCategoryMapper bookCategoryMapper;

    @NotLogin
    @GetMapping("/list")
    @ApiOperation(value = "B1 书籍列表")
    public AjaxResult list(@RequestParam(name = "page_no", defaultValue = "1") Integer pageNo,
                           @RequestParam(name = "page_size", defaultValue = "15") Integer pageSize,
                           @RequestParam(name = "category_id", required = false) Integer categoryId,
                           @RequestParam(required = false) String keyword) {
        QueryWrapper<Book> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);
        if (categoryId != null) {
            wrapper.eq("category_id", categoryId);
        }
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(w -> w.like("title", keyword).or().like("author", keyword));
        }
        // 排序口径：上架时间倒序，同日 tie-break=录入序 id 升序（06 §5 round7 补正，
        // 原 id DESC 的同日序与示样「人类简史先于活着」相反；§6 决策点④，运营置顶留迭代）
        wrapper.orderByDesc("create_time").orderByAsc("id");

        IPage<Book> page = bookMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);

        Map<Integer, String> categoryNames = getCategoryNames(page.getRecords());

        List<Map<String, Object>> lists = new ArrayList<>();
        for (Book book : page.getRecords()) {
            lists.add(toSummary(book, categoryNames));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("count", page.getTotal());
        result.put("lists", lists);
        // 聚合 categories 键（§6 决策点②：书库 chips 数据源一次带出，省一次请求）
        if (pageNo == 1) {
            List<Map<String, Object>> categories = new ArrayList<>();
            for (BookCategory cate : bookCategoryMapper.selectList(
                    new QueryWrapper<BookCategory>().eq("status", 1).orderByAsc("id"))) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", cate.getId());
                item.put("name", cate.getName());
                categories.add(item);
            }
            result.put("categories", categories);
        }
        return AjaxResult.success(result);
    }

    @NotLogin
    @GetMapping("/detail")
    @ApiOperation(value = "B2 书籍详情")
    public AjaxResult detail(@RequestParam Integer id) {
        Book book = bookMapper.selectById(id);
        if (book == null) {
            return AjaxResult.failed("书籍不存在");
        }
        Map<String, Object> result = toSummary(book, getCategoryNames(
                Collections.singletonList(book)));
        result.put("intro", book.getIntro());
        result.put("isbn", book.getIsbn());
        // 公开接口不暴露 status/时间等管理字段（详情页不区分上下架展示，列表已过滤）
        return AjaxResult.success(result);
    }

    private Map<Integer, String> getCategoryNames(List<Book> books) {
        Set<Integer> ids = new HashSet<>();
        for (Book book : books) {
            if (book.getCategoryId() != null) {
                ids.add(book.getCategoryId());
            }
        }
        Map<Integer, String> names = new HashMap<>();
        if (!ids.isEmpty()) {
            for (BookCategory cate : bookCategoryMapper.selectBatchIds(ids)) {
                names.put(cate.getId(), cate.getName());
            }
        }
        return names;
    }

    private Map<String, Object> toSummary(Book book, Map<Integer, String> categoryNames) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", book.getId());
        item.put("title", book.getTitle());
        item.put("author", book.getAuthor());
        item.put("publisher", book.getPublisher());
        item.put("cover", book.getCover());
        item.put("pages", book.getPages());
        item.put("rating", book.getRating());
        item.put("category_id", book.getCategoryId());
        item.put("category_name", categoryNames.get(book.getCategoryId()));
        return item;
    }
}
