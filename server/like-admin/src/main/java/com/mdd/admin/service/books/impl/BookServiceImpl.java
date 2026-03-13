package com.mdd.admin.service.books.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdd.admin.service.books.IBookService;
import com.mdd.common.entity.books.Book;
import com.mdd.common.mapper.books.BookMapper;
import org.springframework.stereotype.Service;

@Service
public class BookServiceImpl extends ServiceImpl<BookMapper, Book> implements IBookService {
}
