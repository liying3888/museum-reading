package com.mdd.common.entity.books;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户阅读配置（每日目标单一数据源，docs/S3/01 §1.3）
 * 不动底座 la_user 表（likeadmin 升级路径）；无记录=默认 30 分钟
 */
@Data
@TableName("la_user_reading_config")
public class UserReadingConfig implements Serializable {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer userId;
    /** 每日目标分钟数（10-240） */
    private Integer dailyGoal;
    private Integer createTime;
    private Integer updateTime;
}
