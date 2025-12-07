package com.example.ridebooking.events;

import java.time.Instant;

public class RideEvent {
    private String eventType;      // e.g., "ride.accepted"
    private String rideId;         // external ride id
    private Long driverId;
    private Long riderId;
    private String status;         // ACCEPTED / REJECTED
    private Instant timestamp;

    public RideEvent() {}

    public RideEvent(String eventType, String rideId, Long driverId, Long riderId, String status, Instant timestamp) {
        this.eventType = eventType;
        this.rideId = rideId;
        this.driverId = driverId;
        this.riderId = riderId;
        this.status = status;
        this.timestamp = timestamp;
    }

    // getters + setters
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getRideId() { return rideId; }
    public void setRideId(String rideId) { this.rideId = rideId; }
    public Long getDriverId() { return driverId; }
    public void setDriverId(Long driverId) { this.driverId = driverId; }
    public Long getRiderId() { return riderId; }
    public void setRiderId(Long riderId) { this.riderId = riderId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
