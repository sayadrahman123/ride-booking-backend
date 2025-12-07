package com.example.ridebooking.events;

import com.example.ridebooking.config.KafkaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class RideEventsListener {

    private final Logger log = LoggerFactory.getLogger(RideEventsListener.class);

    @KafkaListener(topics = KafkaConfig.TOPIC_RIDE_EVENTS, groupId = "ride-events-group")
    public void onRideEvent(RideEvent event) {
        log.info("[KafkaConsumer] Received ride event: type={} rideId={} driverId={} status={}",
                event.getEventType(), event.getRideId(), event.getDriverId(), event.getStatus());
        // For demo: you could further route to other services, persist audit logs, etc.
    }
}
