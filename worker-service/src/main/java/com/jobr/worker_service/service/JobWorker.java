package com.jobr.worker_service.service;

import com.jobr.worker_service.repository.JobRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class JobWorker {

    private final JobRepository jobRepository;
    private final String workerId = "worker-" + UUID.randomUUID().toString().substring(0, 8);

    public JobWorker(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Scheduled(fixedDelay = 10000)
    public void poll() {
        tryClaimAndExecute();
    }

    private void execute(Long jobId) {
        try {
            for (int i = 0; i < 2; i++) {
                Thread.sleep(1000);
                jobRepository.heartbeat(jobId, workerId);
            }
            jobRepository.markSucceeded(jobId, workerId);
            System.out.println("[" + workerId + "] completed job " + jobId);
        } catch (Exception e) {
            jobRepository.markFailed(jobId, workerId, e.getMessage());
            System.out.println("[" + workerId + "] failed job " + jobId + ": " + e.getMessage());
        }
    }

    public void tryClaimAndExecute() {
        jobRepository.claimNextJob(workerId).ifPresent(jobId -> {
            System.out.println("[" + workerId + "] claimed job " + jobId);
            execute(jobId);
        });
    }
}