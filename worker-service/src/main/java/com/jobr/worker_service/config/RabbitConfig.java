package com.jobr.worker_service.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String JOB_READY_QUEUE = "job.ready";

    @Bean
    public Queue jobReadyQueue() {
        return new Queue(JOB_READY_QUEUE, true); // durable
    }
}