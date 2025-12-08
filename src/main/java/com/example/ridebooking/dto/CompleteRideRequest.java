package com.example.ridebooking.dto;

public class CompleteRideRequest {
    private Long driverId;
    private Long distanceMeters;
    private Long durationSeconds;

    public Long getDriverId() { return driverId; }
    public void setDriverId(Long driverId) { this.driverId = driverId; }
    public Long getDistanceMeters() { return distanceMeters; }
    public void setDistanceMeters(Long distanceMeters) { this.distanceMeters = distanceMeters; }
    public Long getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Long durationSeconds) { this.durationSeconds = durationSeconds; }
}
