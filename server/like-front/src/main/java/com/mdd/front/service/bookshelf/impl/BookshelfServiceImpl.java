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
    public List<Bookshelf> list(Long userId, String status) {
        LambdaQueryWrapper<Bookshelf> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Bookshelf::getUserId, userId);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Bookshelf::getStatus, status);
        }
        return bookshelfMapper.selectList(wrapper);
    }
    
    @Override
    public void add(Long userId, Long bookId, String status) {
        Bookshelf bookshelf = new Bookshelf();
        bookshelf.setUserId(userId);
        bookshelf.setBookId(bookId);
        bookshelf.setStatus(status);
        bookshelfMapper.insert(bookshelf);
    }
    
    @Override
    public void remove(Long userId, Long bookId) {
        LambdaQueryWrapper<Bookshelf> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Bookshelf::getUserId, userId);
        wrapper.eq(Bookshelf::getBookId, bookId);
        bookshelfMapper.delete(wrapper);
    }
    
    @Override
    public void updateStatus(Long userId, Long bookId, String status) {
        LambdaQueryWrapper<Bookshelf> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Bookshelf::getUserId, userId);
        wrapper.eq(Bookshelf::getBookId, bookId);
        Bookshelf bookshelf = new Bookshelf();
        bookshelf.setStatus(status);
        bookshelfMapper.update(bookshelf, wrapper);
    }
}
