package com.example.ridebooking.service.impl;

import com.example.ridebooking.entity.*;
import com.example.ridebooking.fare.FareEngine;
import com.example.ridebooking.fare.FareResult;
import com.example.ridebooking.repository.DriverRepository;
import com.example.ridebooking.repository.RideLocationRepository;
import com.example.ridebooking.repository.RideRecordRepository;
import com.example.ridebooking.repository.RideRepository;
import com.example.ridebooking.redis.RedisKeys;
import com.example.ridebooking.service.RideService;
import com.example.ridebooking.service.RedisMatchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ridebooking.events.RideEventPublisher;
import com.example.ridebooking.events.RideEvent;
import java.time.Instant;


import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class RideServiceImpl implements RideService {

    private final RideRepository rideRepository;
    private final RedisMatchService redisMatchService;
    private final DriverRepository driverRepository;
    private final RideEventPublisher rideEventPublisher;


    private final SimpMessagingTemplate simpMessagingTemplate;
    private final RideLocationRepository rideLocationRepository;

    private final FareEngine fareEngine;
    private final RideRecordRepository rideRecordRepository;
    private final ObjectMapper objectMapper;           // jackson


    public RideServiceImpl(RideRepository rideRepository,
                           RedisMatchService redisMatchService,
                           DriverRepository driverRepository,
//                           RideEventPublisher rideEventPublisher,
                           SimpMessagingTemplate simpMessagingTemplate,
                           RideLocationRepository rideLocationRepository,
                           FareEngine fareEngine,
                           RideRecordRepository rideRecordRepository,
                           ObjectMapper objectMapper,
//                           RideRepository rideRepository,
                           RideEventPublisher rideEventPublisher
    ) {
        this.rideRepository = rideRepository;
        this.redisMatchService = redisMatchService;
        this.driverRepository = driverRepository;
        this.rideEventPublisher = rideEventPublisher;

        this.simpMessagingTemplate = simpMessagingTemplate;
        this.rideLocationRepository = rideLocationRepository;

        this.fareEngine = fareEngine;
        this.rideRecordRepository = rideRecordRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Accept ride: validate Redis lock, create Ride, remove lock and mark driver inactive.
     */
    @Override
    @Transactional
    public Ride acceptRide(String rideExternalId, Long driverId) {
        String current = redisMatchService.getDriverReservation(String.valueOf(driverId));
        String expected = "ride:" + rideExternalId;
        if (current == null || !current.equals(expected)) {
            throw new IllegalStateException("No reservation found for this driver & ride");
        }

        Optional<Ride> existing = rideRepository.findByExternalId(rideExternalId);
        Ride ride = existing.orElseGet(Ride::new);
        ride.setExternalId(rideExternalId);
        ride.setDriverId(driverId);
        ride.setStatus(RideStatus.ACCEPTED);
        ride.setAcceptedAt(Instant.now());
        if (ride.getCreatedAt() == null) ride.setCreatedAt(Instant.now());

        Ride saved = rideRepository.save(ride);

        // Remove lock and mark driver inactive
        redisMatchService.releaseDriverReservationByDriverId(String.valueOf(driverId));
        Driver driver = driverRepository.findById(driverId).orElse(null);
        if (driver != null) {
            driver.setActive(false);
            driverRepository.save(driver);
        }

        // Publish Kafka event (Uber-style)
        RideEvent event = new RideEvent(
                "ride.accepted",
                rideExternalId,
                driverId,
                ride.getRiderId(),
                ride.getStatus().name(),
                Instant.now()
        );
        rideEventPublisher.publish(event);

        return saved;
    }

    /**
     * Reject ride: if the lock belongs to this driver+ride, delete it and return true.
     */
    @Override
    @Transactional
    public boolean rejectRide(String rideExternalId, Long driverId) {
        String current = redisMatchService.getDriverReservation(String.valueOf(driverId));
        String expected = "ride:" + rideExternalId;
        if (current != null && current.equals(expected)) {
            redisMatchService.releaseDriverReservationByDriverId(String.valueOf(driverId));

            // Publish rejected event
            RideEvent event = new RideEvent(
                    "ride.rejected",
                    rideExternalId,
                    driverId,
                    null,
                    "REJECTED",
                    Instant.now()
            );
            rideEventPublisher.publish(event);

            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public Ride startRide(String rideExternalId, Long driverId) {
        Ride ride = rideRepository.findByExternalId(rideExternalId).orElseThrow(() -> new IllegalStateException("Ride not found"));
        ride.setStatus(RideStatus.STARTED);
        ride.setStartedAt(Instant.now());
        Ride saved = rideRepository.save(ride);

        RideEvent ev = new RideEvent("ride.started", rideExternalId, driverId, ride.getRiderId(), "STARTED", Instant.now());
        rideEventPublisher.publish(ev);

        // broadcast to websocket topic /topic/ride.{rideId}
        Map<String, Object> payload = Map.of(
                "event", "ride.started",
                "rideId", rideExternalId,
                "driverId", driverId,
                "ts", Instant.now().toString()
        );
        simpMessagingTemplate.convertAndSend("/topic/ride." + rideExternalId, payload);

        return saved;
    }

    @Override
    @Transactional
    public void updateRideLocation(String rideExternalId, Long driverId, Double lat, Double lng, Double speedMps) {
        // persist location
        RideLocation loc = new RideLocation();
        loc.setRideId(rideExternalId);
        loc.setDriverId(driverId);
        loc.setLatitude(lat);
        loc.setLongitude(lng);
        loc.setSpeed(speedMps);
        loc.setTimestamp(Instant.now());
        rideLocationRepository.save(loc);

        // publish Kafka event
        RideEvent ev = new RideEvent("ride.location.updated", rideExternalId, driverId, null, "LOCATION", Instant.now());
        rideEventPublisher.publish(ev);

        // push via websocket to subscribers of this ride
        Map<String, Object> payload = Map.of(
                "event", "ride.location.updated",
                "rideId", rideExternalId,
                "driverId", driverId,
                "lat", lat,
                "lng", lng,
                "speedMps", speedMps,
                "ts", Instant.now().toString()
        );
        simpMessagingTemplate.convertAndSend("/topic/ride." + rideExternalId, payload);
    }

    /**
     * Mark ride completed: calculate fare, persist ride record and publish completed event.
     *
     * @param rideExternalId        external ride id (eg "ride-101")
     * @param distanceMeters distance in meters
     * @param durationSeconds duration in seconds
//     * @param surgeMultiplier surge multiplier (1.0 = normal)
     * @return saved RideRecord
     */
    @Override
    @Transactional
    public RideRecord completeRide(String rideExternalId,
                                   Long driverId,
                                   Long distanceMeters,
                                   long durationSeconds) {

        // 0) defensive defaults
        long distMeters = (distanceMeters != null) ? distanceMeters : 0L;
        double surgeMultiplier = 1.0; // no surge in interface; default to normal

        // 1) load ride by externalId
        Ride ride = rideRepository.findByExternalId(rideExternalId)
                .orElseThrow(() -> new IllegalArgumentException("Ride not found: " + rideExternalId));

        // 2) calculate fare (adapt if your FareEngine signature differs)
        // If your FareEngine.calculate(...) requires different types, adapt the call.
        FareResult fareResult = fareEngine.calculate(distMeters, durationSeconds, surgeMultiplier);
        long fareCents = fareResult.getFareCents();

        // 3) serialize breakdown safely
        String breakdownJson;
        try {
            breakdownJson = objectMapper.writeValueAsString(fareResult.getBreakdown());
        } catch (Exception e) {
            breakdownJson = fareResult.getBreakdown().toString();
        }

        // 4) build and save RideRecord
        RideRecord record = RideRecord.builder()
                .rideId(rideExternalId)
                .riderId(ride.getRiderId())
                .driverId(ride.getDriverId())
                .startTime(ride.getStartedAt())
                .endTime(Instant.now())
                .distanceMeters(distMeters)
                .durationSeconds(durationSeconds)
                .fareCents(fareCents)
                .fareBreakdownJson(breakdownJson)
                .currency("INR")
                .createdAt(Instant.now())
                .build();

        rideRecordRepository.save(record);

        // 5) update ride status to COMPLETED
        ride.setStatus(RideStatus.COMPLETED);
        rideRepository.save(ride);

        // 6) publish ride.completed event
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("rideId", rideExternalId);
        payload.put("driverId", ride.getDriverId());
        payload.put("distanceMeters", distMeters);
        payload.put("durationSeconds", durationSeconds);
        payload.put("fareCents", fareCents);
        payload.put("fareBreakdown", fareResult.getBreakdown());
        payload.put("ts", Instant.now().toString());

        // Use builder or constructor depending on your RideEvent class — using builder here
        RideEvent event = RideEvent.builder()
                .eventType("ride.completed")
                .rideId(rideExternalId)
                .payload(payload)
                .build();

        rideEventPublisher.publish(event);

        return record;
    }



}
