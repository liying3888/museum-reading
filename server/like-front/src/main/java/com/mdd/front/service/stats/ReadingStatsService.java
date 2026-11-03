package com.mdd.front.service.stats;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mdd.common.entity.books.Bookshelf;
import com.mdd.common.entity.books.ReadingRecord;
import com.mdd.common.entity.books.UserReadingConfig;
import com.mdd.common.mapper.books.BookshelfMapper;
import com.mdd.common.mapper.books.ReadingRecordMapper;
import com.mdd.common.mapper.books.UserReadingConfigMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 阅读统计服务（S3 批 2 改造版）
 * 三处口径修正（docs/S3/01 §0/§2.4）：
 * 1. 时区显式 ZoneId.of("Asia/Shanghai")——修 systemDefault 在容器 JVM=UTC 下的串日
 *    （jnpf 时区三问同源条款；236 compose 虽设 TZ，代码不依赖环境兜底）
 * 2. 记录归属日=end_time 所在日（02 §0.3：跨天会话取结束时刻所在日）
 * 3. 连续打卡=截至最近阅读日的连续段（今日未读不归零，修展示 bug）
 */
@Service
public class ReadingStatsService {

    /** 统计/打卡日界固定东八区 */
    public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    @Resource
    private ReadingRecordMapper readingRecordMapper;

    @Resource
    private BookshelfMapper bookshelfMapper;

    @Resource
    private UserReadingConfigMapper userReadingConfigMapper;

    /**
     * 今日统计（end_time 归属日）
     */
    public Map<String, Object> getTodayStats(Integer userId) {
        LocalDate today = LocalDate.now(ZONE);
        int todayStart = (int) today.atStartOfDay(ZONE).toEpochSecond();
        int tomorrowStart = (int) today.plusDays(1).atStartOfDay(ZONE).toEpochSecond();

        // duration_minutes 单位=分钟（沿用存量出参键名）；duration 列存秒
        int totalSeconds = sumDuration(userId, todayStart, tomorrowStart);
        int totalMinutes = totalSeconds / 60;

        QueryWrapper<ReadingRecord> wrapper = finishedWrapper(userId);
        wrapper.ge("end_time", todayStart).lt("end_time", tomorrowStart);
        List<ReadingRecord> records = readingRecordMapper.selectList(wrapper);

        long bookCount = records.stream()
                .filter(r -> r.getBookId() != null)
                .map(ReadingRecord::getBookId)
                .distinct()
                .count();

        Map<String, Object> result = new HashMap<>();
        result.put("date", today.toString());
        result.put("duration_minutes", totalMinutes);
        result.put("duration_seconds", totalSeconds);
        result.put("read_count", records.size());
        result.put("book_count", bookCount);
        return result;
    }

