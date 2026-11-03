package com.mdd.front.controller.stats;

import com.mdd.common.core.AjaxResult;
import com.mdd.front.LikeFrontThreadLocal;
import com.mdd.front.service.stats.ReadingStatsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计域——契约 T1/T2+四基础接口（docs/S3/01 §2.4，leader 2026-11-04 过闸）
 */
@RestController
@RequestMapping("api/front/stats")
@Api(tags = "阅读统计")
public class StatsController {

    @Resource
    private ReadingStatsService readingStatsService;

    @GetMapping("/today")
    @ApiOperation(value = "今日统计")
    public AjaxResult today() {
        Integer userId = LikeFrontThreadLocal.getUserId();
        return AjaxResult.success(readingStatsService.getTodayStats(userId));
    }

    @GetMapping("/week")
    @ApiOperation(value = "本周统计")
    public AjaxResult week() {
        Integer userId = LikeFrontThreadLocal.getUserId();
        return AjaxResult.success(readingStatsService.getWeekStats(userId));
    }

    @GetMapping("/month")
    @ApiOperation(value = "本月统计")
    public AjaxResult month() {
        Integer userId = LikeFrontThreadLocal.getUserId();
        return AjaxResult.success(readingStatsService.getMonthStats(userId));
    }

    @GetMapping("/total")
    @ApiOperation(value = "累计统计")
    public AjaxResult total() {
        Integer userId = LikeFrontThreadLocal.getUserId();
        return AjaxResult.success(readingStatsService.getTotalStats(userId));
    }

    /**
     * T1 打卡记录（月参数化）：month='yyyy-MM' 缺省当月；日期数组东八区日界
     */
    @GetMapping("/checkin")
    @ApiOperation(value = "打卡记录（按月）")
    public AjaxResult checkin(@RequestParam(required = false) String month) {
        Integer userId = LikeFrontThreadLocal.getUserId();

        List<String> dates = readingStatsService.getCheckinRecords(userId, month);

        Map<String, Object> result = new HashMap<>();
        result.put("dates", dates);
        result.put("total_days", dates.size());
        return AjaxResult.success(result);
    }

    /**
     * T2 统计概览（today/week/month/total + goal——首页目标环/统计页三格一次拿全）
     */
    @GetMapping("/overview")
    @ApiOperation(value = "统计概览")
    public AjaxResult overview() {
        Integer userId = LikeFrontThreadLocal.getUserId();

        Map<String, Object> overview = new HashMap<>();
        overview.put("today", readingStatsService.getTodayStats(userId));
        overview.put("week", readingStatsService.getWeekStats(userId));
        overview.put("month", readingStatsService.getMonthStats(userId));
        overview.put("total", readingStatsService.getTotalStats(userId));
        overview.put("goal", readingStatsService.getDailyGoal(userId));
        return AjaxResult.success(overview);
    }
}
