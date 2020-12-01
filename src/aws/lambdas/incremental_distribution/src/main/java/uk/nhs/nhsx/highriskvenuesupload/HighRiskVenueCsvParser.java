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
    private static final String VENUE_CSV_ROW_MESSAGE_TYPE_FORMAT = "\\s*\"(?<venueId>[\\w\\-]+)\"\\s*,\\s*\"(?<startTime>[\\w.:\\-,/_]+)\"\\s*,\\s*\"(?<endTime>[\\w.:\\-,/_]+)\"\\s*,\\s*\"(?<messageType>[\\w.:\\-,/_]+)\"\\s*,\\s*\"(?<optionalParameter>[\\w ]*)\"\\s*";
    private static final String VENUE_CSV_HEADER_FORMAT = "\\s*#\\s*(?<venueIdColumnHeader>\\w+)\\s*,\\s*(?<startTimeColumnHeader>\\w+)\\s*,\\s*(?<endTimeColumnHeader>\\w+)\\s*";
    private static final String VENUE_CSV_HEADER_MESSAGE_TYPE_FORMAT = "\\s*#\\s*(?<venueIdColumnHeader>\\w+)\\s*,\\s*(?<startTimeColumnHeader>\\w+)\\s*,\\s*(?<endTimeColumnHeader>\\w+)\\s*,\\s*(?<messageTypeColumnHeader>\\w+)\\s*,\\s*(?<optionalParameterColumnHeader>\\w+)\\s*";
    private static final Pattern VENUE_CSV_ROW_PATTERN = Pattern.compile(VENUE_CSV_ROW_FORMAT);
    private static final Pattern VENUE_CSV_ROW_MESSAGE_TYPE_PATTERN = Pattern.compile(VENUE_CSV_ROW_MESSAGE_TYPE_FORMAT);
    private static final Pattern VENUE_CSV_HEADER_PATTERN = Pattern.compile(VENUE_CSV_HEADER_FORMAT);
    private static final Pattern VENUE_CSV_HEADER_MESSAGE_TYPE_PATTERN = Pattern.compile(VENUE_CSV_HEADER_MESSAGE_TYPE_FORMAT);
    private static final String VENUE_ID_FORMAT = "[CDEFHJKMPRTVWXY2345689]*";
    private static final Pattern VENUE_ID_PATTERN = Pattern.compile(VENUE_ID_FORMAT);
    private static final int VENUE_ID_MAX_LENGTH = 12;
    private static final String MESSAGE_TYPE_FORMAT = "M\\d";
    private static final Pattern MESSAGE_TYPE_PATTERN = Pattern.compile(MESSAGE_TYPE_FORMAT);
    private static final String CSV_HEADER_VENUE_ID = "venue_id";
    private static final String CSV_HEADER_START_TIME = "start_time";
    private static final String CSV_HEADER_END_TIME = "end_time";
    private static final String CSV_HEADER_MESSAGE_TYPE = "message_type";
    private static final String CSV_HEADER_OPTIONAL_PARAMETER = "optional_parameter";

    private static final String DEFAULT_MESSAGE_TYPE = "M1";
    private static final String DEFAULT_OPTIONAL_PARAMETER = "";
    private static final int CSV_CONTENT_MAX_SIZE = 1048576;
    private static boolean messageTypeFeatureFlag;
    private static final List<String> MESSAGE_TYPES_WITH_OPTIONAL_PARAMETER = List.of("M3");

    public HighRiskVenueCsvParser() {
        this(false);
    }
    public HighRiskVenueCsvParser(boolean messageTypeFeatureFlag) {
        HighRiskVenueCsvParser.messageTypeFeatureFlag = messageTypeFeatureFlag;
    }

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
        if (csv.getBytes().length > CSV_CONTENT_MAX_SIZE)
            throwParsingExceptionWith("Csv content is more than 1MB");

        String[] rows = csv.split("\\r?\\n");

        List<HighRiskVenue> venues = new ArrayList<>();

        if (messageTypeFeatureFlag) {
            validateHeaderRowWithMessageType(rows[0]);
            for (int i = 1; i < rows.length; i++) {
                VenueRisk venueIdRisk = parseRow(rows[i], i + 1, VENUE_CSV_ROW_MESSAGE_TYPE_PATTERN);
                RiskyWindow riskyWindow = new RiskyWindow(venueIdRisk.startTime, venueIdRisk.endTime);
                HighRiskVenue highRiskVenue = new HighRiskVenue(venueIdRisk.venueId, riskyWindow, venueIdRisk.messageType, venueIdRisk.optionalParameter);
                venues.add(highRiskVenue);
            }
        } else {
            validateHeaderRow(rows[0]);
            for (int i = 1; i < rows.length; i++) {
                VenueRisk venueIdRisk = parseRow(rows[i], i + 1, VENUE_CSV_ROW_PATTERN);
                RiskyWindow riskyWindow = new RiskyWindow(venueIdRisk.startTime, venueIdRisk.endTime);
                HighRiskVenue highRiskVenue = new HighRiskVenue(venueIdRisk.venueId, riskyWindow);
                venues.add(highRiskVenue);
            }
        }
        return new HighRiskVenues(venues);
    }

    private static class VenueRisk {

        final String venueId;
        final String startTime;
        final String endTime;
        final String messageType;
        final String optionalParameter;

        VenueRisk(String venueId, String startTime, String endTime) {
            this(venueId, startTime, endTime, DEFAULT_MESSAGE_TYPE, DEFAULT_OPTIONAL_PARAMETER);
        }

        VenueRisk(String venueId, String startTime, String endTime, String messageType, String optionalParameter) {
            checkRiskyWindow(startTime, endTime);
            checkVenueId(venueId);
            checkMessageType(messageType, optionalParameter);

            this.venueId = venueId;
            this.startTime = startTime;
            this.endTime = endTime;
            this.messageType = messageType;
            this.optionalParameter = optionalParameter;
        }

        private void checkMessageType(String messageType, String optionalParameter) {
            Matcher matcher = MESSAGE_TYPE_PATTERN.matcher(messageType);
            if (!matcher.matches()) {
                throwParsingExceptionWith("Invalid characters on the messageType: " + messageType);
            }
            if (!MESSAGE_TYPES_WITH_OPTIONAL_PARAMETER.contains(messageType) && !Strings.isNullOrEmpty(optionalParameter)) {
                throwParsingExceptionWith(String.format("Message type %s does not support optional parameter", messageType));
            }
            if (MESSAGE_TYPES_WITH_OPTIONAL_PARAMETER.contains(messageType) && Strings.isNullOrEmpty(optionalParameter)) {
                throwParsingExceptionWith(String.format("Message type %s must include an optional parameter", messageType));
            }
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

        private void checkVenueId(String venueId) {
            if (venueId.length() > VENUE_ID_MAX_LENGTH) {
                throwParsingExceptionWith("Length of VenueId is greater than " + VENUE_ID_MAX_LENGTH + " characters");
            }
            Matcher matcher = VENUE_ID_PATTERN.matcher(venueId);
            if (!matcher.matches()) {
                throwParsingExceptionWith("Invalid characters on the venueId: " + venueId);
            }
        }
    }

    private static VenueRisk parseRow(String dataRow, Integer row, Pattern pattern) {
        Matcher matcher = pattern.matcher(dataRow);
        if (!matcher.matches()) {
            throwParsingExceptionWith("Invalid data row on line number: " + row);
        }
        String venueId = matcher.group("venueId");
        String startTime = matcher.group("startTime");
        String endTime = matcher.group("endTime");

        if (!pattern.equals(VENUE_CSV_ROW_MESSAGE_TYPE_PATTERN)) {
            return new VenueRisk(venueId, startTime, endTime);
        }
        String messageType = matcher.group("messageType");
        String optionalParameter = matcher.group("optionalParameter");

        return new VenueRisk(venueId, startTime, endTime, messageType, optionalParameter);
    }

    private static void validateHeaderRow(String headerRow) {
        Matcher matcher = VENUE_CSV_HEADER_PATTERN.matcher(headerRow);

        if (!matcher.matches()) {
            throwParsingExceptionWith("Invalid header");
        }

        if (!CSV_HEADER_VENUE_ID.equals(matcher.group("venueIdColumnHeader")) ||
            !CSV_HEADER_START_TIME.equals(matcher.group("startTimeColumnHeader")) ||
            !CSV_HEADER_END_TIME.equals(matcher.group("endTimeColumnHeader"))) {
            throwParsingExceptionWith("Invalid header");
        }
    }

    private static void validateHeaderRowWithMessageType(String headerRow) {
        Matcher matcher = VENUE_CSV_HEADER_MESSAGE_TYPE_PATTERN.matcher(headerRow);

        if (!matcher.matches()) {
            throwParsingExceptionWith("Invalid header");
        }

        if (!CSV_HEADER_VENUE_ID.equals(matcher.group("venueIdColumnHeader")) ||
            !CSV_HEADER_START_TIME.equals(matcher.group("startTimeColumnHeader")) ||
            !CSV_HEADER_END_TIME.equals(matcher.group("endTimeColumnHeader")) ||
            !CSV_HEADER_MESSAGE_TYPE.equals(matcher.group("messageTypeColumnHeader")) ||
            !CSV_HEADER_OPTIONAL_PARAMETER.equals(matcher.group("optionalParameterColumnHeader"))) {
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
