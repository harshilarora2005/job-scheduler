package com.jobr.scheduler_service.service;

import com.jobr.scheduler_service.config.RabbitConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SchedulerDispatcher {

    private final JdbcTemplate jdbcTemplate;
    private final RabbitTemplate rabbitTemplate;

    public SchedulerDispatcher(JdbcTemplate jdbcTemplate, RabbitTemplate rabbitTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedDelay = 1000)
    public void dispatchDueJobs() {
        List<Long> dueIds = jdbcTemplate.queryForList("""
            SELECT id FROM jobs
            WHERE status = 'PENDING' AND next_run_time <= now()
            ORDER BY priority ASC, next_run_time ASC
            LIMIT 100
            """, Long.class);

        if (dueIds.isEmpty()) return;

        int updated = jdbcTemplate.update("""
            UPDATE jobs SET status = 'DISPATCHED', updated_at = now()
            WHERE id = ANY(?)
            """, (Object) dueIds.toArray(new Long[0]));

        for (Long id : dueIds) {
            rabbitTemplate.convertAndSend(RabbitConfig.JOB_READY_QUEUE, id.toString());
        }
        System.out.println("Dispatched and published " + updated + " jobs");
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