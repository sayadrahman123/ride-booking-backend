package com.example.ridebooking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "ride_records")
public class RideRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String rideId;

    @Column(nullable = false)
    private Long riderId;

    @Column(nullable = false)
    private Long driverId;

    @Column
    private Instant startTime;

    @Column
    private Instant endTime;

    @Column
    private Long distanceMeters;

    @Column
    private Long durationSeconds;

    // Store fare in cents to avoid float issues
    @Column
    private Long fareCents;

    @Column(length = 8)
    private String currency = "INR";

    // JSON stored as TEXT
    @Lob
    private String fareBreakdownJson;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
