package com.mdd.common.entity.books;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("la_reading_record")
public class ReadingRecord implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Integer id;
    
    private Integer userId;
    private Integer bookId;
    private Integer startTime;
    private Integer endTime;
    private Integer duration;
    private String note;
    private Integer createTime;
}
