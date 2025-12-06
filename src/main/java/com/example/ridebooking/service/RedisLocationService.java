package com.example.ridebooking.service;

import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisLocationService {

    private static final String DRIVERS_GEO_KEY = "drivers:geo";
    private static final String DRIVER_AVAIL_KEY_PREFIX = "driver:avail:"; // driver:avail:<id> -> "true"/"false"

    private final StringRedisTemplate redisTemplate;
    private final GeoOperations<String, String> geoOps;

    public RedisLocationService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.geoOps = redisTemplate.opsForGeo();
    }

    // store driver location in GEO set
    public void updateDriverLocation(Long driverId, double lat, double lng) {
        geoOps.add(DRIVERS_GEO_KEY, new Point(lng, lat), "driver:" + driverId);
    }

    // remove driver from geo set (when offline)
    public void removeDriverLocation(Long driverId) {
        geoOps.remove(DRIVERS_GEO_KEY, "driver:" + driverId);
    }

    // mark available/unavailable in Redis (simple flag)
    public void setDriverAvailability(Long driverId, boolean available) {
        redisTemplate.opsForValue().set(DRIVER_AVAIL_KEY_PREFIX + driverId, String.valueOf(available));
        if (!available) {
            removeDriverLocation(driverId);
        }
    }

    public Boolean isDriverAvailable(Long driverId) {
        String v = redisTemplate.opsForValue().get(DRIVER_AVAIL_KEY_PREFIX + driverId);
        return v == null ? Boolean.FALSE : Boolean.valueOf(v);
    }
}
