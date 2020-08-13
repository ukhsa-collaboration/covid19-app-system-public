package uk.nhs.nhsx.highriskvenuesupload;

import com.google.common.base.Strings;
import uk.nhs.nhsx.core.DateFormatValidator;
import uk.nhs.nhsx.core.Jackson;
import uk.nhs.nhsx.highriskvenuesupload.model.HighRiskVenue;
import uk.nhs.nhsx.highriskvenuesupload.model.HighRiskVenues;
import uk.nhs.nhsx.highriskvenuesupload.model.RiskyWindow;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static uk.nhs.nhsx.core.DateFormatValidator.DATE_TIME_PATTERN;

public class HighRiskVenueCsvParser {

    private static final String VENUE_CSV_ROW_FORMAT = "\\s*\"(?<venueId>[\\w\\-]+)\"\\s*,\\s*\"(?<startTime>[\\w.:\\-,/_]+)\"\\s*,\\s*\"(?<endTime>[\\w.:\\-,/_]+)\"\\s*";
    private static final String VENUE_CSV_HEADER_FORMAT = "\\s*#\\s*(?<venueIdColumnHeader>\\w+)\\s*,\\s*(?<startTimeColumnHeader>\\w+)\\s*,\\s*(?<endTimeColumnHeader>\\w+)\\s*";
    private static final Pattern VENUE_CSV_ROW_PATTERN = Pattern.compile(VENUE_CSV_ROW_FORMAT);
    private static final Pattern VENUE_CSV_HEADER_PATTERN = Pattern.compile(VENUE_CSV_HEADER_FORMAT);
    private static final String CSV_HEADER_VENUE_ID = "venue_id";
    private static final String CSV_HEADER_START_TIME = "start_time";
    private static final String CSV_HEADER_END_TIME = "end_time";

    public VenuesParsingResult toJson(String csv) {
        try {
            HighRiskVenues riskyVenues = HighRiskVenueCsvParser.parse(csv);
            return VenuesParsingResult.ok(Jackson.toJson(riskyVenues));
        } catch (VenuesParsingException e) {
            return VenuesParsingResult.failure(e.getMessage());
        }
    }
    
    private static HighRiskVenues parse(String csv) {
        if (Strings.isNullOrEmpty(csv) || csv.trim().isEmpty())
            throwParsingExceptionWith("No payload");

        String[] rows = csv.split("\\r?\\n");

        validateHeaderRow(rows[0]);

        List<HighRiskVenue> venues = new ArrayList<>();
        for (int i = 1; i < rows.length; i++) {
            VenueRisk venueIdRisk = parseRow(rows[i], i + 1);
            RiskyWindow riskyWindow = new RiskyWindow(venueIdRisk.startTime, venueIdRisk.endTime);
            HighRiskVenue highRiskVenue = new HighRiskVenue(venueIdRisk.venueId, riskyWindow);
            venues.add(highRiskVenue);
        }
        // TODO: check if date ranges overlap for the same venue id
        return new HighRiskVenues(venues);
    }

    private static class VenueRisk {

        final String venueId;
        final String startTime;
        final String endTime;

        VenueRisk(String venueId, String startTime, String endTime) {
            checkRiskyWindow(startTime, endTime);
            this.venueId = venueId;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        private void checkRiskyWindow(String startTime, String endTime) {
            ZonedDateTime startDateTime = DateFormatValidator
                .toZonedDateTimeMaybe(startTime)
                .orElseThrow(() ->
                    parsingExceptionWith(
                        "Date " + startTime + " did not conform to expected format " + DATE_TIME_PATTERN
                    )
                );

            ZonedDateTime endDateTime = DateFormatValidator
                .toZonedDateTimeMaybe(endTime)
                .orElseThrow(() ->
                    parsingExceptionWith(
                        "Date " + endTime + " did not conform to expected format " + DATE_TIME_PATTERN
                    )
                );

            if (endDateTime.isBefore(startDateTime)) {
                throwParsingExceptionWith("Start date must be <= end date");
            }
        }
    }

    private static VenueRisk parseRow(String dataRow, Integer row) {
        Matcher matcher = VENUE_CSV_ROW_PATTERN.matcher(dataRow);
        if (!matcher.matches()) {
            throwParsingExceptionWith("Invalid data row on line number: " + row);
        }

        String venueId = matcher.group("venueId");
        String startTime = matcher.group("startTime");
        String endTime = matcher.group("endTime");

        return new VenueRisk(venueId, startTime, endTime);
    }

    private static void validateHeaderRow(String headerRow) {
        Matcher matcher = VENUE_CSV_HEADER_PATTERN.matcher(headerRow);

        if (!matcher.matches()) {
            throwParsingExceptionWith("Invalid header");
        }

        String venueIdColumnHeader = matcher.group("venueIdColumnHeader");
        String startTimeColumnHeader = matcher.group("startTimeColumnHeader");
        String endTimeColumnHeader = matcher.group("endTimeColumnHeader");

        if (!CSV_HEADER_VENUE_ID.equals(venueIdColumnHeader) ||
            !CSV_HEADER_START_TIME.equals(startTimeColumnHeader) ||
            !CSV_HEADER_END_TIME.equals(endTimeColumnHeader)) {
            throwParsingExceptionWith("Invalid header");
        }
    }

    private static void throwParsingExceptionWith(String reason) {
        throw parsingExceptionWith(reason);
    }

    private static VenuesParsingException parsingExceptionWith(String reason) {
        return new VenuesParsingException("validation error: " + reason);
    }
}
