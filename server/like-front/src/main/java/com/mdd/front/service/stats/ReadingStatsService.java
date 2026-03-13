package com.mdd.front.service.stats;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mdd.common.entity.books.ReadingRecord;
import com.mdd.common.entity.books.Bookshelf;
import com.mdd.common.mapper.books.ReadingRecordMapper;
import com.mdd.common.mapper.books.BookshelfMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class ReadingStatsService {

    @Resource
    private ReadingRecordMapper readingRecordMapper;
    
    @Resource
    private BookshelfMapper bookshelfMapper;

    /**
     * 获取今日统计
     */
    public Map<String, Object> getTodayStats(Integer userId) {
        // 今天0点的时间戳
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        int todayTimestamp = (int) todayStart.atZone(ZoneId.systemDefault()).toEpochSecond();
        
        // 查询今日阅读记录
        QueryWrapper<ReadingRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
               .ge("start_time", todayTimestamp);
        List<ReadingRecord> records = readingRecordMapper.selectList(wrapper);
        
        // 计算总时长（分钟）
        int totalMinutes = records.stream()
                .mapToInt(r -> r.getDuration() != null ? r.getDuration() : 0)
                .sum();
        
        // 统计阅读次数
        int readCount = records.size();
        
        // 统计阅读书籍数（去重）
        long bookCount = records.stream()
                .filter(r -> r.getBookId() != null)
                .map(ReadingRecord::getBookId)
                .distinct()
                .count();
        
        Map<String, Object> result = new HashMap<>();
        result.put("date", LocalDate.now().toString());
        result.put("duration_minutes", totalMinutes);
        result.put("duration_hours", Math.round(totalMinutes / 60.0 * 10) / 10.0);
        result.put("read_count", readCount);
        result.put("book_count", bookCount);
        
        return result;
    }

    /**
     * 获取本周统计
     */
    public Map<String, Object> getWeekStats(Integer userId) {
        // 本周一0点
        LocalDateTime weekStart = LocalDate.now()
                .atStartOfDay()
                .minusDays(LocalDate.now().getDayOfWeek().getValue() - 1);
        int weekTimestamp = (int) weekStart.atZone(ZoneId.systemDefault()).toEpochSecond();
        
        QueryWrapper<ReadingRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
               .ge("start_time", weekTimestamp);
        List<ReadingRecord> records = readingRecordMapper.selectList(wrapper);
        
        int totalMinutes = records.stream()
                .mapToInt(r -> r.getDuration() != null ? r.getDuration() : 0)
                .sum();
        
        // 按天分组统计
        Map<String, Integer> dailyStats = new TreeMap<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = LocalDate.now().minusDays(6 - i);
            dailyStats.put(date.toString(), 0);
        }
        
        for (ReadingRecord record : records) {
            LocalDate recordDate = LocalDateTime.ofEpochSecond(
                    record.getStartTime(), 0, ZoneId.systemDefault().getRules().getOffset(LocalDateTime.now())
            ).toLocalDate();
            String dateStr = recordDate.toString();
            if (dailyStats.containsKey(dateStr)) {
                dailyStats.put(dateStr, dailyStats.get(dateStr) + 
                        (record.getDuration() != null ? record.getDuration() : 0));
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("start_date", weekStart.toLocalDate().toString());
        result.put("end_date", LocalDate.now().toString());
        result.put("duration_minutes", totalMinutes);
        result.put("duration_hours", Math.round(totalMinutes / 60.0 * 10) / 10.0);
        result.put("daily_stats", dailyStats);
        
        return result;
    }

    /**
     * 获取本月统计
     */
    public Map<String, Object> getMonthStats(Integer userId) {
        // 本月1日0点
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        int monthTimestamp = (int) monthStart.atZone(ZoneId.systemDefault()).toEpochSecond();
        
        QueryWrapper<ReadingRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
               .ge("start_time", monthTimestamp);
        List<ReadingRecord> records = readingRecordMapper.selectList(wrapper);
        
        int totalMinutes = records.stream()
                .mapToInt(r -> r.getDuration() != null ? r.getDuration() : 0)
                .sum();
        
        int totalDays = records.stream()
                .map(r -> LocalDateTime.ofEpochSecond(
                        r.getStartTime(), 0, ZoneId.systemDefault().getRules().getOffset(LocalDateTime.now())
                ).toLocalDate())
                .distinct()
                .toArray().length;
        
        Map<String, Object> result = new HashMap<>();
        result.put("month", LocalDate.now().getYear() + "-" + LocalDate.now().getMonthValue());
        result.put("duration_minutes", totalMinutes);
        result.put("duration_hours", Math.round(totalMinutes / 60.0 * 10) / 10.0);
        result.put("read_days", totalDays);
        
        return result;
    }

    /**
     * 获取累计统计
     */
    public Map<String, Object> getTotalStats(Integer userId) {
        // 所有阅读记录
        QueryWrapper<ReadingRecord> recordWrapper = new QueryWrapper<>();
        recordWrapper.eq("user_id", userId);
        List<ReadingRecord> records = readingRecordMapper.selectList(recordWrapper);
        
        // 计算总时长
        int totalMinutes = records.stream()
                .mapToInt(r -> r.getDuration() != null ? r.getDuration() : 0)
                .sum();
        
        // 总阅读天数
        long totalDays = records.stream()
                .map(r -> LocalDateTime.ofEpochSecond(
                        r.getStartTime(), 0, ZoneId.systemDefault().getRules().getOffset(LocalDateTime.now())
                ).toLocalDate())
                .distinct()
                .count();
        
        // 书架统计
        QueryWrapper<Bookshelf> bookshelfWrapper = new QueryWrapper<>();
        bookshelfWrapper.eq("user_id", userId);
        List<Bookshelf> bookshelfList = bookshelfMapper.selectList(bookshelfWrapper);
        
        long totalBooks = bookshelfList.size();
        long readingBooks = bookshelfList.stream()
                .filter(b -> b.getStatus() != null && b.getStatus() == 1)
                .count();
        long finishedBooks = bookshelfList.stream()
                .filter(b -> b.getStatus() != null && b.getStatus() == 2)
                .count();
        
        // 连续打卡天数
        int consecutiveDays = calculateConsecutiveDays(userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("duration_minutes", totalMinutes);
        result.put("duration_hours", Math.round(totalMinutes / 60.0 * 10) / 10.0);
        result.put("read_days", totalDays);
        result.put("consecutive_days", consecutiveDays);
        result.put("total_books", totalBooks);
        result.put("reading_books", readingBooks);
        result.put("finished_books", finishedBooks);
        
        return result;
    }

    /**
     * 计算连续打卡天数
     */
    private int calculateConsecutiveDays(Integer userId) {
        // 获取所有阅读日期（按日期降序）
        QueryWrapper<ReadingRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
               .orderByDesc("start_time");
        List<ReadingRecord> records = readingRecordMapper.selectList(wrapper);
        
        if (records.isEmpty()) {
            return 0;
        }
        
        // 转换为日期集合
        Set<LocalDate> dates = new HashSet<>();
        for (ReadingRecord record : records) {
            LocalDate date = LocalDateTime.ofEpochSecond(
                    record.getStartTime(), 0, ZoneId.systemDefault().getRules().getOffset(LocalDateTime.now())
            ).toLocalDate();
            dates.add(date);
        }
        
        // 从今天开始往前数连续天数
        int consecutiveDays = 0;
        LocalDate checkDate = LocalDate.now();
        
        while (dates.contains(checkDate)) {
            consecutiveDays++;
            checkDate = checkDate.minusDays(1);
        }
        
        return consecutiveDays;
    }

    /**
     * 获取打卡记录（最近30天）
     */
    public List<String> getCheckinRecords(Integer userId) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(29);
        int startTimestamp = (int) startDate.atStartOfDay()
                .atZone(ZoneId.systemDefault()).toEpochSecond();
        
        QueryWrapper<ReadingRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
               .ge("start_time", startTimestamp);
        List<ReadingRecord> records = readingRecordMapper.selectList(wrapper);
        
        Set<String> dates = new HashSet<>();
        for (ReadingRecord record : records) {
            LocalDate date = LocalDateTime.ofEpochSecond(
                    record.getStartTime(), 0, ZoneId.systemDefault().getRules().getOffset(LocalDateTime.now())
            ).toLocalDate();
            dates.add(date.toString());
        }
        
        return new ArrayList<>(dates);
    }
}
