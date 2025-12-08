package com.example.ridebooking.service;

import com.example.ridebooking.entity.Ride;

public interface RideService {
    /**
     * Driver accepts ride. Validates Redis lock belongs to driver and rideId.
     * Returns persisted Ride entity.
     */
    Ride acceptRide(String rideExternalId, Long driverId);

    /**
     * Driver rejects ride. If lock existed for this driver+ride, remove it.
     * Returns true if a lock was removed.
     */
    boolean rejectRide(String rideExternalId, Long driverId);

    Ride startRide(String rideExternalId, Long driverId);
    void updateRideLocation(String rideExternalId, Long driverId, Double lat, Double lng, Double speedMps);
    Ride completeRide(String rideExternalId, Long driverId, Long distanceMeters, Long durationSeconds);
}
