package com.example.ridebooking.repository;

import com.example.ridebooking.entity.Ride;
import com.example.ridebooking.entity.RideStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RideRepository extends JpaRepository<Ride, Long> {
    Optional<Ride> findByExternalId(String externalId);

    Optional<Ride> findById(Long id);

    Page<Ride> findAll(Pageable pageable);

    List<Ride> findByStatusIn(List<RideStatus> statuses);

}
