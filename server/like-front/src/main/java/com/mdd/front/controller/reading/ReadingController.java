package com.mdd.front.controller.reading;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.mdd.common.core.AjaxResult;
import com.mdd.common.entity.books.Book;
import com.mdd.common.entity.books.ReadingRecord;
import com.mdd.common.mapper.books.BookMapper;
import com.mdd.common.mapper.books.ReadingRecordMapper;
import com.mdd.front.LikeFrontThreadLocal;
import com.mdd.front.service.stats.ReadingStatsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;

/**
 * 阅读计时域+每日目标——契约 R1-R6/G1/G2（docs/S3/01 §2.3/§2.4，leader 2026-11-04 过闸）
 *
 * 会话语义（§1.1，含公式修正令）：
 *   running: end_time IS NULL AND paused_at=0   净时长=now−start−paused_total
 *   paused : end_time IS NULL AND paused_at>0   净时长=paused_at−start−paused_total（冻结，不随墙钟增长）
 *   finished: end_time IS NOT NULL              净时长=end−start−paused_total
 */
@RestController
@RequestMapping("api/front/reading")
@Api(tags = "阅读管理")
public class ReadingController {

    @Resource
    private ReadingRecordMapper readingRecordMapper;

    @Resource
    private BookMapper bookMapper;

    @Resource
    private ReadingStatsService readingStatsService;

    /**
     * R1 开始阅读（幂等版）：存在未结束会话（end_time IS NULL）→ 返回该会话 resumed=true，
     * 不新建不串页码（02 §5.4 悬置会话重入；异书重入由前端按 resumed+book_id 分支提示）
     */
    @PostMapping("/start")
    @ApiOperation(value = "开始计时（幂等）")
    public AjaxResult start(@RequestBody(required = false) Map<String, Integer> params) {
        Integer userId = LikeFrontThreadLocal.getUserId();
        Integer bookId = params != null ? params.get("book_id") : null;

        ReadingRecord open = selectOpenSession(userId);
        if (open != null) {
            return AjaxResult.success(toSessionResult(open, true));
        }

        int now = now();
        ReadingRecord record = new ReadingRecord();
        record.setUserId(userId);
        record.setBookId(bookId);
        record.setStartTime(now);
        record.setPausedAt(0);
        record.setPausedTotal(0);
        record.setAutoClosed(0);
        record.setDuration(0);
        record.setCreateTime(now);
        readingRecordMapper.insert(record);

        return AjaxResult.success(toSessionResult(record, false));
    }

    /** R2 暂停（真暂停：paused_at=now；已 paused 幂等忽略） */
    @PostMapping("/pause")
    @ApiOperation(value = "暂停计时")
    public AjaxResult pause(@RequestBody Map<String, Integer> params) {
        ReadingRecord record = ownOpenRecord(params.get("record_id"));
        if (record == null) {
            return failedOrOpen(params.get("record_id"));
        }
        if (record.getPausedAt() == null || record.getPausedAt() == 0) {
            UpdateWrapper<ReadingRecord> uw = new UpdateWrapper<>();
            uw.eq("id", record.getId())
              .isNull("end_time")
              .set("paused_at", now());
            readingRecordMapper.update(null, uw);
        }
        return AjaxResult.success();
    }

    /** R3 继续（paused_total += now−paused_at；paused_at 清零；running 幂等忽略） */
    @PostMapping("/resume")
    @ApiOperation(value = "继续计时")
    public AjaxResult resume(@RequestBody Map<String, Integer> params) {
        ReadingRecord record = ownOpenRecord(params.get("record_id"));
        if (record == null) {
            return failedOrOpen(params.get("record_id"));
        }
        if (record.getPausedAt() != null && record.getPausedAt() > 0) {
            int now = now();
            int added = now - record.getPausedAt();
            UpdateWrapper<ReadingRecord> uw = new UpdateWrapper<>();
            uw.eq("id", record.getId())
              .isNull("end_time")
              .set("paused_at", 0)
              .setSql("paused_total = paused_total + " + added);
            readingRecordMapper.update(null, uw);
        }
        return AjaxResult.success();
    }

