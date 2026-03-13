package com.mdd.front.controller.stats;

import com.mdd.common.core.AjaxResult;
import com.mdd.front.service.stats.ReadingStatsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/front/stats")
@Api(tags = "阅读统计")
public class StatsController {

    @Resource
    private ReadingStatsService readingStatsService;

    @GetMapping("/today")
    @ApiOperation(value = "今日统计")
    public AjaxResult today() {
        // TODO: 从token获取用户ID
        Integer userId = 1;
        
        Map<String, Object> stats = readingStatsService.getTodayStats(userId);
        return AjaxResult.success(stats);
    }

    @GetMapping("/week")
    @ApiOperation(value = "本周统计")
    public AjaxResult week() {
        Integer userId = 1;
        
        Map<String, Object> stats = readingStatsService.getWeekStats(userId);
        return AjaxResult.success(stats);
    }

    @GetMapping("/month")
    @ApiOperation(value = "本月统计")
    public AjaxResult month() {
        Integer userId = 1;
        
        Map<String, Object> stats = readingStatsService.getMonthStats(userId);
        return AjaxResult.success(stats);
    }

    @GetMapping("/total")
    @ApiOperation(value = "累计统计")
    public AjaxResult total() {
        Integer userId = 1;
        
        Map<String, Object> stats = readingStatsService.getTotalStats(userId);
        return AjaxResult.success(stats);
    }

    @GetMapping("/checkin")
    @ApiOperation(value = "打卡记录")
    public AjaxResult checkin() {
        Integer userId = 1;
        
        List<String> dates = readingStatsService.getCheckinRecords(userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("dates", dates);
        result.put("total_days", dates.size());
        return AjaxResult.success(result);
    }

    @GetMapping("/overview")
    @ApiOperation(value = "统计概览")
    public AjaxResult overview() {
        Integer userId = 1;
        
        Map<String, Object> overview = new HashMap<>();
        overview.put("today", readingStatsService.getTodayStats(userId));
        overview.put("week", readingStatsService.getWeekStats(userId));
        overview.put("month", readingStatsService.getMonthStats(userId));
        overview.put("total", readingStatsService.getTotalStats(userId));
        overview.put("checkin", readingStatsService.getCheckinRecords(userId));
        
        return AjaxResult.success(overview);
    }
}
