package com.mdd.admin.controller.books;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdd.admin.service.books.IBookService;
import com.mdd.common.core.AjaxResult;
import com.mdd.common.entity.books.Book;
import com.mdd.common.util.StringUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("api/admin/books")
@Api(tags = "书籍管理")
public class BookController {

    @Resource
    private IBookService bookService;

    @GetMapping("/list")
    @ApiOperation(value = "书籍列表")
    public AjaxResult list(@RequestParam(defaultValue = "1") Integer pageNo,
                          @RequestParam(defaultValue = "15") Integer pageSize,
                          @RequestParam(required = false) String title,
                          @RequestParam(required = false) Integer categoryId) {
        QueryWrapper<Book> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1);
        if (StringUtils.isNotEmpty(title)) {
            queryWrapper.like("title", title);
        }
        if (categoryId != null) {
            queryWrapper.eq("category_id", categoryId);
        }
        queryWrapper.orderByDesc("id");
        
        IPage<Book> page = bookService.page(new Page<>(pageNo, pageSize), queryWrapper);
        
        Map<String, Object> result = new HashMap<>();
        result.put("lists", page.getRecords());
        result.put("count", page.getTotal());
        return AjaxResult.success(result);
    }

    @GetMapping("/detail")
    @ApiOperation(value = "书籍详情")
    public AjaxResult detail(@RequestParam Integer id) {
        Book book = bookService.getById(id);
        if (book == null) {
            return AjaxResult.failed("书籍不存在");
        }
        return AjaxResult.success(book);
    }

    @PostMapping("/add")
    @ApiOperation(value = "添加书籍")
    public AjaxResult add(@RequestBody Book book) {
        book.setCreateTime((int)(System.currentTimeMillis() / 1000));
        book.setUpdateTime((int)(System.currentTimeMillis() / 1000));
        bookService.save(book);
        return AjaxResult.success();
    }

    @PostMapping("/edit")
    @ApiOperation(value = "编辑书籍")
    public AjaxResult edit(@RequestBody Book book) {
        book.setUpdateTime((int)(System.currentTimeMillis() / 1000));
        bookService.updateById(book);
        return AjaxResult.success();
    }

    @PostMapping("/delete")
    @ApiOperation(value = "删除书籍")
    public AjaxResult delete(@RequestBody Map<String, Integer> params) {
        Integer id = params.get("id");
        Book book = new Book();
        book.setId(id);
        book.setStatus(0);
        book.setUpdateTime((int)(System.currentTimeMillis() / 1000));
        bookService.updateById(book);
        return AjaxResult.success();
    }
}
