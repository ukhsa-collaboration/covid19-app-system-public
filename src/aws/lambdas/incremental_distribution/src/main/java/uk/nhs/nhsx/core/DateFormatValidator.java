package uk.nhs.nhsx.core;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Optional;

public class DateFormatValidator {

    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    public static final DateTimeFormatter formatter = DateTimeFormatter
        .ofPattern(DATE_TIME_PATTERN)
        .withResolverStyle(ResolverStyle.STRICT);

    public static boolean isValid(String dateString) {
        try {
            formatter.parse(dateString);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    public static Optional<ZonedDateTime> toZonedDateTimeMaybe(String date) {
        if (!isValid(date)) {
            return Optional.empty();
        }
        return Optional.of(ZonedDateTime.parse(date));
    }
}
