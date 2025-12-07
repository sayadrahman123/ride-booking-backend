package com.example.ridebooking.controller;

import com.example.ridebooking.entity.Ride;
import com.example.ridebooking.repository.DriverRepository;
import com.example.ridebooking.security.AuthUtils;
import com.example.ridebooking.service.RideService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/ride")
public class RideController {

    private final RideService rideService;
    private final AuthUtils authUtils;
    private final DriverRepository driverRepository;

    public RideController(RideService rideService, AuthUtils authUtils, DriverRepository driverRepository) {
        this.rideService = rideService;
        this.authUtils = authUtils;
        this.driverRepository = driverRepository;
    }

    /**
     * Driver accepts the ride assigned to them by reservation.
     * The driver is inferred from the JWT (AuthUtils).
     */
    @PostMapping("/{rideId}/accept")
    @PreAuthorize("hasAuthority('ROLE_DRIVER')")
    public ResponseEntity<?> accept(@PathVariable String rideId) {
        // infer current user -> driverId
        Long userId = authUtils.requireCurrentUserId();
        Long driverId = driverRepository.findByUserId(userId)
                .map(d -> d.getId())
                .orElseThrow(() -> new IllegalStateException("Driver profile not found for user"));

        try {
            Ride ride = rideService.acceptRide(rideId, driverId);
            return ResponseEntity.ok(Map.of(
                    "rideExternalId", ride.getExternalId(),
                    "rideDbId", ride.getId(),
                    "status", ride.getStatus()
            ));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(Map.of("error", ex.getMessage()));
        }
    }

    /**
     * Driver rejects the ride. This removes the Redis reservation lock so matching can continue.
     */
    @PostMapping("/{rideId}/reject")
    @PreAuthorize("hasAuthority('ROLE_DRIVER')")
    public ResponseEntity<?> reject(@PathVariable String rideId) {
        Long userId = authUtils.requireCurrentUserId();
        Long driverId = driverRepository.findByUserId(userId)
                .map(d -> d.getId())
                .orElseThrow(() -> new IllegalStateException("Driver profile not found for user"));

        boolean removed = rideService.rejectRide(rideId, driverId);
        if (removed) {
            return ResponseEntity.ok(Map.of("result", "reservation_removed"));
        } else {
            return ResponseEntity.status(404).body(Map.of("error", "no matching reservation found"));
        }
    }
}
