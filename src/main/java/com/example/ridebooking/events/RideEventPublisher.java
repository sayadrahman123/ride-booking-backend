package com.example.ridebooking.events;

import com.example.ridebooking.config.KafkaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class RideEventPublisher {

    private final KafkaTemplate<String, Object> kafka;
    private final Logger log = LoggerFactory.getLogger(RideEventPublisher.class);

    public RideEventPublisher(KafkaTemplate<String, Object> kafka) {
        this.kafka = kafka;
    }

    public void publish(RideEvent event) {
        try {
            kafka.send(KafkaConfig.TOPIC_RIDE_EVENTS, event.getRideId(), event);
            log.info("Published ride event: {} -> rideId={}", event.getEventType(), event.getRideId());
        } catch (Exception ex) {
            log.error("Failed to publish ride event {}", event.getRideId(), ex);
        }
    }
}
