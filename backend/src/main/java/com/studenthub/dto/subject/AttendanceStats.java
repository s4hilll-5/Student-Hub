package com.studenthub.dto.subject;

import java.math.BigDecimal;

/**
 * Calculated attendance for one subject (or overall).
 *
 * @param present                    classes attended (including prior classes)
 * @param absent                     classes missed (including prior classes)
 * @param cancelled                  cancelled classes, excluded from every percentage
 * @param totalCounted               present + absent
 * @param percentage                 present / totalCounted x 100, 2 decimals (HALF_UP); null when no classes yet
 * @param target                     target percentage
 * @param meetsTarget                exact comparison (not the rounded percentage) against the target
 * @param classesNeededToReachTarget consecutive classes to attend to reach the target; 0 if already met;
 *                                   null if unreachable (target of 100% after any absence)
 * @param targetReachable            false only when the target can never be reached again
 * @param classesYouCanMiss          further classes that can be missed while staying at or above target
 */
public record AttendanceStats(
        int present,
        int absent,
        int cancelled,
        int totalCounted,
        BigDecimal percentage,
        BigDecimal target,
        boolean meetsTarget,
        Integer classesNeededToReachTarget,
        boolean targetReachable,
        int classesYouCanMiss) {
}
