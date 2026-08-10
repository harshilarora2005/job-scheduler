package com.jobr.scheduler_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String JOB_EVENTS_TOPIC = "job.events";

    /**
     * Partitioned by job_type via the producer key (see JobEventPublisher), so all events
     * for a given job type land on the same partition and stay ordered. 3 partitions is
     * plenty for local load testing and still demonstrates multi-partition behavior.
     */
    @Bean
    public NewTopic jobEventsTopic() {
        return TopicBuilder.name(JOB_EVENTS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
