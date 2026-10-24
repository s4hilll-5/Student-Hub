package com.studenthub.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.studenthub.dto.subject.AttendanceStats;

class AttendanceCalculatorTest {

    private static AttendanceStats calc(int present, int absent, int cancelled, String target) {
        return AttendanceCalculator.calculate(present, absent, cancelled, new BigDecimal(target));
    }

    @Test
    @DisplayName("no classes yet: no percentage, nothing needed, nothing to miss")
    void zeroClasses() {
        AttendanceStats stats = calc(0, 0, 0, "75.00");
        assertThat(stats.percentage()).isNull();
        assertThat(stats.totalCounted()).isZero();
        assertThat(stats.meetsTarget()).isTrue();
        assertThat(stats.classesNeededToReachTarget()).isZero();
        assertThat(stats.classesYouCanMiss()).isZero();
    }

    @Test
    @DisplayName("cancelled classes are excluded from the percentage")
    void cancelledExcluded() {
        AttendanceStats stats = calc(3, 1, 4, "75.00");
        assertThat(stats.totalCounted()).isEqualTo(4);
        assertThat(stats.cancelled()).isEqualTo(4);
        assertThat(stats.percentage()).isEqualByComparingTo("75.00");
    }

    @Test
    @DisplayName("only cancelled classes still means no percentage (no division by zero)")
    void onlyCancelled() {
        AttendanceStats stats = calc(0, 0, 6, "75.00");
        assertThat(stats.percentage()).isNull();
        assertThat(stats.meetsTarget()).isTrue();
    }

    @Test
    @DisplayName("percentage is rounded HALF_UP to two decimals")
    void rounding() {
        assertThat(calc(2, 1, 0, "75").percentage()).isEqualByComparingTo("66.67");
        assertThat(calc(1, 2, 0, "75").percentage()).isEqualByComparingTo("33.33");
        assertThat(calc(1, 7, 0, "75").percentage()).isEqualByComparingTo("12.50");
    }

    @Test
    @DisplayName("target is compared on the exact value, not the rounded display value")
    void exactComparison() {
        // 18749 / 25000 = 74.996% -> displays 75.00 but is below a 75% target
        AttendanceStats stats = calc(18749, 6251, 0, "75.00");
        assertThat(stats.percentage()).isEqualByComparingTo("75.00");
        assertThat(stats.meetsTarget()).isFalse();
        assertThat(stats.classesNeededToReachTarget()).isEqualTo(4);
    }

    @ParameterizedTest(name = "{0} present, {1} absent, target {2}% -> attend {3} in a row")
    @CsvSource({
            "2, 1, 75, 1",     // 3/4 = 75%
            "20, 20, 75, 40",  // 60/80 = 75%
            "6, 4, 75, 6",     // 12/16 = 75%
            "3, 1, 75.5, 1",   // 4/5 = 80% >= 75.5%
            "0, 1, 50, 1",     // 1/2 = 50%
            "30, 10, 75, 0"    // already exactly at target
    })
    void classesNeeded(int present, int absent, String target, int expected) {
        assertThat(calc(present, absent, 0, target).classesNeededToReachTarget()).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0} present, {1} absent, target {2}% -> can miss {3}")
    @CsvSource({
            "33, 7, 75, 4",   // 33/44 = 75%
            "30, 10, 75, 0",  // exactly at target: no room
            "10, 0, 75, 3",   // 10/13 = 76.9%, 10/14 = 71.4%
            "2, 1, 75, 0"     // already below target
    })
    void classesYouCanMiss(int present, int absent, String target, int expected) {
        assertThat(calc(present, absent, 0, target).classesYouCanMiss()).isEqualTo(expected);
    }

    @Test
    @DisplayName("a 100% target becomes unreachable after one absence")
    void hundredPercentUnreachable() {
        AttendanceStats stats = calc(9, 1, 0, "100");
        assertThat(stats.targetReachable()).isFalse();
        assertThat(stats.classesNeededToReachTarget()).isNull();
        assertThat(calc(9, 0, 0, "100").meetsTarget()).isTrue();
    }

    @Test
    @DisplayName("formulas agree with brute force for every small combination")
    void bruteForce() {
        String[] targets = {"50", "60", "75", "80", "85.5", "99"};
        for (int p = 0; p <= 30; p++) {
            for (int a = 0; a <= 30; a++) {
                for (String t : targets) {
                    BigDecimal target = new BigDecimal(t);
                    AttendanceStats stats = AttendanceCalculator.calculate(p, a, 0, target);
                    assertThat(stats.classesNeededToReachTarget()).as("needed p=%d a=%d t=%s", p, a, t)
                            .isEqualTo(bruteNeeded(p, p + a, target));
                    assertThat(stats.classesYouCanMiss()).as("miss p=%d a=%d t=%s", p, a, t)
                            .isEqualTo(bruteMiss(p, p + a, target));
                }
            }
        }
    }

    @Test
    @DisplayName("invalid attendance counts and targets are rejected")
    void invalidInput() {
        assertThatThrownBy(() -> calc(-1, 0, 0, "75")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calc(0, -2, 0, "75")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calc(0, 0, -3, "75")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calc(1, 0, 0, "0.5")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calc(1, 0, 0, "100.01")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AttendanceCalculator.calculate(1, 0, 0, null)).isInstanceOf(IllegalArgumentException.class);
    }

    private static int bruteNeeded(int present, int total, BigDecimal target) {
        int x = 0;
        while (BigDecimal.valueOf((present + x) * 100L).compareTo(target.multiply(BigDecimal.valueOf(total + x))) < 0) {
            x++;
        }
        return x;
    }

    private static int bruteMiss(int present, int total, BigDecimal target) {
        BigDecimal p100 = BigDecimal.valueOf(present * 100L);
        if (total > 0 && p100.compareTo(target.multiply(BigDecimal.valueOf(total))) < 0) {
            return 0;
        }
        int y = 0;
        while (p100.compareTo(target.multiply(BigDecimal.valueOf(total + y + 1))) >= 0) {
            y++;
        }
        return y;
    }
}
