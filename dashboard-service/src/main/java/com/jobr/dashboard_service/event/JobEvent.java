package com.jobr.dashboard_service.event;

import java.time.Instant;

/**
 * Mirrors scheduler_service.event.JobEvent / worker_service.event.JobEvent.
 * Field names must match exactly - this is deserialized from the same JSON
 * those two services produce onto the job.events topic.
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
