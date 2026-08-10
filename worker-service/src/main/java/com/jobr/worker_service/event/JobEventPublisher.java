package com.jobr.worker_service.event;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class JobEventPublisher {

    // Topic is declared (with partitioning) by scheduler-service's KafkaTopicConfig;
    // worker-service just needs the name to produce to it.
    public static final String JOB_EVENTS_TOPIC = "job.events";

    private final KafkaTemplate<String, JobEvent> kafkaTemplate;

    public JobEventPublisher(KafkaTemplate<String, JobEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(Long jobId, String jobType, String status, String workerId,
                         Integer attemptCount, String error) {
        JobEvent event = new JobEvent(jobId, jobType, status, workerId, attemptCount, error, Instant.now());
        kafkaTemplate.send(JOB_EVENTS_TOPIC, jobType, event);
    }
}
