package com.jobr.dashboard_service.service;

import com.jobr.dashboard_service.event.JobEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class JobEventConsumer {

    private final JobStatsService jobStatsService;

    public JobEventConsumer(JobStatsService jobStatsService) {
        this.jobStatsService = jobStatsService;
    }

    @KafkaListener(topics = "job.events", groupId = "dashboard-service")
    public void onJobEvent(JobEvent event) {
        jobStatsService.record(event);
    }
}
