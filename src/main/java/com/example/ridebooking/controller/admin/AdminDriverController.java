package com.example.ridebooking.controller.admin;

import com.example.ridebooking.dto.AdminDriverStatusResponse;
import com.example.ridebooking.entity.Driver;
import com.example.ridebooking.entity.User;
import com.example.ridebooking.repository.DriverRepository;
import com.example.ridebooking.repository.UserRepository;
import com.example.ridebooking.service.RedisMatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/drivers")
public class AdminDriverController {

    private final DriverRepository driverRepository;
    private final RedisMatchService redisMatchService;
    private final UserRepository userRepository;

    public AdminDriverController(DriverRepository driverRepository,
                                 RedisMatchService redisMatchService,
                                 UserRepository userRepository) {
        this.driverRepository = driverRepository;
        this.redisMatchService = redisMatchService;
        this.userRepository = userRepository;
    }

    /**
     * GET /api/admin/drivers
     * Admin: view all drivers with live status
     */
    @GetMapping
    public ResponseEntity<List<AdminDriverStatusResponse>> getAllDrivers() {

        List<Driver> drivers = driverRepository.findAll();

        List<AdminDriverStatusResponse> response = drivers.stream().map(d -> {
            AdminDriverStatusResponse r = new AdminDriverStatusResponse();

            User user = userRepository.findById(d.getUserId()).orElse(null);

            r.setDriverId(d.getId());
            r.setName(user != null ? user.getName() : "UNKNOWN");
            r.setActive(d.getActive());

            boolean busy = redisMatchService.isDriverBusy(String.valueOf(d.getId()));
            boolean available = redisMatchService.isDriverAvailableById(String.valueOf(d.getId()));

            r.setBusy(busy);
            r.setAvailable(available);

            return r;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }


    /**
     * GET /api/admin/drivers/online
     */
    @GetMapping("/online")
    public ResponseEntity<List<AdminDriverStatusResponse>> getOnlineDrivers() {
        return ResponseEntity.ok(
                getAllDrivers().getBody().stream()
                        .filter(AdminDriverStatusResponse::isActive)
                        .collect(Collectors.toList())
        );
    }

    /**
     * GET /api/admin/drivers/busy
     */
    @GetMapping("/busy")
    public ResponseEntity<List<AdminDriverStatusResponse>> getBusyDrivers() {
        return ResponseEntity.ok(
                getAllDrivers().getBody().stream()
                        .filter(AdminDriverStatusResponse::isBusy)
                        .collect(Collectors.toList())
        );
    }
}
