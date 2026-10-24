package com.studenthub.service;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;

import com.studenthub.entity.User;

/** Calendar helpers that respect each student's own timezone. */
public final class TimeUtils {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private TimeUtils() {
    }

    public static ZoneId zoneOf(User user) {
        try {
            return ZoneId.of(user.getTimezone());
        } catch (DateTimeException | NullPointerException e) {
            return ZoneOffset.UTC;
        }
    }

    public static LocalDate today(Clock clock, ZoneId zone) {
        return LocalDate.ofInstant(clock.instant(), zone);
    }

    /** Monday of the week containing the date. */
    public static LocalDate weekStart(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    public static String formatRange(LocalTime start, LocalTime end) {
        return HH_MM.format(start) + "–" + HH_MM.format(end);
    }

    public static String dayName(DayOfWeek day) {
        return day.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }
}
