package com.example.ridebooking.controller;

import com.example.ridebooking.security.AuthUtils;
import com.example.ridebooking.service.RedisMatchService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Controller that exposes endpoints for finding nearby drivers and attempting to reserve one.
 */
@RestController
@RequestMapping("/api/match")
public class MatchingController {

    private final RedisMatchService matchService;
    private final AuthUtils authUtils;

    public MatchingController(RedisMatchService matchService, AuthUtils authUtils) {
        this.matchService = matchService;
        this.authUtils = authUtils;
    }

    /**
     * Quick nearby search (no reservation).
     * Example:
     * GET /api/match/nearby?lat=26.14&lng=91.73&radiusKm=3&count=10
     */
    @GetMapping("/nearby")
    public ResponseEntity<?> nearby(@RequestParam double lat,
                                    @RequestParam double lng,
                                    @RequestParam(defaultValue = "3") double radiusKm,
                                    @RequestParam(defaultValue = "10") int count) {
        List<Map<String, Object>> results = matchService.findNearbyDrivers(lat, lng, radiusKm, count);
        return ResponseEntity.ok(results);
    }

    /**
     * Try to reserve a driver for a ride.
     * Caller must be authenticated (rider or system user).
     *
     * Example:
     * POST /api/match/try/{rideId}?lat=26.14&lng=91.73&radiusKm=3&count=10&reserveTtlSeconds=20
     */
    @PostMapping("/try/{rideId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> tryMatch(@PathVariable String rideId,
                                      @RequestParam double lat,
                                      @RequestParam double lng,
                                      @RequestParam(defaultValue = "3") double radiusKm,
                                      @RequestParam(defaultValue = "10") int count,
                                      @RequestParam(defaultValue = "20") long reserveTtlSeconds) {
        // optional: get caller id (for logging/authorization/audit)
        Long callerUserId = null;
        try { callerUserId = authUtils.requireCurrentUserId(); } catch (Exception ignored) {}

        // Get candidate drivers as maps (driverMember, driverId, distanceKm, available)
        List<Map<String, Object>> candidates = matchService.findNearbyDrivers(lat, lng, radiusKm, count);

        for (Map<String, Object> candidate : candidates) {
            Object memberObj = candidate.get("driverMember");
            if (memberObj == null) continue;
            String driverMember = String.valueOf(memberObj);

            boolean reserved = matchService.tryReserveDriver(driverMember, rideId, reserveTtlSeconds);
            if (reserved) {
                // success response
                Map<String, Object> resp = new HashMap<>();
                resp.put("driverMember", driverMember);
                resp.put("driverId", candidate.get("driverId"));
                resp.put("reservedFor", "ride:" + rideId);
                resp.put("ttlSeconds", reserveTtlSeconds);
                resp.put("requestedByUserId", callerUserId);
                return ResponseEntity.ok(resp);
            }
        }

        // nothing could be reserved
        Map<String, Object> body = new HashMap<>();
        body.put("message", "No available drivers could be reserved");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }
}
