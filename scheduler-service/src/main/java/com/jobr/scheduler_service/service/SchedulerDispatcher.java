package com.jobr.scheduler_service.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SchedulerDispatcher {

    private final JdbcTemplate jdbcTemplate;

    public SchedulerDispatcher(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(fixedDelay = 1000)
    public void dispatchDueJobs() {
        int updated = jdbcTemplate.update("""
            UPDATE jobs
            SET status = 'DISPATCHED', updated_at = now()
            WHERE id IN (
                SELECT id FROM jobs
                WHERE status = 'PENDING' AND next_run_time <= now()
                ORDER BY priority ASC, next_run_time ASC
                LIMIT 100
            )
            """);
        if (updated > 0) System.out.println("Dispatched " + updated + " jobs");
    }

    @Scheduled(fixedDelay = 5000)
    public void reclaimExpiredLeases() {
        int reclaimed = jdbcTemplate.update("""
            UPDATE jobs
            SET status = 'PENDING', lease_owner = NULL, lease_expires_at = NULL, next_run_time = now()
            WHERE status = 'RUNNING' AND lease_expires_at < now()
            """);
        if (reclaimed > 0) System.out.println("Reclaimed " + reclaimed + " expired leases");
    }
}