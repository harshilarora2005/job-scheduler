package com.jobr.dashboard_service.service;

import com.jobr.dashboard_service.event.JobEvent;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Rolling, in-memory counters built entirely off the job.events Kafka stream - no
 * Postgres aggregate queries involved. This is the "real-time analytics off an event
 * stream" piece rather than another consumer of the same DB the other services write to.
 *
 * Deliberately simple (plain counters, no windowing library): good enough for the
 * load-test numbers this project needs, and easy to explain in an interview.
 */
@Service
public class JobStatsService {

    private final LongAdder dispatched = new LongAdder();
    private final LongAdder running = new LongAdder();
    private final LongAdder succeeded = new LongAdder();
    private final LongAdder failed = new LongAdder();
    private final LongAdder dead = new LongAdder();

    private final Map<String, LongAdder> succeededByType = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> failedByType = new ConcurrentHashMap<>();

    // sum of (SUCCEEDED timestamp - DISPATCHED timestamp) in millis, plus a count, to derive
    // an average end-to-end completion latency without storing every individual sample
    private final AtomicLong totalCompletionMillis = new AtomicLong();
    private final LongAdder completionSamples = new LongAdder();
    private final Map<Long, Instant> dispatchedAt = new ConcurrentHashMap<>();

    public void record(JobEvent event) {
        switch (event.status()) {
            case "DISPATCHED" -> {
                dispatched.increment();
                dispatchedAt.put(event.jobId(), event.timestamp());
            }
            case "RUNNING" -> running.increment();
            case "SUCCEEDED" -> {
                succeeded.increment();
                succeededByType.computeIfAbsent(event.jobType(), t -> new LongAdder()).increment();
                Instant start = dispatchedAt.remove(event.jobId());
                if (start != null) {
                    totalCompletionMillis.addAndGet(event.timestamp().toEpochMilli() - start.toEpochMilli());
                    completionSamples.increment();
                }
            }
            case "FAILED" -> {
                failed.increment();
                failedByType.computeIfAbsent(event.jobType(), t -> new LongAdder()).increment();
            }
            case "DEAD" -> {
                dead.increment();
                dispatchedAt.remove(event.jobId());
            }
            default -> { /* ignore unknown status values rather than throwing */ }
        }
    }

    public JobStatsSnapshot snapshot() {
        long samples = completionSamples.sum();
        Double avgCompletionMillis = samples == 0 ? null : (double) totalCompletionMillis.get() / samples;

        return new JobStatsSnapshot(
                dispatched.sum(),
                running.sum(),
                succeeded.sum(),
                failed.sum(),
                dead.sum(),
                toPlainMap(succeededByType),
                toPlainMap(failedByType),
                avgCompletionMillis
        );
    }

    private Map<String, Long> toPlainMap(Map<String, LongAdder> source) {
        Map<String, Long> result = new ConcurrentHashMap<>();
        source.forEach((k, v) -> result.put(k, v.sum()));
        return result;
    }

    public record JobStatsSnapshot(
            long dispatched,
            long running,
            long succeeded,
            long failed,
            long dead,
            Map<String, Long> succeededByType,
            Map<String, Long> failedByType,
            Double avgCompletionMillis
    ) {
    }
}
