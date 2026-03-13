package com.mdd.common.entity.books;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("la_book")
public class Book implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Integer id;
    
    private String title;
    private String author;
    private String publisher;
    private String isbn;
    private String cover;
    private String intro;
    private Integer pages;
    private Double rating;
    private Integer ratingCount;
    private Integer categoryId;
    private Integer status;
    private Integer createTime;
    private Integer updateTime;
}
