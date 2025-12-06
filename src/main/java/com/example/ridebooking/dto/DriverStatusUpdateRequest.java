package com.example.ridebooking.dto;

public class DriverStatusUpdateRequest {
    private boolean available;
    private Double lat;
    private Double lng;

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }
    public Double getLng() { return lng; }
    public void setLng(Double lng) { this.lng = lng; }
}
