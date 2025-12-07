package com.example.ridebooking.service.impl;

import com.example.ridebooking.entity.Driver;
import com.example.ridebooking.entity.Ride;
import com.example.ridebooking.entity.RideStatus;
import com.example.ridebooking.repository.DriverRepository;
import com.example.ridebooking.repository.RideRepository;
import com.example.ridebooking.redis.RedisKeys;
import com.example.ridebooking.service.RideService;
import com.example.ridebooking.service.RedisMatchService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ridebooking.events.RideEventPublisher;
import com.example.ridebooking.events.RideEvent;
import java.time.Instant;


import java.util.Optional;

@Service
public class RideServiceImpl implements RideService {

    private final RideRepository rideRepository;
    private final RedisMatchService redisMatchService;
    private final DriverRepository driverRepository;
    private final RideEventPublisher rideEventPublisher;

    public RideServiceImpl(RideRepository rideRepository,
                           RedisMatchService redisMatchService,
                           DriverRepository driverRepository,
                           RideEventPublisher rideEventPublisher) {
        this.rideRepository = rideRepository;
        this.redisMatchService = redisMatchService;
        this.driverRepository = driverRepository;
        this.rideEventPublisher = rideEventPublisher;
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

}