    /**
     * 本周统计（周一起；按 end_time 归属日聚合）
     */
    public Map<String, Object> getWeekStats(Integer userId) {
        LocalDate today = LocalDate.now(ZONE);
        LocalDate weekStartDay = today.minusDays(today.getDayOfWeek().getValue() - 1);
        int weekStart = (int) weekStartDay.atStartOfDay(ZONE).toEpochSecond();
        int tomorrowStart = (int) today.plusDays(1).atStartOfDay(ZONE).toEpochSecond();

        int totalSeconds = sumDuration(userId, weekStart, tomorrowStart);

        QueryWrapper<ReadingRecord> wrapper = finishedWrapper(userId);
        wrapper.ge("end_time", weekStart).lt("end_time", tomorrowStart);
        List<ReadingRecord> records = readingRecordMapper.selectList(wrapper);

        // 一~日 7 天序列（02 §6.1 柱图数据基座）
        Map<String, Integer> dailyStats = new TreeMap<>();
        for (int i = 0; i < 7; i++) {
            dailyStats.put(weekStartDay.plusDays(i).toString(), 0);
        }
        for (ReadingRecord record : records) {
            String dateStr = toZoneDate(record.getEndTime());
            Integer seconds = record.getDuration() != null ? record.getDuration() : 0;
            dailyStats.merge(dateStr, seconds, Integer::sum);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("start_date", weekStartDay.toString());
        result.put("end_date", today.toString());
        result.put("duration_minutes", totalSeconds / 60);
        result.put("duration_seconds", totalSeconds);
        result.put("daily_stats", dailyStats);
        return result;
    }

    /**
     * 本月统计
     */
    public Map<String, Object> getMonthStats(Integer userId) {
        LocalDate today = LocalDate.now(ZONE);
        int monthStart = (int) today.withDayOfMonth(1).atStartOfDay(ZONE).toEpochSecond();
        int tomorrowStart = (int) today.plusDays(1).atStartOfDay(ZONE).toEpochSecond();

        int totalSeconds = sumDuration(userId, monthStart, tomorrowStart);

        QueryWrapper<ReadingRecord> wrapper = finishedWrapper(userId);
        wrapper.ge("end_time", monthStart).lt("end_time", tomorrowStart);
        List<ReadingRecord> records = readingRecordMapper.selectList(wrapper);

        long readDays = records.stream()
                .map(r -> toZoneDate(r.getEndTime()))
                .distinct()
                .count();

        Map<String, Object> result = new HashMap<>();
        result.put("month", today.format(MONTH_FMT));
        result.put("duration_minutes", totalSeconds / 60);
        result.put("duration_seconds", totalSeconds);
        result.put("read_days", readDays);
        return result;
    }

    /**
     * 累计统计
     */
    public Map<String, Object> getTotalStats(Integer userId) {
        QueryWrapper<ReadingRecord> recordWrapper = finishedWrapper(userId);
        List<ReadingRecord> records = readingRecordMapper.selectList(recordWrapper);

        int totalSeconds = records.stream()
                .mapToInt(r -> r.getDuration() != null ? r.getDuration() : 0)
                .sum();

        long totalDays = records.stream()
                .map(r -> toZoneDate(r.getEndTime()))
                .distinct()
                .count();

        QueryWrapper<Bookshelf> bookshelfWrapper = new QueryWrapper<>();
        bookshelfWrapper.eq("user_id", userId);
        List<Bookshelf> bookshelfList = bookshelfMapper.selectList(bookshelfWrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("duration_minutes", totalSeconds / 60);
        result.put("duration_seconds", totalSeconds);
        result.put("read_days", totalDays);
        result.put("consecutive_days", calculateConsecutiveDays(userId));
        result.put("total_books", bookshelfList.size());
        result.put("reading_books", bookshelfList.stream().filter(b -> b.getStatus() != null && b.getStatus() == 1).count());
        result.put("finished_books", bookshelfList.stream().filter(b -> b.getStatus() != null && b.getStatus() == 2).count());
        return result;
    }

    /**
     * 连续打卡天数（修正版）：截至最近阅读日的连续段——今日已读从今日倒数；
     * 今日未读从昨日倒数（不归零，06 示样「连续 3 天」在次日未读时仍显示 3）
     */
    public int calculateConsecutiveDays(Integer userId) {
        Set<LocalDate> dates = readDates(userId);
        if (dates.isEmpty()) {
            return 0;
        }
        LocalDate checkDate = LocalDate.now(ZONE);
        if (!dates.contains(checkDate)) {
            checkDate = checkDate.minusDays(1);
        }
        int consecutive = 0;
        while (dates.contains(checkDate)) {
            consecutive++;
            checkDate = checkDate.minusDays(1);
        }
        return consecutive;
    }

    /**
     * T1 打卡记录（月参数化）：month='yyyy-MM' 缺省当月；
     * 返回该月全部已读日（end_time 归属日，东八区）
     */
    public List<String> getCheckinRecords(Integer userId, String month) {
        YearMonth ym;
        if (month != null && !month.isEmpty()) {
            try {
                ym = YearMonth.parse(month, MONTH_FMT);
            } catch (Exception e) {
                ym = YearMonth.now(ZONE);
            }
        } else {
            ym = YearMonth.now(ZONE);
        }
        int monthStart = (int) ym.atDay(1).atStartOfDay(ZONE).toEpochSecond();
        int nextMonthStart = (int) ym.plusMonths(1).atDay(1).atStartOfDay(ZONE).toEpochSecond();

        QueryWrapper<ReadingRecord> wrapper = finishedWrapper(userId);
        wrapper.ge("end_time", monthStart).lt("end_time", nextMonthStart);
        List<ReadingRecord> records = readingRecordMapper.selectList(wrapper);

        Set<String> dates = new TreeSet<>();
        for (ReadingRecord record : records) {
            dates.add(toZoneDate(record.getEndTime()));
        }
        return new ArrayList<>(dates);
    }

    /**
     * G1 每日目标读取（无配置行兜底 30，docs/S3/01 §2.4）
     */
    public int getDailyGoal(Integer userId) {
        QueryWrapper<UserReadingConfig> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        UserReadingConfig config = userReadingConfigMapper.selectOne(wrapper);
        return config != null && config.getDailyGoal() != null ? config.getDailyGoal() : 30;
    }

    /**
     * G2 每日目标设置（10-240 分钟校验+upsert）
     */
    public void setDailyGoal(Integer userId, int minutes) {
        if (minutes < 10 || minutes > 240) {
            throw new IllegalArgumentException("目标需在 10-240 分钟之间");
        }
        int now = (int) (System.currentTimeMillis() / 1000);
        QueryWrapper<UserReadingConfig> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        UserReadingConfig config = userReadingConfigMapper.selectOne(wrapper);
        if (config == null) {
            config = new UserReadingConfig();
            config.setUserId(userId);
            config.setDailyGoal(minutes);
            config.setCreateTime(now);
            config.setUpdateTime(now);
            userReadingConfigMapper.insert(config);
        } else {
            config.setDailyGoal(minutes);
            config.setUpdateTime(now);
            userReadingConfigMapper.updateById(config);
        }
    }

    /** 已结束记录查询底座（进行中会话不计入统计——时长未定，前端叠 R5 net_duration 展示） */
    private QueryWrapper<ReadingRecord> finishedWrapper(Integer userId) {
        QueryWrapper<ReadingRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).isNotNull("end_time");
        return wrapper;
    }

    private int sumDuration(Integer userId, int fromTs, int toTs) {
        QueryWrapper<ReadingRecord> wrapper = finishedWrapper(userId);
        wrapper.ge("end_time", fromTs).lt("end_time", toTs);
        List<ReadingRecord> records = readingRecordMapper.selectList(wrapper);
        return records.stream().mapToInt(r -> r.getDuration() != null ? r.getDuration() : 0).sum();
    }

    private Set<LocalDate> readDates(Integer userId) {
        QueryWrapper<ReadingRecord> wrapper = finishedWrapper(userId);
        List<ReadingRecord> records = readingRecordMapper.selectList(wrapper);
        Set<LocalDate> dates = new HashSet<>();
        for (ReadingRecord record : records) {
            dates.add(LocalDate.parse(toZoneDate(record.getEndTime())));
        }
        return dates;
    }

    private String toZoneDate(Integer epochSecond) {
        return LocalDateTime.ofInstant(
                java.time.Instant.ofEpochSecond(epochSecond), ZONE).toLocalDate().toString();
    }
}
