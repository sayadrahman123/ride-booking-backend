package com.example.ridebooking.repository;

import com.example.ridebooking.entity.Ride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RideRepository extends JpaRepository<Ride, Long> {
    Optional<Ride> findByExternalId(String externalId);
}
