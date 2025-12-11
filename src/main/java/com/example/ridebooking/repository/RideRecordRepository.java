package com.example.ridebooking.repository;

import com.example.ridebooking.entity.RideRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RideRecordRepository extends JpaRepository<RideRecord, Long> {

    Optional<RideRecord> findByRideId(String rideId);

    List<RideRecord> findByRiderIdOrderByCreatedAtDesc(Long riderId);
}
