package com.mdd.admin.service.books.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdd.admin.service.books.IBookCategoryService;
import com.mdd.common.entity.books.BookCategory;
import com.mdd.common.mapper.books.BookCategoryMapper;
import org.springframework.stereotype.Service;

@Service
public class BookCategoryServiceImpl extends ServiceImpl<BookCategoryMapper, BookCategory> implements IBookCategoryService {
}
