package com.mdd.front.controller.reading;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mdd.common.core.AjaxResult;
import com.mdd.common.entity.books.ReadingRecord;
import com.mdd.common.mapper.books.ReadingRecordMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;

@RestController
@RequestMapping("api/front/reading")
@Api(tags = "阅读管理")
public class ReadingController {

    @Resource
    private ReadingRecordMapper readingRecordMapper;

    @PostMapping("/start")
    @ApiOperation(value = "开始计时")
    public AjaxResult start(@RequestBody(required = false) Map<String, Integer> params) {
        // TODO: 从token获取用户ID
        Integer userId = 1;
        
        Integer bookId = params != null ? params.get("book_id") : null;
        
        ReadingRecord record = new ReadingRecord();
        record.setUserId(userId);
        record.setBookId(bookId);
        record.setStartTime((int)(System.currentTimeMillis() / 1000));
        record.setCreateTime((int)(System.currentTimeMillis() / 1000));
        record.setDuration(0);
        
        readingRecordMapper.insert(record);
        
        Map<String, Object> result = new HashMap<>();
        result.put("record_id", record.getId());
        result.put("start_time", record.getStartTime());
        return AjaxResult.success(result);
    }

    @PostMapping("/end")
    @ApiOperation(value = "结束计时")
    public AjaxResult end(@RequestBody Map<String, Integer> params) {
        Integer recordId = params.get("record_id");
        
        if (recordId == null) {
            return AjaxResult.failed("参数错误");
        }
        
        ReadingRecord record = readingRecordMapper.selectById(recordId);
        if (record == null) {
            return AjaxResult.failed("记录不存在");
        }
        
        int endTime = (int)(System.currentTimeMillis() / 1000);
        int duration = endTime - record.getStartTime();
        
        record.setEndTime(endTime);
        record.setDuration(duration);
        readingRecordMapper.updateById(record);
        
        // 获取今日统计
        int todayTotal = getTodayTotal(record.getUserId());
        
        Map<String, Object> result = new HashMap<>();
        result.put("duration", duration);
        result.put("today_total", todayTotal);
        return AjaxResult.success(result);
    }

    @GetMapping("/stats")
    @ApiOperation(value = "阅读统计")
    public AjaxResult stats() {
        // TODO: 从token获取用户ID
        Integer userId = 1;
        
        int todayTotal = getTodayTotal(userId);
        int weekTotal = getWeekTotal(userId);
        int monthTotal = getMonthTotal(userId);
        int total = getTotal(userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("today", todayTotal);
        result.put("week", weekTotal);
        result.put("month", monthTotal);
        result.put("total", total);
        result.put("goal", 30); // 每日目标30分钟
        return AjaxResult.success(result);
    }

    @GetMapping("/records")
    @ApiOperation(value = "阅读记录")
    public AjaxResult records(@RequestParam(defaultValue = "1") Integer pageNo,
                             @RequestParam(defaultValue = "20") Integer pageSize) {
        // TODO: 从token获取用户ID
        Integer userId = 1;
        
        QueryWrapper<ReadingRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).orderByDesc("create_time");
        
        List<ReadingRecord> records = readingRecordMapper.selectList(wrapper);
        
        Map<String, Object> result = new HashMap<>();
        result.put("lists", records);
        result.put("count", records.size());
        return AjaxResult.success(result);
    }

    // 辅助方法：获取今日阅读时长（秒）
    private int getTodayTotal(Integer userId) {
        int todayStart = (int)(System.currentTimeMillis() / 1000) - 86400;
        QueryWrapper<ReadingRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).ge("create_time", todayStart);
        List<ReadingRecord> records = readingRecordMapper.selectList(wrapper);
        return records.stream().mapToInt(ReadingRecord::getDuration).sum();
    }

    // 辅助方法：获取本周阅读时长（秒）
    private int getWeekTotal(Integer userId) {
        int weekStart = (int)(System.currentTimeMillis() / 1000) - 604800;
        QueryWrapper<ReadingRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).ge("create_time", weekStart);
        List<ReadingRecord> records = readingRecordMapper.selectList(wrapper);
        return records.stream().mapToInt(ReadingRecord::getDuration).sum();
    }

    // 辅助方法：获取本月阅读时长（秒）
    private int getMonthTotal(Integer userId) {
        int monthStart = (int)(System.currentTimeMillis() / 1000) - 2592000;
        QueryWrapper<ReadingRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).ge("create_time", monthStart);
        List<ReadingRecord> records = readingRecordMapper.selectList(wrapper);
        return records.stream().mapToInt(ReadingRecord::getDuration).sum();
    }

    // 辅助方法：获取总阅读时长（秒）
    private int getTotal(Integer userId) {
        QueryWrapper<ReadingRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        List<ReadingRecord> records = readingRecordMapper.selectList(wrapper);
        return records.stream().mapToInt(ReadingRecord::getDuration).sum();
    }
}
