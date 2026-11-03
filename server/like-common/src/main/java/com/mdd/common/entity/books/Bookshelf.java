package com.mdd.common.entity.books;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("la_bookshelf")
public class Bookshelf implements Serializable {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer userId;
    private Integer bookId;
    private Integer status;
    private Integer currentPage;
    private Integer progress;
    /** 置顶: 0=否 1=是（长按置顶，02 §4.1） */
    private Integer pinned;
    private Integer startTime;
    private Integer finishTime;
    private Integer createTime;
    private Integer updateTime;
}
