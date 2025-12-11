package com.gogidix.platform.common.util;

import lombok.experimental.UtilityClass;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Utility class for date and time operations
 * Provides common date formatting and manipulation methods
 */
@UtilityClass
public class DateUtils {

    // Common date patterns
    public static final String ISO_DATE_TIME = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String ISO_DATE = "yyyy-MM-dd";
    public static final String ISO_TIME = "HH:mm:ss";
    public static final String SLASH_DATE = "MM/dd/yyyy";
    public static final String PRETTY_DATE_TIME = "MMMM dd, yyyy HH:mm a";

    // DateTimeFormatter instances (thread-safe)
    public static final DateTimeFormatter ISO_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(ISO_DATE_TIME);
    public static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ofPattern(ISO_DATE);
    public static final DateTimeFormatter ISO_TIME_FORMATTER = DateTimeFormatter.ofPattern(ISO_TIME);
    public static final DateTimeFormatter SLASH_DATE_FORMATTER = DateTimeFormatter.ofPattern(SLASH_DATE);
    public static final DateTimeFormatter PRETTY_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(PRETTY_DATE_TIME);

    /**
     * Convert Instant to LocalDateTime in system default timezone
     */
    public static LocalDateTime toLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    /**
     * Convert Date to LocalDateTime
     */
    public static LocalDateTime toLocalDateTime(Date date) {
        return toLocalDateTime(date.toInstant());
    }

    /**
     * Convert LocalDateTime to Instant
     */
    public static Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
    }

    /**
     * Convert LocalDateTime to Date
     */
    public static Date toDate(LocalDateTime localDateTime) {
        return Date.from(toInstant(localDateTime));
    }

    /**
     * Format LocalDateTime to ISO date time string
     */
    public static String toIsoDateTime(LocalDateTime dateTime) {
        return dateTime.format(ISO_DATE_TIME_FORMATTER);
    }

    /**
     * Format LocalDateTime to ISO date string
     */
    public static String toIsoDate(LocalDateTime dateTime) {
        return dateTime.format(ISO_DATE_FORMATTER);
    }

    /**
     * Parse ISO date time string to LocalDateTime
     */
    public static LocalDateTime fromIsoDateTime(String dateTimeString) {
        return LocalDateTime.parse(dateTimeString, ISO_DATE_TIME_FORMATTER);
    }

    /**
     * Parse ISO date string to LocalDateTime (start of day)
     */
    public static LocalDateTime fromIsoDate(String dateString) {
        return LocalDate.parse(dateString, ISO_DATE_FORMATTER).atStartOfDay();
    }

    /**
     * Get current timestamp in UTC
     */
    public static Instant nowUtc() {
        return Instant.now();
    }

    /**
     * Get current LocalDateTime in system default timezone
     */
    public static LocalDateTime now() {
        return LocalDateTime.now();
    }

    /**
     * Get start of day for given date
     */
    public static LocalDateTime startOfDay(LocalDateTime dateTime) {
        return dateTime.toLocalDate().atStartOfDay();
    }

    /**
     * Get end of day for given date
     */
    public static LocalDateTime endOfDay(LocalDateTime dateTime) {
        return dateTime.toLocalDate().atTime(23, 59, 59, 999999999);
    }

    /**
     * Calculate days between two dates
     */
    public static long daysBetween(LocalDateTime start, LocalDateTime end) {
        return ChronoUnit.DAYS.between(start, end);
    }

    /**
     * Calculate hours between two dates
     */
    public static long hoursBetween(LocalDateTime start, LocalDateTime end) {
        return ChronoUnit.HOURS.between(start, end);
    }

    /**
     * Check if a date is within the last N days
     */
    public static boolean isWithinLastDays(LocalDateTime date, int days) {
        return date.isAfter(LocalDateTime.now().minusDays(days));
    }

    /**
     * Check if a date is within the next N days
     */
    public static boolean isWithinNextDays(LocalDateTime date, int days) {
        return date.isBefore(LocalDateTime.now().plusDays(days));
    }

    /**
     * Add business days to a date (skips weekends)
     */
    public static LocalDateTime addBusinessDays(LocalDateTime date, int days) {
        LocalDateTime result = date;
        int addedDays = 0;

        while (addedDays < days) {
            result = result.plusDays(1);
            if (result.getDayOfWeek() != DayOfWeek.SATURDAY && result.getDayOfWeek() != DayOfWeek.SUNDAY) {
                addedDays++;
            }
        }

        return result;
    }

    /**
     * Get age from birth date
     */
    public static int getAge(LocalDate birthDate) {
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    /**
     * Check if date is weekend
     */
    public static boolean isWeekend(LocalDateTime date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    /**
     * Get first day of month
     */
    public static LocalDateTime firstDayOfMonth(LocalDateTime date) {
        return date.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
    }

    /**
     * Get last day of month
     */
    public static LocalDateTime lastDayOfMonth(LocalDateTime date) {
        return date.withDayOfMonth(date.toLocalDate().lengthOfMonth())
                   .withHour(23).withMinute(59).withSecond(59).withNano(999999999);
    }

    /**
     * Format duration in human readable format
     */
    public static String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
}