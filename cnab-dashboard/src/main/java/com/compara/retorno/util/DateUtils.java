package com.compara.retorno.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateUtils {

    public static final String DATE_PATTERN = "dd/MM/yyyy";
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);

    /**
     * Formats a LocalDate to the standard DD/MM/YYYY string.
     * Returns empty string if date is null.
     */
    public static String format(LocalDate date) {
        if (date == null) {
            return "";
        }
        return date.format(FORMATTER);
    }

    /**
     * Ensures start date is not after end date.
     * If start > end, they are swapped.
     * Handles nulls by defaulting to yesterday.
     * Returns an array where [0] is start and [1] is end.
     */
    public static LocalDate[] validateAndFixRange(LocalDate start, LocalDate end) {
        if (start == null) start = LocalDate.now().minusDays(1);
        if (end == null) end = LocalDate.now().minusDays(1);

        if (start.isAfter(end)) {
            return new LocalDate[]{end, start};
        }
        return new LocalDate[]{start, end};
    }
}
