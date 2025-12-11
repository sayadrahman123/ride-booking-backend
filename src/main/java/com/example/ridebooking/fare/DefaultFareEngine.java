package com.example.ridebooking.fare;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Default fare engine implementation.
 *
 * Currency and units:
 * - Input: distanceMeters (long), durationSeconds (long), surgeMultiplier (double)
 * - Money arithmetic uses BigDecimal; final stored value returned in cents (long).
 */
@Service
public class DefaultFareEngine implements FareEngine {

    // configured in application.properties (INR assumed)
    private final BigDecimal baseFare;      // e.g. 40.00 INR
    private final BigDecimal perKm;         // e.g. 10.00 INR per km
    private final BigDecimal perMin;        // e.g. 1.00 INR per minute
    private final BigDecimal bookingFee;    // e.g. 5.00 INR fixed
    private final BigDecimal minimumFare;   // e.g. 50.00 INR
    private int roundingScale = 2;    // decimals to keep
    private RoundingMode roundingMode = RoundingMode.HALF_UP;

    /**
     * Constructor used by Spring (reads properties).
     */
    @Autowired
    public DefaultFareEngine(
            @Value("${fare.base:40.00}") BigDecimal baseFare,
            @Value("${fare.perKm:10.00}") BigDecimal perKm,
            @Value("${fare.perMin:1.00}") BigDecimal perMin,
            @Value("${fare.bookingFee:5.00}") BigDecimal bookingFee,
            @Value("${fare.minimum:50.00}") BigDecimal minimumFare
    ) {
        this.baseFare = baseFare;
        this.perKm = perKm;
        this.perMin = perMin;
        this.bookingFee = bookingFee;
        this.minimumFare = minimumFare;
    }

    /**
     * Static factory for tests (avoids duplicate constructor signature).
     */
    public static DefaultFareEngine testInstance(BigDecimal baseFare,
                                                 BigDecimal perKm,
                                                 BigDecimal perMin,
                                                 BigDecimal bookingFee,
                                                 BigDecimal minimumFare) {
        return new DefaultFareEngine(baseFare, perKm, perMin, bookingFee, minimumFare, 2, RoundingMode.HALF_UP);
    }

    // private constructor used by the test factory
    private DefaultFareEngine(BigDecimal baseFare,
                              BigDecimal perKm,
                              BigDecimal perMin,
                              BigDecimal bookingFee,
                              BigDecimal minimumFare,
                              int roundingScale,
                              RoundingMode roundingMode) {
        this.baseFare = baseFare;
        this.perKm = perKm;
        this.perMin = perMin;
        this.bookingFee = bookingFee;
        this.minimumFare = minimumFare;
        this.roundingScale = roundingScale;
        this.roundingMode = roundingMode;
    }


    @Override
    public FareResult calculate(long distanceMeters, long durationSeconds, double surgeMultiplier) {
        if (surgeMultiplier <= 0) surgeMultiplier = 1.0;

        // Convert to BigDecimal units
        BigDecimal distanceKm = BigDecimal.valueOf(distanceMeters).divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP);
        BigDecimal timeMinutes = BigDecimal.valueOf(durationSeconds).divide(BigDecimal.valueOf(60), 6, RoundingMode.HALF_UP);

        // Charges
        BigDecimal distanceCharge = distanceKm.multiply(perKm);
        BigDecimal timeCharge = timeMinutes.multiply(perMin);

        // Sum base components
        BigDecimal subtotal = baseFare.add(distanceCharge).add(timeCharge).add(bookingFee);

        // Apply surge
        BigDecimal afterSurge = subtotal.multiply(BigDecimal.valueOf(surgeMultiplier));

        // Round to 2 decimals
        BigDecimal rounded = afterSurge.setScale(roundingScale, roundingMode);

        // Enforce minimum
        BigDecimal finalFare = rounded.max(minimumFare);

        // Convert to cents (multiply by 100)
        long fareCents = finalFare.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValueExact();

        // Build breakdown map (ordered)
        Map<String, Object> breakdown = new LinkedHashMap<>();
        breakdown.put("baseFare", baseFare.setScale(roundingScale, roundingMode).toPlainString());
        breakdown.put("distanceKm", distanceKm.setScale(3, RoundingMode.HALF_UP).toPlainString());
        breakdown.put("distanceCharge", distanceCharge.setScale(roundingScale, roundingMode).toPlainString());
        breakdown.put("timeMin", timeMinutes.setScale(3, RoundingMode.HALF_UP).toPlainString());
        breakdown.put("timeCharge", timeCharge.setScale(roundingScale, roundingMode).toPlainString());
        breakdown.put("bookingFee", bookingFee.setScale(roundingScale, roundingMode).toPlainString());
        breakdown.put("surgeMultiplier", surgeMultiplier);
        breakdown.put("fareBeforeMin", rounded.setScale(roundingScale, roundingMode).toPlainString());
        breakdown.put("finalFare", finalFare.setScale(roundingScale, roundingMode).toPlainString());
        breakdown.put("currency", "INR");

        return new FareResult(fareCents, breakdown);
    }

}
