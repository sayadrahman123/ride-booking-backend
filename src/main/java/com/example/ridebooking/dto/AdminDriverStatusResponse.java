package com.example.ridebooking.dto;

public class AdminDriverStatusResponse {

    private Long driverId;
    private String name;
    private boolean active;   // DB flag
    private boolean available; // Redis flag
    private boolean busy;     // Redis lock exists

    // getters & setters
    public Long getDriverId() { return driverId; }
    public void setDriverId(Long driverId) { this.driverId = driverId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
    public boolean isBusy() { return busy; }
    public void setBusy(boolean busy) { this.busy = busy; }
}
