package com.jobr.scheduler_service.event;

import java.time.Instant;

/**
 * Published to the job.events Kafka topic on every state transition.
 * Consumed by dashboard-service to build real-time stats without querying Postgres.
 * Mirrored (not shared via a common module on purpose - keeps each service independently deployable)
 * in worker-service and dashboard-service.
 */
public record JobEvent(
        Long jobId,
        String jobType,
        String status,
        String workerId,
        Integer attemptCount,
        String error,
        Instant timestamp
) {
}
