package com.example.ridebooking.controller.admin;

import com.example.ridebooking.entity.Ride;
import com.example.ridebooking.entity.RideStatus;
import com.example.ridebooking.repository.RideRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/rides")
public class AdminRideController {

    private final RideRepository rideRepository;

    public AdminRideController(RideRepository rideRepository) {
        this.rideRepository = rideRepository;
    }

    /**
     * GET /api/admin/rides
     * Admin: view all rides (paginated)
     */
    @GetMapping
    public ResponseEntity<Page<Ride>> getAllRides(Pageable pageable) {
        return ResponseEntity.ok(rideRepository.findAll(pageable));
    }

    /**
     * GET /api/admin/rides/active
     * Admin: view currently active rides
     */
    @GetMapping("/active")
    public ResponseEntity<List<Ride>> getActiveRides() {
        List<Ride> active = rideRepository.findByStatusIn(
                List.of(RideStatus.ACCEPTED, RideStatus.STARTED)
        );
        return ResponseEntity.ok(active);
    }
}
