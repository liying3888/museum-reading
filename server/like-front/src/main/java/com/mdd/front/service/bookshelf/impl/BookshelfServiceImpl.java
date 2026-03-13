package com.mdd.front.service.bookshelf.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mdd.common.entity.books.Bookshelf;
import com.mdd.common.mapper.books.BookshelfMapper;
import com.mdd.front.service.bookshelf.IBookshelfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class BookshelfServiceImpl implements IBookshelfService {
    
    @Autowired
    private BookshelfMapper bookshelfMapper;
    
    @Override
    public List<Bookshelf> list(Integer userId, Integer status) {
        LambdaQueryWrapper<Bookshelf> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Bookshelf::getUserId, userId);
        if (status != null) {
            wrapper.eq(Bookshelf::getStatus, status);
        }
        return bookshelfMapper.selectList(wrapper);
    }
    
    @Override
    public void add(Integer userId, Integer bookId, Integer status) {
        Bookshelf bookshelf = new Bookshelf();
        bookshelf.setUserId(userId);
        bookshelf.setBookId(bookId);
        bookshelf.setStatus(status);
        bookshelfMapper.insert(bookshelf);
    }
    
    @Override
    public void remove(Integer userId, Integer bookId) {
        LambdaQueryWrapper<Bookshelf> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Bookshelf::getUserId, userId);
        wrapper.eq(Bookshelf::getBookId, bookId);
        bookshelfMapper.delete(wrapper);
    }
    
    @Override
    public void updateStatus(Integer userId, Integer bookId, Integer status) {
        LambdaQueryWrapper<Bookshelf> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Bookshelf::getUserId, userId);
        wrapper.eq(Bookshelf::getBookId, bookId);
        Bookshelf bookshelf = new Bookshelf();
        bookshelf.setStatus(status);
        bookshelfMapper.update(bookshelf, wrapper);
    }
}
