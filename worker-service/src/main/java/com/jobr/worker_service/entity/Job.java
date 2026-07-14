package com.jobr.worker_service.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Type;

import java.time.Instant;

@Entity
@Table(name = "jobs")
@Data
public class Job {

    @Id
    private Long id;

    @Column(name = "job_type", nullable = false)
    private String jobType;

    @Type(JsonType.class)
    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    private String payload;

    private String status;
    private Short priority;

    @Column(name = "next_run_time")
    private Instant nextRunTime;

    @Column(name = "attempt_count")
    private Integer attemptCount;

    @Column(name = "max_attempts")
    private Integer maxAttempts;

    @Column(name = "lease_owner")
    private String leaseOwner;

    @Column(name = "lease_expires_at")
    private Instant leaseExpiresAt;

    @Column(name = "last_heartbeat_at")
    private Instant lastHeartbeatAt;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "updated_at")
    private Instant updatedAt;
}