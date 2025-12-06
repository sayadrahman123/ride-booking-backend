package com.example.ridebooking.controller;

import com.example.ridebooking.dto.*;
import com.example.ridebooking.entity.*;
import com.example.ridebooking.repository.*;
import com.example.ridebooking.security.AuthUtils;
import com.example.ridebooking.service.DriverService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    private final DriverService driverService;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final AuthUtils authUtils;

    public DriverController(DriverService driverService,
                            DriverRepository driverRepository,
                            VehicleRepository vehicleRepository,
                            UserRepository userRepository,
                            AuthUtils authUtils) {
        this.driverService = driverService;
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
        this.authUtils = authUtils;
    }

    /**
     * Onboard the currently authenticated user as a driver.
     * Requires the signed-in user to have ROLE_DRIVER.
     * The request no longer needs userId — we infer it from the token.
     */
    @PostMapping("/onboard")
    @PreAuthorize("hasAuthority('ROLE_DRIVER')")
    public ResponseEntity<?> onboard(@RequestBody DriverOnboardRequest req) {
        // infer userId from auth
        Long userId = authUtils.requireCurrentUserId();
        req.setUserId(userId);
        var created = driverService.onboard(req);
        return ResponseEntity.status(201).body(created.getId());
    }

    /**
     * Update availability + optional location.
     * Only the driver that owns this driverId can update status.
     */
    @PostMapping("/{driverId}/status")
    @PreAuthorize("hasAuthority('ROLE_DRIVER')")
    public ResponseEntity<?> updateStatus(@PathVariable Long driverId, @RequestBody DriverStatusUpdateRequest req) {
        // ensure the current user owns this driver record
        var driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found"));
        Long currentUserId = authUtils.requireCurrentUserId();
        if (!driver.getUserId().equals(currentUserId)) {
            return ResponseEntity.status(403).body("Forbidden: not your driver profile");
        }
        driverService.updateStatus(driverId, req);
        return ResponseEntity.ok().body("ok");
    }

    /**
     * Update location only.
     */
    @PostMapping("/{driverId}/location")
    @PreAuthorize("hasAuthority('ROLE_DRIVER')")
    public ResponseEntity<?> updateLocation(@PathVariable Long driverId, @RequestBody DriverLocationUpdateRequest req) {
        var driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found"));
        Long currentUserId = authUtils.requireCurrentUserId();
        if (!driver.getUserId().equals(currentUserId)) {
            return ResponseEntity.status(403).body("Forbidden: not your driver profile");
        }
        driverService.updateLocation(driverId, req);
        return ResponseEntity.ok().body("ok");
    }

    // get driver + vehicle info — allow admins or owner or public (you can adjust)
    @GetMapping("/{driverId}")
    public ResponseEntity<?> getDriver(@PathVariable Long driverId) {
        Driver d = driverRepository.findById(driverId).orElseThrow(() -> new IllegalArgumentException("Driver not found"));
        var vehicle = vehicleRepository.findByDriverId(driverId).orElse(null);
        var user = userRepository.findById(d.getUserId()).orElse(null);
        var resp = new java.util.HashMap<String, Object>();
        resp.put("driver", d);
        resp.put("vehicle", vehicle);
        resp.put("user", user);
        return ResponseEntity.ok(resp);
    }
}
