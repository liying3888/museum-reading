package com.mdd.common.mapper.books;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdd.common.entity.books.Book;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BookMapper extends BaseMapper<Book> {
}
