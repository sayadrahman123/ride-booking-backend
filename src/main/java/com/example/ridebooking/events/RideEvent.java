package com.example.ridebooking.events;

import java.time.Instant;
import java.util.Map;

import lombok.*;


@Builder
@AllArgsConstructor
public class RideEvent {

    // getters + setters
    @Getter
    private String eventType;      // e.g., "ride.accepted"
    @Setter
    @Getter
    private String rideId;         // external ride id
    @Setter
    @Getter
    private Long driverId;
    private Long riderId;
    @Getter
    private String status;         // ACCEPTED / REJECTED
    private Instant timestamp;
    private Map<String,Object> payload;

    public RideEvent() {}

    public RideEvent(String eventType, String rideId, Long driverId, Long riderId, String status, Instant timestamp) {
        this.eventType = eventType;
        this.rideId = rideId;
        this.driverId = driverId;
        this.riderId = riderId;
        this.status = status;
        this.timestamp = timestamp;
    }

}
