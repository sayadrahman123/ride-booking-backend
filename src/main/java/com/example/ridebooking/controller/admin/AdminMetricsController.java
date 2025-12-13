package com.example.ridebooking.controller.admin;

import com.example.ridebooking.dto.AdminMetricsResponse;
import com.example.ridebooking.entity.RideStatus;
import com.example.ridebooking.repository.RideRecordRepository;
import com.example.ridebooking.repository.RideRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/metrics")
public class AdminMetricsController {

    private final RideRepository rideRepository;
    private final RideRecordRepository rideRecordRepository;

    public AdminMetricsController(RideRepository rideRepository,
                                  RideRecordRepository rideRecordRepository) {
        this.rideRepository = rideRepository;
        this.rideRecordRepository = rideRecordRepository;
    }

    @GetMapping("/summary")
    public ResponseEntity<AdminMetricsResponse> getSummary() {

        long totalRides = rideRepository.count();
        long activeRides = rideRepository.countByStatusIn(
                List.of(RideStatus.ACCEPTED, RideStatus.STARTED)
        );
        long completedRides = rideRepository.countByStatus(RideStatus.COMPLETED);

        Long totalRevenue = rideRecordRepository.sumTotalRevenue();
        Long avgFare = rideRecordRepository.avgFare();
        Long avgDuration = rideRecordRepository.avgDuration();

        AdminMetricsResponse resp = new AdminMetricsResponse();
        resp.setTotalRides(totalRides);
        resp.setActiveRides(activeRides);
        resp.setCompletedRides(completedRides);
        resp.setTotalRevenueCents(totalRevenue != null ? totalRevenue : 0);
        resp.setAverageFareCents(avgFare != null ? avgFare : 0);
        resp.setAverageDurationSeconds(avgDuration != null ? avgDuration : 0);

        return ResponseEntity.ok(resp);
    }
}
