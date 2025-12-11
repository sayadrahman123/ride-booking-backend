package com.example.ridebooking.fare;

import java.util.Map;

public class FareResult {
    private final long fareCents;
    private final Map<String, Object> breakdown;

    public FareResult(long fareCents, Map<String, Object> breakdown) {
        this.fareCents = fareCents;
        this.breakdown = breakdown;
    }

    public long getFareCents() {
        return fareCents;
    }

    public Map<String, Object> getBreakdown() {
        return breakdown;
    }
}