    /**
     * R4 结束（真暂停结算+成果数据一次返全）
     * duration=end−start−paused_total（paused 结束时 pause 段先结算入 paused_total）；
     * 重复结束幂等返回既有结果（行级条件 end_time IS NULL 保双通道幂等——与 240 扫描竞争安全）
     */
    @PostMapping("/end")
    @ApiOperation(value = "结束计时")
    public AjaxResult end(@RequestBody Map<String, Integer> params) {
        Integer recordId = params.get("record_id");
        if (recordId == null) {
            return AjaxResult.failed("参数错误");
        }
        Integer userId = LikeFrontThreadLocal.getUserId();
        ReadingRecord record = readingRecordMapper.selectById(recordId);
        if (record == null) {
            return AjaxResult.failed("记录不存在");
        }
        if (!record.getUserId().equals(userId)) {
            return AjaxResult.failed("无权操作该记录");
        }

        int now = now();
        int pausedTotal = record.getPausedTotal() != null ? record.getPausedTotal() : 0;
        if (record.getEndTime() == null) {
            // 暂停中结束：先把 end−paused_at 段结算进 paused_total，再落结束
            if (record.getPausedAt() != null && record.getPausedAt() > 0) {
                pausedTotal += Math.max(0, now - record.getPausedAt());
            }
            int duration = Math.max(0, now - record.getStartTime() - pausedTotal);
            UpdateWrapper<ReadingRecord> uw = new UpdateWrapper<>();
            uw.eq("id", recordId)
              .isNull("end_time")   // 行级幂等：已被 240 扫描结算则跳过
              .set("end_time", now)
              .set("paused_at", 0)
              .set("paused_total", pausedTotal)
              .set("duration", duration);
            readingRecordMapper.update(null, uw);
            record = readingRecordMapper.selectById(recordId);
        }
        return AjaxResult.success(buildResultPayload(record));
    }

    /** R5 当前进行中会话（net_duration 服务端算好；paused 态冻结不随墙钟增长——修正令） */
    @GetMapping("/session")
    @ApiOperation(value = "当前会话")
    public AjaxResult session() {
        Integer userId = LikeFrontThreadLocal.getUserId();
        ReadingRecord open = selectOpenSession(userId);
        if (open == null) {
            return AjaxResult.success((Object) null); // 契约 R5：无会话 data=null（显式转型避 success(Integer/String) 歧义）
        }
        return AjaxResult.success(toSessionResult(open, true));
    }

    /** 阅读记录分页（记录管理列表：返 auto_closed/book_title/duration，02 §0.3 兜底③） */
    @GetMapping("/records")
    @ApiOperation(value = "阅读记录")
    public AjaxResult records(@RequestParam(name = "page_no", defaultValue = "1") Integer pageNo,
                              @RequestParam(name = "page_size", defaultValue = "20") Integer pageSize) {
        Integer userId = LikeFrontThreadLocal.getUserId();

        QueryWrapper<ReadingRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).orderByDesc("create_time");

        List<ReadingRecord> all = readingRecordMapper.selectList(wrapper);
        int from = Math.max(0, (pageNo - 1) * pageSize);
        int to = Math.min(all.size(), from + pageSize);
        List<ReadingRecord> page = from < all.size() ? all.subList(from, to) : Collections.emptyList();

        List<Map<String, Object>> lists = new ArrayList<>();
        for (ReadingRecord record : page) {
            Map<String, Object> item = new HashMap<>();
            item.put("record_id", record.getId());
            item.put("book_id", record.getBookId());
            item.put("book_title", bookTitle(record.getBookId()));
            item.put("start_time", record.getStartTime());
            item.put("end_time", record.getEndTime());
            item.put("duration", record.getDuration());
            item.put("auto_closed", record.getAutoClosed());
            lists.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("lists", lists);
        result.put("count", all.size());
        return AjaxResult.success(result);
    }

    /** R6 删除异常记录（二次确认后调；stats 均 SUM 实时聚合，今日/本周累计天然重算） */
    @PostMapping("/record/delete")
    @ApiOperation(value = "删除阅读记录")
    public AjaxResult recordDelete(@RequestBody Map<String, Integer> params) {
        Integer recordId = params.get("record_id");
        if (recordId == null) {
            return AjaxResult.failed("参数错误");
        }
        Integer userId = LikeFrontThreadLocal.getUserId();
        ReadingRecord record = readingRecordMapper.selectById(recordId);
        if (record == null) {
            return AjaxResult.failed("记录不存在");
        }
        if (!record.getUserId().equals(userId)) {
            return AjaxResult.failed("无权操作该记录");
        }
        readingRecordMapper.deleteById(recordId);
        return AjaxResult.success();
    }

