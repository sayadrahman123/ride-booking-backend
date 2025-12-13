package com.example.ridebooking.repository;

import com.example.ridebooking.entity.RideRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RideRecordRepository extends JpaRepository<RideRecord, Long> {

    Optional<RideRecord> findByRideId(String rideId);

    List<RideRecord> findByRiderIdOrderByCreatedAtDesc(Long riderId);

    @Query("select coalesce(sum(r.fareCents),0) from RideRecord r")
    Long sumTotalRevenue();

    @Query("select coalesce(avg(r.fareCents),0) from RideRecord r")
    Long avgFare();

    @Query("select coalesce(avg(r.durationSeconds),0) from RideRecord r")
    Long avgDuration();

}
