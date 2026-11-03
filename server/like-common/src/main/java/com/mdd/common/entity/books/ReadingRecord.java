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
    /** 当前暂停起点时间戳（0=非暂停态）；会话语义见 docs/S3/01 §1.1 */
    private Integer pausedAt;
    /** 已累计暂停秒数（净时长=Σ运行段，勿以此列当运行时长） */
    private Integer pausedTotal;
    /** 0=正常结束；1=240分钟自动结束 */
    private Integer autoClosed;
    private Integer duration;
    private String note;
    private Integer createTime;
}
