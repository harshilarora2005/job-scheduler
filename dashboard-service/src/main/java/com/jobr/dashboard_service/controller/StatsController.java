package com.jobr.dashboard_service.controller;

import com.jobr.dashboard_service.service.JobStatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatsController {

    private final JobStatsService jobStatsService;

    public StatsController(JobStatsService jobStatsService) {
        this.jobStatsService = jobStatsService;
    }

    @GetMapping("/stats")
    public JobStatsService.JobStatsSnapshot getStats() {
        return jobStatsService.snapshot();
    }
}
