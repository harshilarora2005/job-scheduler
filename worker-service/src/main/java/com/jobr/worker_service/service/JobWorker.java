package com.jobr.worker_service.service;

import com.jobr.worker_service.entity.Job;
import com.jobr.worker_service.event.JobEventPublisher;
import com.jobr.worker_service.repository.JobRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class JobWorker {

    private final JobRepository jobRepository;
    private final JobEventPublisher jobEventPublisher;
    private final String workerId = "worker-" + UUID.randomUUID().toString().substring(0, 8);

    public JobWorker(JobRepository jobRepository, JobEventPublisher jobEventPublisher) {
        this.jobRepository = jobRepository;
        this.jobEventPublisher = jobEventPublisher;
    }

    @Scheduled(fixedDelay = 10000)
    public void poll() {
        tryClaimAndExecute();
    }

    private void execute(Job job) {
        Long jobId = job.getId();
        try {
            for (int i = 0; i < 2; i++) {
                Thread.sleep(1000);
                jobRepository.heartbeat(jobId, workerId);
            }
            jobRepository.markSucceeded(jobId, workerId);
            jobEventPublisher.publish(jobId, job.getJobType(), "SUCCEEDED", workerId, job.getAttemptCount(), null);
            System.out.println("[" + workerId + "] completed job " + jobId);
        } catch (Exception e) {
            jobRepository.markFailed(jobId, workerId, e.getMessage());
            // markFailed sets status to DEAD (max attempts hit) or PENDING (will retry) -
            // re-read so the event reflects which one actually happened.
            String finalStatus = jobRepository.findById(jobId)
                    .map(Job::getStatus)
                    .orElse("FAILED");
            jobEventPublisher.publish(jobId, job.getJobType(), finalStatus, workerId, job.getAttemptCount(), e.getMessage());
            System.out.println("[" + workerId + "] failed job " + jobId + ": " + e.getMessage());
        }
    }

    public void tryClaimAndExecute() {
        jobRepository.claimNextJob(workerId).ifPresent(jobId -> {
            System.out.println("[" + workerId + "] claimed job " + jobId);
            jobRepository.findById(jobId).ifPresent(job -> {
                jobEventPublisher.publish(jobId, job.getJobType(), "RUNNING", workerId, job.getAttemptCount(), null);
                execute(job);
            });
        });
    }
}