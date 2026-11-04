package com.mdd.admin.controller.books;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mdd.admin.service.books.IBookCategoryService;
import com.mdd.admin.service.books.IBookService;
import com.mdd.common.core.AjaxResult;
import com.mdd.common.entity.books.Book;
import com.mdd.common.entity.books.BookCategory;
import com.mdd.common.util.StringUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/category")
@Api(tags = "分类管理")
public class CategoryController {

    @Resource
    private IBookCategoryService categoryService;

    @Resource
    private IBookService bookService;

    @GetMapping("/list")
    @ApiOperation(value = "分类列表")
    public AjaxResult list(@RequestParam(required = false) String name,
                           @RequestParam(required = false) Integer status) {
        QueryWrapper<BookCategory> queryWrapper = new QueryWrapper<>();
        // status 不传=全部（admin 需见停用分类做启用操作）
        if (status != null) {
            queryWrapper.eq("status", status);
        }
        if (StringUtils.isNotEmpty(name)) {
            queryWrapper.like("name", name);
        }
        queryWrapper.orderByAsc("sort").orderByDesc("id");

        List<BookCategory> list = categoryService.list(queryWrapper);

        // 每分类书目计数（04 §4 分类列表列；含上下架全部书目）
        QueryWrapper<Book> cntWrapper = new QueryWrapper<>();
        cntWrapper.select("category_id", "count(*) as cnt").groupBy("category_id");
        Map<Integer, Integer> cntMap = new HashMap<>();
        for (Map<String, Object> row : bookService.listMaps(cntWrapper)) {
            cntMap.put(Integer.parseInt(String.valueOf(row.get("category_id"))),
                    Integer.parseInt(String.valueOf(row.get("cnt"))));
        }
        for (BookCategory cate : list) {
            cate.setBookCount(cntMap.getOrDefault(cate.getId(), 0));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("lists", list);
        result.put("count", list.size());
        return AjaxResult.success(result);
    }

    @GetMapping("/detail")
    @ApiOperation(value = "分类详情")
    public AjaxResult detail(@RequestParam Integer id) {
        BookCategory category = categoryService.getById(id);
        if (category == null) {
            return AjaxResult.failed("分类不存在");
        }
        return AjaxResult.success(category);
    }

    @PostMapping("/add")
    @ApiOperation(value = "添加分类")
    public AjaxResult add(@RequestBody BookCategory category) {
        category.setCreateTime((int)(System.currentTimeMillis() / 1000));
        category.setUpdateTime((int)(System.currentTimeMillis() / 1000));
        if (category.getSort() == null) {
            category.setSort(0);
        }
        if (category.getStatus() == null) {
            category.setStatus(1);
        }
        categoryService.save(category);
        return AjaxResult.success();
    }

    @PostMapping("/edit")
    @ApiOperation(value = "编辑分类")
    public AjaxResult edit(@RequestBody BookCategory category) {
        // 停用阻拦：分类下还有书目时不允许直接停用（04 §4 需先迁移）
        if (category.getStatus() != null && category.getStatus() == 0) {
            AjaxResult check = checkDisable(category.getId());
            if (check != null) {
                return check;
            }
        }
        category.setUpdateTime((int)(System.currentTimeMillis() / 1000));
        categoryService.updateById(category);
        return AjaxResult.success();
    }

    @PostMapping("/del")
    @ApiOperation(value = "删除分类")
    public AjaxResult delete(@RequestBody Map<String, Integer> params) {
        Integer id = params.get("id");
        AjaxResult check = checkDisable(id);
        if (check != null) {
            return check;
        }
        BookCategory category = new BookCategory();
        category.setId(id);
        category.setStatus(0);
        category.setUpdateTime((int)(System.currentTimeMillis() / 1000));
        categoryService.updateById(category);
        return AjaxResult.success();
    }

    /**
     * 停用前置校验：分类下还有书目时返回失败信息，否则返回 null 放行
     */
    private AjaxResult checkDisable(Integer id) {
        if (id == null) {
            return AjaxResult.failed("参数缺失");
        }
        Long cnt = bookService.count(new QueryWrapper<Book>().eq("category_id", id));
        if (cnt != null && cnt > 0) {
            return AjaxResult.failed("该分类下还有 " + cnt + " 本书，请先迁移书目");
        }
        return null;
    }
}
