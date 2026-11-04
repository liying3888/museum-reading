package com.mdd.common.entity.books;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("la_book_category")
public class BookCategory implements Serializable {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String name;
    private String icon;
    private Integer sort;
    private Integer status;
    private Integer createTime;
    private Integer updateTime;

    /** 书目计数（admin 列表聚合列，非表字段） */
    @TableField(exist = false)
    private Integer bookCount;
}
