package com.mdd.admin.service.statistics.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mdd.admin.service.statistics.IStatisticsService;
import com.mdd.common.entity.books.Book;
import com.mdd.common.entity.books.Bookshelf;
import com.mdd.common.entity.books.ReadingRecord;
import com.mdd.common.mapper.books.BookMapper;
import com.mdd.common.mapper.books.BookshelfMapper;
import com.mdd.common.mapper.books.ReadingRecordMapper;
import com.mdd.common.mapper.user.UserMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Service
public class StatisticsServiceImpl implements IStatisticsService {

    @Resource
    private UserMapper userMapper;
    
    @Resource
    private BookMapper bookMapper;
    
    @Resource
    private BookshelfMapper bookshelfMapper;
    
    @Resource
    private ReadingRecordMapper readingRecordMapper;

    @Override
    public Map<String, Object> getOverview() {
        Map<String, Object> result = new HashMap<>();
        
        // 用户总数
        Long userCount = userMapper.selectCount(new QueryWrapper<>());
        result.put("userCount", userCount);
        
        // 书籍总数
        QueryWrapper<Book> bookQuery = new QueryWrapper<>();
        bookQuery.eq("status", 1);
        Long bookCount = bookMapper.selectCount(bookQuery);
        result.put("bookCount", bookCount);
        
        // 分类总数
        Long categoryCount = 0L;
        result.put("categoryCount", categoryCount);
        
        // 阅读记录总数
        Long readingCount = readingRecordMapper.selectCount(new QueryWrapper<>());
        result.put("readingCount", readingCount);
        
        // 书架书籍总数
        Long bookshelfCount = bookshelfMapper.selectCount(new QueryWrapper<>());
        result.put("bookshelfCount", bookshelfCount);
        
        return result;
    }

    @Override
    public Map<String, Object> getReadingStatistics(String startDate, String endDate) {
        Map<String, Object> result = new HashMap<>();
        
        QueryWrapper<ReadingRecord> queryWrapper = new QueryWrapper<>();
        
        // 如果提供了日期范围，添加过滤条件
        if (startDate != null && !startDate.isEmpty()) {
            queryWrapper.ge("create_time", startDate);
        }
        if (endDate != null && !endDate.isEmpty()) {
            queryWrapper.le("create_time", endDate);
        }
        
        // 获取阅读记录列表
        List<ReadingRecord> records = readingRecordMapper.selectList(queryWrapper);
        
        // 统计每日阅读时长
        Map<String, Integer> dailyDuration = new HashMap<>();
        for (ReadingRecord record : records) {
            if (record.getCreateTime() != null) {
                // 假设 createTime 是时间戳，转换为日期字符串
                String date = new java.text.SimpleDateFormat("yyyy-MM-dd")
                    .format(new java.util.Date(record.getCreateTime() * 1000L));
                dailyDuration.merge(date, record.getDuration(), Integer::sum);
            }
        }
        
        result.put("dailyDuration", dailyDuration);
        result.put("totalRecords", records.size());
        
        // 计算总阅读时长
        int totalDuration = records.stream()
            .mapToInt(r -> r.getDuration() != null ? r.getDuration() : 0)
            .sum();
        result.put("totalDuration", totalDuration);
        
        return result;
    }
}
