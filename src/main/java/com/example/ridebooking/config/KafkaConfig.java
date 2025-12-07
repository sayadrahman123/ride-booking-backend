package com.example.ridebooking.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfig {

    // Topic names
    public static final String TOPIC_RIDE_EVENTS = "ride.events";

    @Bean
    public NewTopic rideEventsTopic() {
        return new NewTopic(TOPIC_RIDE_EVENTS, 1, (short) 1);
    }
}
