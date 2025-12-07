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

import java.time.Instant;
import java.util.Optional;

@Service
public class RideServiceImpl implements RideService {

    private final RideRepository rideRepository;
    private final RedisMatchService redisMatchService;
    private final DriverRepository driverRepository;

    public RideServiceImpl(RideRepository rideRepository,
                           RedisMatchService redisMatchService,
                           DriverRepository driverRepository) {
        this.rideRepository = rideRepository;
        this.redisMatchService = redisMatchService;
        this.driverRepository = driverRepository;
    }

    /**
     * Accept ride: validate Redis lock, create Ride, remove lock and mark driver inactive.
     */
    @Override
    @Transactional
    public Ride acceptRide(String rideExternalId, Long driverId) {
        // Validate lock - redisMatchService has helper to read reservation value
        String current = redisMatchService.getDriverReservation(String.valueOf(driverId));
        String expected = "ride:" + rideExternalId;
        if (current == null || !current.equals(expected)) {
            throw new IllegalStateException("No reservation found for this driver & ride");
        }

        // Create or update Ride record
        Optional<Ride> existing = rideRepository.findByExternalId(rideExternalId);
        Ride ride = existing.orElseGet(Ride::new);
        ride.setExternalId(rideExternalId);
        ride.setDriverId(driverId);
        // riderId may be null if not provided earlier
        ride.setStatus(RideStatus.ACCEPTED);
        ride.setAcceptedAt(Instant.now());
        if (ride.getCreatedAt() == null) ride.setCreatedAt(Instant.now());

        Ride saved = rideRepository.save(ride);

        // Remove lock from Redis (reservation consumed)
        redisMatchService.releaseDriverReservationByDriverId(String.valueOf(driverId));

        // mark driver inactive / assigned
        Driver driver = driverRepository.findById(driverId).orElse(null);
        if (driver != null) {
            driver.setActive(false);
            driverRepository.save(driver);
        }

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
            return true;
        }
        return false;
    }
}
