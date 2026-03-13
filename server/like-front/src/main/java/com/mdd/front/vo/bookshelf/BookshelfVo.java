package com.mdd.front.vo.bookshelf;

import lombok.Data;

@Data
public class BookshelfVo {
    private Integer id;
    private Integer bookId;
    private String bookTitle;
    private String bookAuthor;
    private String bookCover;
    private Integer pages;
    private String status;
}
