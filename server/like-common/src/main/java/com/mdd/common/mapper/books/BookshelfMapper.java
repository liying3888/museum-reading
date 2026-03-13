package com.mdd.common.mapper.books;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdd.common.entity.books.Bookshelf;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BookshelfMapper extends BaseMapper<Bookshelf> {
}
