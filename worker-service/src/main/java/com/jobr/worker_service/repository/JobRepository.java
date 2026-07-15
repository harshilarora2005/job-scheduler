package com.jobr.worker_service.repository;

import com.jobr.worker_service.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long> {
    @Transactional
    @Query(value = """
        WITH claimed AS (
            SELECT id
            FROM jobs
            WHERE status = 'DISPATCHED'
              AND next_run_time <= now()
            ORDER BY priority ASC, next_run_time ASC
            LIMIT 1
            FOR UPDATE SKIP LOCKED
        )
        UPDATE jobs
        SET status = 'RUNNING',
            lease_owner = :workerId,
            lease_expires_at = now() + interval '30 seconds',
            last_heartbeat_at = now(),
            attempt_count = attempt_count + 1,
            updated_at = now()
        FROM claimed
        WHERE jobs.id = claimed.id
        RETURNING jobs.id
        """, nativeQuery = true)
    List<Long> claimNextJobRaw(@Param("workerId") String workerId);

    default Optional<Long> claimNextJob(String workerId) {
        List<Long> result = claimNextJobRaw(workerId);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    @Modifying
    @Transactional
    @Query(value = """
        UPDATE jobs SET lease_expires_at = now() + interval '30 seconds',
        last_heartbeat_at = now()
        WHERE id = :jobId AND lease_owner = :workerId
        """, nativeQuery = true)
    void heartbeat(@Param("jobId") Long jobId, @Param("workerId") String workerId);

    @Modifying
    @Transactional
    @Query(value = """
        UPDATE jobs SET status = 'SUCCEEDED', updated_at = now()
        WHERE id = :jobId AND lease_owner = :workerId
        """, nativeQuery = true)
    void markSucceeded(@Param("jobId") Long jobId, @Param("workerId") String workerId);

    @Modifying
    @Transactional
    @Query(value = """
        UPDATE jobs
        SET status = CASE WHEN attempt_count >= max_attempts THEN 'DEAD' ELSE 'PENDING' END,
            next_run_time = now() + (least(power(2, attempt_count), 300) || ' seconds')::interval,
            last_error = :error,
            lease_owner = NULL,
            lease_expires_at = NULL,
            updated_at = now()
        WHERE id = :jobId AND lease_owner = :workerId
        """, nativeQuery = true)
    void markFailed(@Param("jobId") Long jobId, @Param("workerId") String workerId, @Param("error") String error);
}