package com.jobr.scheduler_service.service;

import com.jobr.scheduler_service.config.RabbitConfig;
import com.jobr.scheduler_service.event.JobEventPublisher;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SchedulerDispatcher {

    private final JdbcTemplate jdbcTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final JobEventPublisher jobEventPublisher;

    public SchedulerDispatcher(JdbcTemplate jdbcTemplate, RabbitTemplate rabbitTemplate,
                                JobEventPublisher jobEventPublisher) {
        this.jdbcTemplate = jdbcTemplate;
        this.rabbitTemplate = rabbitTemplate;
        this.jobEventPublisher = jobEventPublisher;
    }

    @Scheduled(fixedDelay = 1000)
    public void dispatchDueJobs() {
        List<Map<String, Object>> due = jdbcTemplate.queryForList("""
            SELECT id, job_type, attempt_count FROM jobs
            WHERE status = 'PENDING' AND next_run_time <= now()
            ORDER BY priority ASC, next_run_time ASC
            LIMIT 100
            """);

        if (due.isEmpty()) return;

        List<Long> dueIds = due.stream().map(row -> (Long) row.get("id")).toList();

        int updated = jdbcTemplate.update("""
            UPDATE jobs SET status = 'DISPATCHED', updated_at = now()
            WHERE id = ANY(?)
            """, (Object) dueIds.toArray(new Long[0]));

        for (Map<String, Object> row : due) {
            Long id = (Long) row.get("id");
            String jobType = (String) row.get("job_type");
            Integer attemptCount = (Integer) row.get("attempt_count");
            rabbitTemplate.convertAndSend(RabbitConfig.JOB_READY_QUEUE, id.toString());
            jobEventPublisher.publish(id, jobType, "DISPATCHED", attemptCount);
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