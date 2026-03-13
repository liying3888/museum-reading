package com.mdd.admin.controller.books;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mdd.admin.service.books.IBookCategoryService;
import com.mdd.common.core.AjaxResult;
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
@RequestMapping("adminapi/category")
@Api(tags = "分类管理")
public class CategoryController {

    @Resource
    private IBookCategoryService categoryService;

    @GetMapping("/list")
    @ApiOperation(value = "分类列表")
    public AjaxResult list(@RequestParam(required = false) String name) {
        QueryWrapper<BookCategory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1);
        if (StringUtils.isNotEmpty(name)) {
            queryWrapper.like("name", name);
        }
        queryWrapper.orderByAsc("sort").orderByDesc("id");
        
        List<BookCategory> list = categoryService.list(queryWrapper);
        
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
        category.setUpdateTime((int)(System.currentTimeMillis() / 1000));
        categoryService.updateById(category);
        return AjaxResult.success();
    }

    @PostMapping("/del")
    @ApiOperation(value = "删除分类")
    public AjaxResult delete(@RequestBody Map<String, Integer> params) {
        Integer id = params.get("id");
        BookCategory category = new BookCategory();
        category.setId(id);
        category.setStatus(0);
        category.setUpdateTime((int)(System.currentTimeMillis() / 1000));
        categoryService.updateById(category);
        return AjaxResult.success();
    }
}
