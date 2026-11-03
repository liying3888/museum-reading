package com.mdd.front.service.reading;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.mdd.common.entity.books.ReadingRecord;
import com.mdd.common.mapper.books.ReadingRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 240 分钟自动结束（后端权威）——契约 §2.5（docs/S3/01，leader 2026-11-04 过闸）
 *
 * 口径（02 §0.3 round2/round3）：
 * - 扫描范围=running+paused 两态并集（end_time IS NULL），暂停忘结束同样到线结算不悬挂
 * - 240 分钟线按墙钟自 start 起算（暂停计入墙钟——暂停不能无限拖延自动结束）
 * - paused 会话净时长冻结口径=paused_at−start−paused_total（修正令），running=now−start−paused_total
 * - 与 R4 end 双通道幂等：UPDATE ... WHERE end_time IS NULL 行级条件
 */
@Slf4j
@Service
public class ReadingAutoCloseService {

    /** 240 分钟（秒） */
    private static final int AUTO_CLOSE_LIMIT = 240 * 60;

    @Resource
    private ReadingRecordMapper readingRecordMapper;

    /** 每 5 分钟扫描一次 */
    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    public void autoClose() {
        int now = (int) (System.currentTimeMillis() / 1000);
        int deadline = now - AUTO_CLOSE_LIMIT;

        QueryWrapper<ReadingRecord> wrapper = new QueryWrapper<>();
        wrapper.isNull("end_time").le("start_time", deadline);
        List<ReadingRecord> overdue = readingRecordMapper.selectList(wrapper);

        for (ReadingRecord record : overdue) {
            int pausedTotal = record.getPausedTotal() != null ? record.getPausedTotal() : 0;
            int pausedAt = record.getPausedAt() != null ? record.getPausedAt() : 0;
            // §1.1 口径：paused 冻结在暂停点；running 结算到当前时刻
            int duration = Math.max(0,
                    (pausedAt > 0 ? pausedAt : now) - record.getStartTime() - pausedTotal);

            UpdateWrapper<ReadingRecord> uw = new UpdateWrapper<>();
            uw.eq("id", record.getId())
              .isNull("end_time")   // 行级幂等：与 end 接口双通道竞争安全
              .set("end_time", now)
              .set("paused_at", 0)
              .set("paused_total", pausedTotal)
              .set("duration", duration)
              .set("auto_closed", 1);
            int rows = readingRecordMapper.update(null, uw);
            if (rows > 0) {
                log.info("阅读会话自动结束: recordId={} userId={} duration={}s paused={}",
                        record.getId(), record.getUserId(), duration, pausedAt > 0);
            }
        }
    }
}
