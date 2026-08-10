package com.jobr.scheduler_service.event;

import com.jobr.scheduler_service.config.KafkaTopicConfig;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class JobEventPublisher {

    private final KafkaTemplate<String, JobEvent> kafkaTemplate;

    public JobEventPublisher(KafkaTemplate<String, JobEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(Long jobId, String jobType, String status, Integer attemptCount) {
        JobEvent event = new JobEvent(jobId, jobType, status, null, attemptCount, null, Instant.now());
        // key by jobType so all events for a given job type stay ordered on one partition
        kafkaTemplate.send(KafkaTopicConfig.JOB_EVENTS_TOPIC, jobType, event);
    }
}
