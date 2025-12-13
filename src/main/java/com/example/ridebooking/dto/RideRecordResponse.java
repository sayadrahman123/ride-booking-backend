package com.example.ridebooking.dto;

import java.time.Instant;
import java.util.Map;

public class RideRecordResponse {
    private String rideId;
    private Long riderId;
    private Long driverId;
    private Instant startTime;
    private Instant endTime;
    private Long distanceMeters;
    private Long durationSeconds;
    private long fareCents;
    private String currency;
    private Map<String, Object> fareBreakdown;

    // getters / setters

    public String getRideId() { return rideId; }
    public void setRideId(String rideId) { this.rideId = rideId; }
    public Long getRiderId() { return riderId; }
    public void setRiderId(Long riderId) { this.riderId = riderId; }
    public Long getDriverId() { return driverId; }
    public void setDriverId(Long driverId) { this.driverId = driverId; }
    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }
    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }
    public Long getDistanceMeters() { return distanceMeters; }
    public void setDistanceMeters(Long distanceMeters) { this.distanceMeters = distanceMeters; }
    public Long getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Long durationSeconds) { this.durationSeconds = durationSeconds; }
    public long getFareCents() { return fareCents; }
    public void setFareCents(long fareCents) { this.fareCents = fareCents; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Map<String, Object> getFareBreakdown() { return fareBreakdown; }
    public void setFareBreakdown(Map<String, Object> fareBreakdown) { this.fareBreakdown = fareBreakdown; }
}
