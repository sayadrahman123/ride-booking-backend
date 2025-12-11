package com.example.ridebooking.fare;

public interface FareEngine {
    /**
     * Calculate fare for a ride.
     *
     * @param distanceMeters   total distance in meters
     * @param durationSeconds  total duration in seconds
     * @param surgeMultiplier  surge multiplier (1.0 = no surge)
     * @return FareResult containing fare in cents and a breakdown map
     */
    FareResult calculate(long distanceMeters, long durationSeconds, double surgeMultiplier);


}
