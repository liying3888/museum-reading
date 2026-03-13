package com.mdd.front.controller.bookshelf;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdd.common.aop.NotLogin;
import com.mdd.common.core.AjaxResult;
import com.mdd.common.entity.books.Book;
import com.mdd.common.entity.books.Bookshelf;
import com.mdd.common.mapper.books.BookMapper;
import com.mdd.common.mapper.books.BookshelfMapper;
import com.mdd.front.service.bookshelf.IBookshelfService;
import com.mdd.front.vo.bookshelf.BookshelfVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;

@RestController
@RequestMapping("api/front/bookshelf")
@Api(tags = "书架管理")
public class BookshelfController {

    @Resource
    private IBookshelfService bookshelfService;
    
    @Resource
    private BookshelfMapper bookshelfMapper;
    
    @Resource
    private BookMapper bookMapper;

    @GetMapping("/list")
    @ApiOperation(value = "我的书架")
    public AjaxResult list(@RequestParam(defaultValue = "1") Integer pageNo,
                          @RequestParam(defaultValue = "15") Integer pageSize,
                          @RequestParam(required = false) Integer status) {
        // TODO: 从token获取用户ID，这里先用1测试
        Integer userId = 1;
        
        QueryWrapper<Bookshelf> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        if (status != null) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("update_time");
        
        IPage<Bookshelf> page = bookshelfMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
        
        // 转换为VO，包含书籍信息
        List<BookshelfVo> voList = new ArrayList<>();
        for (Bookshelf bookshelf : page.getRecords()) {
            Book book = bookMapper.selectById(bookshelf.getBookId());
            if (book != null) {
                BookshelfVo vo = new BookshelfVo();
                BeanUtils.copyProperties(bookshelf, vo);
                vo.setBookId(book.getId());
                vo.setTitle(book.getTitle());
                vo.setAuthor(book.getAuthor());
                vo.setCover(book.getCover());
                vo.setPages(book.getPages());
                voList.add(vo);
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("lists", voList);
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
        
        // 检查书籍是否存在
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            return AjaxResult.failed("书籍不存在");
        }
        
        // TODO: 从token获取用户ID
        Integer userId = 1;
        
        // 检查是否已在书架
        QueryWrapper<Bookshelf> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("book_id", bookId);
        Bookshelf existing = bookshelfMapper.selectOne(wrapper);
        
        if (existing != null) {
            return AjaxResult.failed("该书籍已在书架中");
        }
        
        // 加入书架
        Bookshelf bookshelf = new Bookshelf();
        bookshelf.setUserId(userId);
        bookshelf.setBookId(bookId);
        bookshelf.setStatus(0); // 想读
        bookshelf.setCurrentPage(0);
        bookshelf.setProgress(0);
        bookshelf.setCreateTime((int)(System.currentTimeMillis() / 1000));
        bookshelf.setUpdateTime((int)(System.currentTimeMillis() / 1000));
        bookshelfMapper.insert(bookshelf);
        
        return AjaxResult.success();
    }

    @PostMapping("/progress")
    @ApiOperation(value = "更新进度")
    public AjaxResult updateProgress(@RequestBody Map<String, Integer> params) {
        Integer bookId = params.get("book_id");
        Integer currentPage = params.get("current_page");
        
        if (bookId == null || currentPage == null) {
            return AjaxResult.failed("参数错误");
        }
        
        // 获取书籍总页数
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            return AjaxResult.failed("书籍不存在");
        }
        
        // TODO: 从token获取用户ID
        Integer userId = 1;
        
        // 更新书架记录
        QueryWrapper<Bookshelf> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("book_id", bookId);
        Bookshelf bookshelf = bookshelfMapper.selectOne(wrapper);
        
        if (bookshelf == null) {
            return AjaxResult.failed("该书籍不在书架中");
        }
        
        // 计算进度
        int progress = 0;
        if (book.getPages() != null && book.getPages() > 0) {
            progress = (int)((currentPage * 100.0) / book.getPages());
        }
        
        bookshelf.setCurrentPage(currentPage);
        bookshelf.setProgress(progress);
        bookshelf.setStatus(1); // 在读
        bookshelf.setUpdateTime((int)(System.currentTimeMillis() / 1000));
        
        // 如果读完了
        if (progress >= 100) {
            bookshelf.setStatus(2); // 已读
            bookshelf.setProgress(100);
            bookshelf.setFinishTime((int)(System.currentTimeMillis() / 1000));
        }
        
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
        
        // TODO: 从token获取用户ID
        Integer userId = 1;
        
        QueryWrapper<Bookshelf> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("book_id", bookId);
        Bookshelf bookshelf = bookshelfMapper.selectOne(wrapper);
        
        if (bookshelf == null) {
            return AjaxResult.failed("该书籍不在书架中");
        }
        
        bookshelf.setStatus(status);
        bookshelf.setUpdateTime((int)(System.currentTimeMillis() / 1000));
        
        if (status == 2) {
            bookshelf.setFinishTime((int)(System.currentTimeMillis() / 1000));
        }
        
        bookshelfMapper.updateById(bookshelf);
        
        return AjaxResult.success();
    }
}
