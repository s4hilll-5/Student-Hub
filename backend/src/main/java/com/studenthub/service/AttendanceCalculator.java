package com.studenthub.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.studenthub.dto.subject.AttendanceStats;

/**
 * Pure attendance maths, kept free of Spring and JPA so it can be unit tested exhaustively.
 *
 * <pre>
 * percentage = present / (present + absent) x 100       (cancelled classes are ignored)
 *
 * classes needed to reach target T (smallest x with (p + x) / (n + x) >= T/100):
 *     x = ceil( (T*n - 100*p) / (100 - T) )
 *
 * classes you can miss while staying at T (largest y with p / (n + y) >= T/100):
 *     y = floor( (100*p - T*n) / T )
 * </pre>
 *
 * All comparisons use exact BigDecimal arithmetic. The displayed percentage is rounded
 * to 2 decimals (HALF_UP), but "meets target" is decided on the unrounded value, so
 * 74.996% is correctly reported as below a 75% target even though it displays as 75.00.
 */
public final class AttendanceCalculator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private AttendanceCalculator() {
    }

    public static AttendanceStats calculate(int present, int absent, int cancelled, BigDecimal target) {
        if (present < 0 || absent < 0 || cancelled < 0) {
            throw new IllegalArgumentException("Attendance counts cannot be negative");
        }
        if (target == null || target.compareTo(BigDecimal.ONE) < 0 || target.compareTo(HUNDRED) > 0) {
            throw new IllegalArgumentException("Target must be between 1 and 100");
        }

        int total = present + absent;
        BigDecimal percentage = total == 0
                ? null
                : BigDecimal.valueOf(present).multiply(HUNDRED)
                        .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);

        BigDecimal presentTimesHundred = BigDecimal.valueOf(present).multiply(HUNDRED);
        BigDecimal targetTimesTotal = target.multiply(BigDecimal.valueOf(total));
        // With no classes yet there is nothing to fall short of.
        boolean meetsTarget = total == 0 || presentTimesHundred.compareTo(targetTimesTotal) >= 0;

        Integer classesNeeded;
        boolean reachable = true;
        if (meetsTarget) {
            classesNeeded = 0;
        } else if (target.compareTo(HUNDRED) == 0) {
            // After any absence, 100% can never be reached again.
            classesNeeded = null;
            reachable = false;
        } else {
            BigDecimal deficit = targetTimesTotal.subtract(presentTimesHundred);
            classesNeeded = deficit.divide(HUNDRED.subtract(target), 0, RoundingMode.CEILING).intValueExact();
        }

        int canMiss = 0;
        if (meetsTarget) {
            BigDecimal surplus = presentTimesHundred.subtract(targetTimesTotal);
            canMiss = surplus.divide(target, 0, RoundingMode.FLOOR).intValueExact();
        }

        return new AttendanceStats(present, absent, cancelled, total, percentage, target, meetsTarget,
                classesNeeded, reachable, canMiss);
    }
}
