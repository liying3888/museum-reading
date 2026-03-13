package com.mdd.front.service.bookshelf;

import com.mdd.common.entity.books.Bookshelf;
import java.util.List;

public interface IBookshelfService {
    List<Bookshelf> list(Long userId, String status);
    void add(Long userId, Long bookId, String status);
    void remove(Long userId, Long bookId);
    void updateStatus(Long userId, Long bookId, String status);
}
