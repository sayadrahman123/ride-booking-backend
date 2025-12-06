package com.example.ridebooking.service.impl;

import com.example.ridebooking.dto.DriverLocationUpdateRequest;
import com.example.ridebooking.dto.DriverOnboardRequest;
import com.example.ridebooking.dto.DriverStatusUpdateRequest;
import com.example.ridebooking.entity.Driver;
import com.example.ridebooking.entity.Vehicle;
import com.example.ridebooking.repository.DriverRepository;
import com.example.ridebooking.repository.VehicleRepository;
import com.example.ridebooking.service.DriverService;
import com.example.ridebooking.service.RedisLocationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DriverServiceImpl implements DriverService {

    private final DriverRepository driverRepo;
    private final VehicleRepository vehicleRepo;
    private final RedisLocationService redisLocationService;
    private final com.example.ridebooking.repository.UserRepository userRepository;

    public DriverServiceImpl(DriverRepository driverRepo,
                             VehicleRepository vehicleRepo,
                             RedisLocationService redisLocationService,
                             com.example.ridebooking.repository.UserRepository userRepository) {
        this.driverRepo = driverRepo;
        this.vehicleRepo = vehicleRepo;
        this.redisLocationService = redisLocationService;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public Driver onboard(DriverOnboardRequest req) {
        // check user exists and is ROLE_DRIVER
        var userOpt = userRepository.findById(req.getUserId());
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found with id: " + req.getUserId());
        }
        var user = userOpt.get();
        if (user.getRole() != com.example.ridebooking.entity.Role.ROLE_DRIVER) {
            throw new IllegalArgumentException("User must have ROLE_DRIVER to onboard as driver");
        }

        if (driverRepo.findByUserId(req.getUserId()).isPresent()) {
            throw new IllegalArgumentException("Driver already onboarded for this user");
        }

        Driver d = new Driver();
        d.setUserId(req.getUserId());
        d.setLicenseNumber(req.getLicenseNumber());
        d.setActive(false);
        Driver saved = driverRepo.save(d);

        // vehicle
        Vehicle v = new Vehicle();
        v.setDriverId(saved.getId());
        v.setMake(req.getMake());
        v.setModel(req.getModel());
        v.setPlateNumber(req.getPlateNumber());
        v.setCapacity(req.getCapacity() == null ? 4 : req.getCapacity());
        vehicleRepo.save(v);

        return saved;
    }

    @Override
    public Vehicle saveOrUpdateVehicle(Long driverId, DriverOnboardRequest req) {
        Vehicle v = vehicleRepo.findByDriverId(driverId).orElseGet(Vehicle::new);
        v.setDriverId(driverId);
        v.setMake(req.getMake());
        v.setModel(req.getModel());
        v.setPlateNumber(req.getPlateNumber());
        v.setCapacity(req.getCapacity() == null ? 4 : req.getCapacity());
        return vehicleRepo.save(v);
    }

    @Override
    @Transactional
    public Driver updateStatus(Long driverId, DriverStatusUpdateRequest req) {
        Driver d = driverRepo.findById(driverId).orElseThrow(() -> new IllegalArgumentException("Driver not found"));
        d.setActive(req.isAvailable());
        Driver saved = driverRepo.save(d);

        // set in Redis
        redisLocationService.setDriverAvailability(driverId, req.isAvailable());
        // also update location if provided
        if (req.isAvailable() && req.getLat() != null && req.getLng() != null) {
            redisLocationService.updateDriverLocation(driverId, req.getLat(), req.getLng());
        } else if (!req.isAvailable()) {
            redisLocationService.removeDriverLocation(driverId);
        }
        return saved;
    }

    @Override
    public void updateLocation(Long driverId, DriverLocationUpdateRequest req) {
        // Ensure the driver exists and is active
        Driver d = driverRepo.findById(driverId).orElseThrow(() -> new IllegalArgumentException("Driver not found"));
        if (Boolean.TRUE.equals(d.getActive())) {
            redisLocationService.updateDriverLocation(driverId, req.getLat(), req.getLng());
        } else {
            // still update location in Redis but mark not available (optional)
            redisLocationService.updateDriverLocation(driverId, req.getLat(), req.getLng());
            redisLocationService.setDriverAvailability(driverId, false);
        }
    }
}
