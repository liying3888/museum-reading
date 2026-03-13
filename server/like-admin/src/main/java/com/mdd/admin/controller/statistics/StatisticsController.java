package com.mdd.admin.controller.statistics;

import com.mdd.admin.service.statistics.IStatisticsService;
import com.mdd.common.core.AjaxResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

@RestController
@RequestMapping("adminapi/statistics")
@Api(tags = "数据统计")
public class StatisticsController {

    @Resource
    private IStatisticsService statisticsService;

    @GetMapping("/overview")
    @ApiOperation(value = "总览数据")
    public AjaxResult overview() {
        Map<String, Object> data = statisticsService.getOverview();
        return AjaxResult.success(data);
    }

    @GetMapping("/reading")
    @ApiOperation(value = "阅读统计")
    public AjaxResult reading(@RequestParam(required = false) String startDate,
                             @RequestParam(required = false) String endDate) {
        Map<String, Object> data = statisticsService.getReadingStatistics(startDate, endDate);
        return AjaxResult.success(data);
    }
}
