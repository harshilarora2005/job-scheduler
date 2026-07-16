package com.jobr.worker_service.service;

import com.jobr.worker_service.config.RabbitConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class JobListener {

    private final JobWorker jobWorker;

    public JobListener(JobWorker jobWorker) {
        this.jobWorker = jobWorker;
    }

    @RabbitListener(queues = RabbitConfig.JOB_READY_QUEUE)
    public void onJobReady(String jobIdMessage) {
        // The message is just a nudge, not trusted directly.
        // Try to claim immediately rather than waiting for the next poll.
        jobWorker.tryClaimAndExecute();
    }
}