    /** G1 每日目标读取（兜底 30） */
    @GetMapping("/goal")
    @ApiOperation(value = "每日目标读取")
    public AjaxResult goalGet() {
        Integer userId = LikeFrontThreadLocal.getUserId();
        Map<String, Object> result = new HashMap<>();
        result.put("daily_goal", readingStatsService.getDailyGoal(userId));
        return AjaxResult.success(result);
    }

    /** G2 每日目标设置（10-240） */
    @PostMapping("/goal")
    @ApiOperation(value = "每日目标设置")
    public AjaxResult goalSet(@RequestBody Map<String, Integer> params) {
        Integer minutes = params.get("minutes");
        if (minutes == null) {
            return AjaxResult.failed("参数错误");
        }
        try {
            readingStatsService.setDailyGoal(LikeFrontThreadLocal.getUserId(), minutes);
        } catch (IllegalArgumentException e) {
            return AjaxResult.failed(e.getMessage());
        }
        return AjaxResult.success();
    }

    // ---------- 辅助 ----------

    private static int now() {
        return (int) (System.currentTimeMillis() / 1000);
    }

    /** 查当前用户未结束会话（无则 null） */
    private ReadingRecord selectOpenSession(Integer userId) {
        QueryWrapper<ReadingRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).isNull("end_time").last("LIMIT 1");
        return readingRecordMapper.selectOne(wrapper);
    }

    /** 按 record_id 取本人未结束记录（结束态/他人记录返 null） */
    private ReadingRecord ownOpenRecord(Integer recordId) {
        if (recordId == null) {
            return null;
        }
        ReadingRecord record = readingRecordMapper.selectById(recordId);
        if (record == null || record.getEndTime() != null
                || !record.getUserId().equals(LikeFrontThreadLocal.getUserId())) {
            return null;
        }
        return record;
    }

    private AjaxResult failedOrOpen(Integer recordId) {
        if (recordId == null) {
            return AjaxResult.failed("参数错误");
        }
        ReadingRecord record = readingRecordMapper.selectById(recordId);
        if (record == null) {
            return AjaxResult.failed("记录不存在");
        }
        if (!record.getUserId().equals(LikeFrontThreadLocal.getUserId())) {
            return AjaxResult.failed("无权操作该记录");
        }
        return AjaxResult.failed("该记录已结束");
    }

    /** 会话出参（R1/R5 共用）：net_duration 按 §1.1 口径服务端算好 */
    private Map<String, Object> toSessionResult(ReadingRecord record, boolean resumed) {
        int start = record.getStartTime();
        int pausedTotal = record.getPausedTotal() != null ? record.getPausedTotal() : 0;
        int pausedAt = record.getPausedAt() != null ? record.getPausedAt() : 0;
        // running: now−start−paused_total；paused: paused_at−start−paused_total（冻结）
        int net = (pausedAt > 0 ? pausedAt : now()) - start - pausedTotal;

        Map<String, Object> result = new HashMap<>();
        result.put("record_id", record.getId());
        result.put("book_id", record.getBookId());
        result.put("book_title", bookTitle(record.getBookId()));
        result.put("start_time", start);
        result.put("paused_at", pausedAt);
        result.put("paused_total", pausedTotal);
        result.put("net_duration", Math.max(0, net));
        result.put("resumed", resumed);
        return result;
    }

    /** R4 成果数据：duration/today_total/streak_days/goal/goal_gap/goal_achieved/auto_closed */
    private Map<String, Object> buildResultPayload(ReadingRecord record) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("duration", record.getDuration());
        payload.put("auto_closed", record.getAutoClosed() != null ? record.getAutoClosed() : 0);

        Map<String, Object> today = readingStatsService.getTodayStats(record.getUserId());
        int todaySeconds = (int) today.getOrDefault("duration_seconds", 0);
        int goalMinutes = readingStatsService.getDailyGoal(record.getUserId());
        int goalGapSeconds = goalMinutes * 60 - todaySeconds;

        payload.put("today_total", todaySeconds);
        payload.put("streak_days", readingStatsService.calculateConsecutiveDays(record.getUserId()));
        payload.put("goal", goalMinutes);
        payload.put("goal_gap", Math.max(0, goalGapSeconds));
        payload.put("goal_achieved", goalGapSeconds <= 0);
        return payload;
    }

    private String bookTitle(Integer bookId) {
        if (bookId == null) {
            return null; // 自由阅读（无 bookId），前端显示「自由阅读」
        }
        Book book = bookMapper.selectById(bookId);
        return book != null ? book.getTitle() : null;
    }
}
