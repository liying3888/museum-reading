package com.mdd.admin.service.statistics;

import java.util.Map;

public interface IStatisticsService {
    Map<String, Object> getOverview();
    Map<String, Object> getReadingStatistics(String startDate, String endDate);
}
