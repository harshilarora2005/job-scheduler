package com.jobr.worker_service.config;

import com.jobr.worker_service.event.JobEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Spring Boot's autoconfigured KafkaTemplate bean is generically typed <Object, Object>,
 * which does NOT satisfy an injection point asking for KafkaTemplate<String, JobEvent> -
 * generics are invariant, so autowiring fails with a "no bean of type KafkaTemplate found"
 * error even though one exists. Defining it explicitly here, correctly typed, fixes that.
 */
@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, JobEvent> jobEventProducerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, JobEvent> jobEventKafkaTemplate(
            ProducerFactory<String, JobEvent> jobEventProducerFactory) {
        return new KafkaTemplate<>(jobEventProducerFactory);
    }
}
