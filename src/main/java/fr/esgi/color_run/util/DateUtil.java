package fr.esgi.color_run.util;

import java.text.DateFormatSymbols;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

public class DateUtil {
    public static String formatDateFr(Date date) {
        if (date == null) return "";

        // Convertir Date → LocalDateTime
        LocalDateTime localDateTime = Instant.ofEpochMilli(date.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        // Formatter personnalisé en français
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy HH'h'mm", Locale.FRENCH);

        return localDateTime.format(formatter);
    }
}
