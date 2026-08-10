package com.jobr.worker_service.event;

import java.time.Instant;

/**
 * Published to the job.events Kafka topic on every state transition.
 * Mirrors scheduler_service.event.JobEvent - kept separate on purpose so each
 * service stays independently deployable without a shared library module.
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
