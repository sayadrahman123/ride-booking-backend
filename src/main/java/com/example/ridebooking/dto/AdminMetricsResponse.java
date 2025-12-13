package com.example.ridebooking.dto;

public class AdminMetricsResponse {

    private long totalRides;
    private long activeRides;
    private long completedRides;

    private long totalRevenueCents;
    private long averageFareCents;
    private long averageDurationSeconds;

    // getters & setters
    public long getTotalRides() { return totalRides; }
    public void setTotalRides(long totalRides) { this.totalRides = totalRides; }

    public long getActiveRides() { return activeRides; }
    public void setActiveRides(long activeRides) { this.activeRides = activeRides; }

    public long getCompletedRides() { return completedRides; }
    public void setCompletedRides(long completedRides) { this.completedRides = completedRides; }

    public long getTotalRevenueCents() { return totalRevenueCents; }
    public void setTotalRevenueCents(long totalRevenueCents) { this.totalRevenueCents = totalRevenueCents; }

    public long getAverageFareCents() { return averageFareCents; }
    public void setAverageFareCents(long averageFareCents) { this.averageFareCents = averageFareCents; }

    public long getAverageDurationSeconds() { return averageDurationSeconds; }
    public void setAverageDurationSeconds(long averageDurationSeconds) { this.averageDurationSeconds = averageDurationSeconds; }
}
