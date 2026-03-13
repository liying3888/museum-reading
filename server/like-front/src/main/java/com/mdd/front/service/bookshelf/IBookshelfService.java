package com.mdd.front.service.bookshelf;

import com.mdd.common.entity.books.Bookshelf;
import java.util.List;

public interface IBookshelfService {
    List<Bookshelf> list(Integer userId, Integer status);
    void add(Integer userId, Integer bookId, Integer status);
    void remove(Integer userId, Integer bookId);
    void updateStatus(Integer userId, Integer bookId, Integer status);
}
