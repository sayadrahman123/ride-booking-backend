package com.example.ridebooking.controller;

import com.example.ridebooking.dto.ReceiptResponse;
import com.example.ridebooking.dto.RideRecordResponse;
import com.example.ridebooking.entity.RideRecord;
import com.example.ridebooking.repository.RideRecordRepository;
import com.example.ridebooking.util.AuthUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rides")
public class RideHistoryController {

    private final RideRecordRepository rideRecordRepository;
    private final ObjectMapper objectMapper;

    public RideHistoryController(RideRecordRepository rideRecordRepository, ObjectMapper objectMapper) {
        this.rideRecordRepository = rideRecordRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * GET /api/rides/my
     * Returns paged list of rides for current authenticated rider.
     */
    @GetMapping("/my")
    public ResponseEntity<?> myRides(Pageable pageable) {
        Long userId = AuthUtils.requireCurrentUserId();
        List<RideRecord> all = rideRecordRepository.findByRiderIdOrderByCreatedAtDesc(userId);

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), all.size());
        List<RideRecord> subset = (start <= end) ? all.subList(start, end) : Collections.emptyList();

        List<RideRecordResponse> resp = subset.stream().map(r -> toResponse(r)).collect(Collectors.toList());
        Page<RideRecordResponse> page = new PageImpl<>(resp, pageable, all.size());
        return ResponseEntity.ok(page);
    }

    /**
     * GET /api/rides/{rideId}
     * Returns details for a ride (if owned by current user)
     */
    @GetMapping("/{rideId}")
    public ResponseEntity<?> getRide(@PathVariable String rideId) {
        Long userId = AuthUtils.requireCurrentUserId();
        RideRecord rec = rideRecordRepository.findByRideId(rideId)
                .orElseThrow(() -> new NoSuchElementException("Ride not found"));

        if (!Objects.equals(rec.getRiderId(), userId) && !Objects.equals(rec.getDriverId(), userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "forbidden"));
        }

        return ResponseEntity.ok(toResponse(rec));
    }

    /**
     * GET /api/rides/{rideId}/receipt
     * Returns a friendly receipt object with human readable amount.
     */
    @GetMapping("/{rideId}/receipt")
    public ResponseEntity<?> getReceipt(@PathVariable String rideId) {
        Long userId = AuthUtils.requireCurrentUserId();
        RideRecord rec = rideRecordRepository.findByRideId(rideId)
                .orElseThrow(() -> new NoSuchElementException("Ride not found"));

        if (!Objects.equals(rec.getRiderId(), userId) && !Objects.equals(rec.getDriverId(), userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "forbidden"));
        }

        ReceiptResponse receipt = new ReceiptResponse();
        receipt.setRideId(rec.getRideId());
        receipt.setRiderId(rec.getRiderId());
        receipt.setDriverId(rec.getDriverId());
        receipt.setStartTime(rec.getStartTime());
        receipt.setEndTime(rec.getEndTime());
        receipt.setDistanceMeters(rec.getDistanceMeters());
        receipt.setDurationSeconds(rec.getDurationSeconds());
        receipt.setFareCents(rec.getFareCents());
        receipt.setCurrency(rec.getCurrency());

        // parse breakdown json
        try {
            Map<String,Object> breakdown = objectMapper.readValue(rec.getFareBreakdownJson(), Map.class);
            receipt.setFareBreakdown(breakdown);
        } catch (Exception ex) {
            receipt.setFareBreakdown(Map.of("raw", rec.getFareBreakdownJson() == null ? "" : rec.getFareBreakdownJson()));
        }

        BigDecimal amt = BigDecimal.valueOf(rec.getFareCents()).divide(BigDecimal.valueOf(100));
        receipt.setHumanReadableAmount(String.format("₹%s", amt.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString()));

        return ResponseEntity.ok(receipt);
    }

    // --- mapper
    private RideRecordResponse toResponse(RideRecord r) {
        RideRecordResponse rr = new RideRecordResponse();
        rr.setRideId(r.getRideId());
        rr.setRiderId(r.getRiderId());
        rr.setDriverId(r.getDriverId());
        rr.setStartTime(r.getStartTime());
        rr.setEndTime(r.getEndTime());
        rr.setDistanceMeters(r.getDistanceMeters());
        rr.setDurationSeconds(r.getDurationSeconds());
        rr.setFareCents(r.getFareCents() == null ? 0L : r.getFareCents());
        rr.setCurrency(r.getCurrency());
        try {
            Map<String,Object> breakdown = objectMapper.readValue(r.getFareBreakdownJson() == null ? "{}" : r.getFareBreakdownJson(), Map.class);
            rr.setFareBreakdown(breakdown);
        } catch (Exception ex) {
            rr.setFareBreakdown(Map.of("raw", r.getFareBreakdownJson() == null ? "" : r.getFareBreakdownJson()));
        }
        return rr;
    }
}
