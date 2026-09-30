
package com.alexbalutin.smartbattery.analysis;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HighDrainDetectorTest {

    private final HighDrainDetector detector = new HighDrainDetector();

    @Test
    void detectsRateAboveAbsoluteAndStatisticalThresholds() {
        HighDrainDetector.Decision decision = detector.evaluate(
                18.0,
                List.of(5.5, 6.0, 6.2, 5.8, 6.1),
                10.0,
                1.5,
                3);

        assertTrue(decision.highDrain());
    }

    @Test
    void rejectsRateThatIsElevatedButBelowAbsoluteMinimum() {
        HighDrainDetector.Decision decision = detector.evaluate(
                8.0,
                List.of(2.0, 2.2, 2.1, 2.3),
                10.0,
                1.5,
                3);

        assertFalse(decision.highDrain());
    }

    @Test
    void usesAbsoluteThresholdWhenHistoryIsInsufficient() {
        HighDrainDetector.Decision decision = detector.evaluate(
                12.0,
                List.of(5.0),
                10.0,
                1.5,
                3);

        assertTrue(decision.highDrain());
    }
}
