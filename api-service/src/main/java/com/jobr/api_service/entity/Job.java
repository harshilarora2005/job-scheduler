package com.jobr.api_service.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "jobs")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_type", nullable = false)
    private String jobType;

    @Column(columnDefinition = "jsonb", nullable = false)
    private String payload;

    @Column(nullable = false)
    private String status = "PENDING";

    private Short priority = 5;

    @Column(name = "next_run_time", nullable = false)
    private Instant nextRunTime = Instant.now();

    @Column(name = "cron_expression")
    private String cronExpression;

    @Column(name = "is_recurring")
    private Boolean isRecurring = false;

    @Column(name = "attempt_count")
    private Integer attemptCount = 0;

    @Column(name = "max_attempts")
    private Integer maxAttempts = 5;

    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    @Column(name = "job_hash")
    private String jobHash;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();
}