package com.example.ridebooking.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "rides", indexes = {
        @Index(name = "idx_rides_external_id", columnList = "external_id", unique = true)
})
public class Ride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // external id (the 'rideId' used in matching)
    @Column(name = "external_id", nullable = false, unique = true)
    private String externalId;

    private Long riderId;   // optional (if known)
    private Long driverId;  // assigned driver

    @Enumerated(EnumType.STRING)
    private RideStatus status;

    private Instant createdAt = Instant.now();
    private Instant acceptedAt;
    private Instant startedAt;
    private Instant completedAt;

    // additional fields (pickup/drop coordinates) can be added later
    // getters / setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public RideStatus getRideStatus() { return status; }
    public void setRideStatus(RideStatus status) { this.status = status; }

    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }

    public Long getRiderId() { return riderId; }
    public void setRiderId(Long riderId) { this.riderId = riderId; }

    public Long getDriverId() { return driverId; }
    public void setDriverId(Long driverId) { this.driverId = driverId; }

    public RideStatus getStatus() { return status; }
    public void setStatus(RideStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(Instant acceptedAt) { this.acceptedAt = acceptedAt; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
