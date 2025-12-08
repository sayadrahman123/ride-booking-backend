package com.example.ridebooking.repository;

import com.example.ridebooking.entity.RideLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RideLocationRepository extends JpaRepository<RideLocation, Long> {
    List<RideLocation> findByRideIdOrderByTimestampAsc(String rideId);
}
