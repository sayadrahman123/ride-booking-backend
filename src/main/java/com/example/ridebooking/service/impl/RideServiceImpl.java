package com.example.ridebooking.service.impl;

import com.example.ridebooking.entity.Driver;
import com.example.ridebooking.entity.Ride;
import com.example.ridebooking.entity.RideLocation;
import com.example.ridebooking.entity.RideStatus;
import com.example.ridebooking.repository.DriverRepository;
import com.example.ridebooking.repository.RideLocationRepository;
import com.example.ridebooking.repository.RideRepository;
import com.example.ridebooking.redis.RedisKeys;
import com.example.ridebooking.service.RideService;
import com.example.ridebooking.service.RedisMatchService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ridebooking.events.RideEventPublisher;
import com.example.ridebooking.events.RideEvent;
import java.time.Instant;


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

    public RideServiceImpl(RideRepository rideRepository,
                           RedisMatchService redisMatchService,
                           DriverRepository driverRepository,
                           RideEventPublisher rideEventPublisher, SimpMessagingTemplate simpMessagingTemplate, RideLocationRepository rideLocationRepository) {
        this.rideRepository = rideRepository;
        this.redisMatchService = redisMatchService;
        this.driverRepository = driverRepository;
        this.rideEventPublisher = rideEventPublisher;

        this.simpMessagingTemplate = simpMessagingTemplate;
        this.rideLocationRepository = rideLocationRepository;
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

    @Override
    @Transactional
    public Ride completeRide(String rideExternalId, Long driverId, Long distanceMeters, Long durationSeconds) {
        Ride ride = rideRepository.findByExternalId(rideExternalId).orElseThrow(() -> new IllegalStateException("Ride not found"));
        ride.setStatus(RideStatus.COMPLETED);
        ride.setCompletedAt(Instant.now());
        // optionally store metrics on Ride (add fields if you want)
        Ride saved = rideRepository.save(ride);

        RideEvent ev = new RideEvent("ride.completed", rideExternalId, driverId, ride.getRiderId(), "COMPLETED", Instant.now());
        rideEventPublisher.publish(ev);

        Map<String, Object> payload = Map.of(
                "event", "ride.completed",
                "rideId", rideExternalId,
                "driverId", driverId,
                "distanceMeters", distanceMeters,
                "durationSeconds", durationSeconds,
                "ts", Instant.now().toString()
        );
        simpMessagingTemplate.convertAndSend("/topic/ride." + rideExternalId, payload);

        return saved;
    }

}
