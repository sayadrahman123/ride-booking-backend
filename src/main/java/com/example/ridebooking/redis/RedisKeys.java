package com.example.ridebooking.redis;

public final class RedisKeys {
    public static final String DRIVERS_GEO_KEY = "drivers:geo";
    public static final String DRIVER_AVAIL_PREFIX = "driver:avail:"; // + driverId -> "true"/"false"
    public static final String DRIVER_LOCK_PREFIX = "driver:lock:"; // + driverId -> ride:<rideId>
    private RedisKeys() {}
}
