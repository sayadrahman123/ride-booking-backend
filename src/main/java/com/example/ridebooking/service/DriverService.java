package com.example.ridebooking.service;

import com.example.ridebooking.dto.DriverLocationUpdateRequest;
import com.example.ridebooking.dto.DriverOnboardRequest;
import com.example.ridebooking.dto.DriverStatusUpdateRequest;
import com.example.ridebooking.entity.Driver;
import com.example.ridebooking.entity.Vehicle;

public interface DriverService {
    Driver onboard(DriverOnboardRequest req);
    Vehicle saveOrUpdateVehicle(Long driverId, DriverOnboardRequest req);
    Driver updateStatus(Long driverId, DriverStatusUpdateRequest req);
    void updateLocation(Long driverId, DriverLocationUpdateRequest req);
}
