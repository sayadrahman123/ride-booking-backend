package com.example.ridebooking.service;

import com.example.ridebooking.redis.RedisKeys;

// 1. Correct Geo Imports
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;

// 2. Redis Connection Imports
import org.springframework.data.redis.connection.RedisGeoCommands.GeoLocation;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoSearchCommandArgs;

// 3. THIS IS THE MISSING IMPORT CAUSING YOUR ERROR
import org.springframework.data.redis.domain.geo.GeoReference;

import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

@Service
public class RedisMatchService {

    private final StringRedisTemplate redisTemplate;
    private final GeoOperations<String, String> geoOps;
    private final ValueOperations<String, String> valOps;

    public RedisMatchService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.geoOps = redisTemplate.opsForGeo();
        this.valOps = redisTemplate.opsForValue();
    }

    public List<Map<String, Object>> findNearbyDrivers(double lat, double lng, double radiusKm, int count) {

        // 1. Validate coordinates
        if (Math.abs(lat) > 90 || Math.abs(lng) > 180) {
            return Collections.emptyList();
        }

        // 2. Prepare Arguments (Limit, Sort, etc.)
        GeoSearchCommandArgs args = GeoSearchCommandArgs.newGeoSearchArgs()
                .includeDistance()
                .sortAscending()
                .limit(count);

        // 3. Execute Search - FIX: Split 'Circle' into 'Reference' and 'Distance'
        // This fixes the "Cannot resolve method search" error
        GeoResults<GeoLocation<String>> results = geoOps.search(
                RedisKeys.DRIVERS_GEO_KEY,
                GeoReference.fromCoordinate(new Point(lng, lat)), // The Center
                new Distance(radiusKm, Metrics.KILOMETERS),       // The Radius
                args
        );

        if (results == null || results.getContent().isEmpty()) {
            return Collections.emptyList();
        }

        // 4. Optimization: Fetch all availability flags in ONE network call (Pipeline)
        List<GeoResult<GeoLocation<String>>> resultList = results.getContent();
        List<String> availabilityKeys = new ArrayList<>();
        List<String> driverMembers = new ArrayList<>();

        for (GeoResult<GeoLocation<String>> res : resultList) {
            String member = res.getContent().getName();
            driverMembers.add(member);
            availabilityKeys.add(RedisKeys.DRIVER_AVAIL_PREFIX + extractDriverId(member));
        }

        List<String> availabilityValues = valOps.multiGet(availabilityKeys);

        // 5. Build Final Result
        List<Map<String, Object>> response = new ArrayList<>();

        for (int i = 0; i < resultList.size(); i++) {
            GeoResult<GeoLocation<String>> geoRes = resultList.get(i);
            String member = driverMembers.get(i);
            String availVal = (availabilityValues != null) ? availabilityValues.get(i) : null;

            Map<String, Object> map = new HashMap<>();
            map.put("driverMember", member);
            map.put("driverId", extractDriverId(member));
            map.put("distanceKm", geoRes.getDistance().getValue());
            map.put("available", "true".equalsIgnoreCase(availVal));

            response.add(map);
        }

        return response;
    }

    // --- Helper Methods ---

    private String extractDriverId(String driverMember) {
        if (driverMember == null) return "";
        int idx = driverMember.indexOf(':');
        return (idx == -1) ? driverMember : driverMember.substring(idx + 1);
    }

    public boolean isDriverAvailable(String driverMember) {
        String driverId = extractDriverId(driverMember);
        String val = valOps.get(RedisKeys.DRIVER_AVAIL_PREFIX + driverId);
        return "true".equalsIgnoreCase(val);
    }

    public boolean tryReserveDriver(String driverMember, String rideId, long ttlSeconds) {
        String driverId = extractDriverId(driverMember);

        if (!isDriverAvailable(driverMember)) {
            return false;
        }

        String lockKey = RedisKeys.DRIVER_LOCK_PREFIX + driverId;
        Boolean success = valOps.setIfAbsent(lockKey, "ride:" + rideId, Duration.ofSeconds(ttlSeconds));
        return Boolean.TRUE.equals(success);
    }

    public void releaseDriverReservationByDriverId(String driverId) {
        if (driverId != null) {
            redisTemplate.delete(RedisKeys.DRIVER_LOCK_PREFIX + driverId);
        }
    }

    public String getDriverReservation(String driverId) {
        return valOps.get(RedisKeys.DRIVER_LOCK_PREFIX + driverId);
    }

    public boolean isDriverBusy(String driverId) {
        return redisTemplate.hasKey(
                RedisKeys.DRIVER_LOCK_PREFIX + driverId
        );
    }

    public boolean isDriverAvailableById(String driverId) {
        String val = valOps.get(RedisKeys.DRIVER_AVAIL_PREFIX + driverId);
        return "true".equalsIgnoreCase(val);
    }

}