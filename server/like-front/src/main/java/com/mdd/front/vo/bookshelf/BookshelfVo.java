package com.mdd.front.vo.bookshelf;

import lombok.Data;

@Data
public class BookshelfVo {
    private Long id;
    private Long bookId;
    private String bookTitle;
    private String bookAuthor;
    private String bookCover;
    private String status;
}
