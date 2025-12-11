package com.example.ridebooking.fare;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DefaultFareEngineTest {

    // create an instance with known params
    private final DefaultFareEngine engine = new DefaultFareEngine(
            BigDecimal.valueOf(40.00),   // base fare
            BigDecimal.valueOf(10.00),   // per km
            BigDecimal.valueOf(1.00),    // per min
            BigDecimal.valueOf(5.00),    // booking fee
            BigDecimal.valueOf(50.00)    // minimum fare
    );

    @Test
    void zeroDistanceZeroTime_returnsAtLeastMinimumFare() {
        FareResult r = engine.calculate(0L, 0L, 1.0);
        assertNotNull(r);
        // Minimum fare is 50 INR -> 5000 cents
        assertEquals(5000L, r.getFareCents());
        Map<String, Object> b = r.getBreakdown();
        assertEquals("50.00", b.get("finalFare")); // finalFare shown as string "50.00"
    }

    @Test
    void normalRide_computesExpectedFare() {
        // distance 4500 m -> 4.5 km; duration 900s -> 15 min; surge 1.0
        FareResult r = engine.calculate(4500L, 900L, 1.0);
        // manual calc:
        // base 40 + distance(4.5*10=45) + time(15*1=15) + booking 5 = 105
        // final = 105 (above minimum 50) -> 10500 cents
        assertEquals(10500L, r.getFareCents());
        Map<String, Object> b = r.getBreakdown();
        assertEquals("105.00", b.get("fareBeforeMin"));
        assertEquals("105.00", b.get("finalFare"));
    }

    @Test
    void surgeApplied_increasesFare() {
        // same ride but with surge 1.5 -> before surge 105 -> after surge 157.5 -> rounded 157.50
        FareResult r = engine.calculate(4500L, 900L, 1.5);
        assertEquals(15750L, r.getFareCents());
        Map<String, Object> b = r.getBreakdown();
        assertEquals(1.5, (Double) b.get("surgeMultiplier"));
        assertEquals("157.50", b.get("fareBeforeMin"));
        assertEquals("157.50", b.get("finalFare"));
    }

    @Test
    void verySmallRide_respectsMinimumFare() {
        // small ride with distance 100m and 30 seconds
        FareResult r = engine.calculate(100L, 30L, 1.0);
        // charges: base 40 + distance(0.1*10=1) + time(0.5*1=0.5) + booking 5 = 46.5 -> rounds 46.50 -> min enforces 50
        assertEquals(5000L, r.getFareCents());
    }
}
