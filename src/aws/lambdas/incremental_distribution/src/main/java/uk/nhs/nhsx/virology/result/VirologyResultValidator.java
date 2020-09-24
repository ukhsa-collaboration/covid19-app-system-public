package uk.nhs.nhsx.virology.result;

import uk.nhs.nhsx.core.exceptions.ApiResponseException;

import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import static uk.nhs.nhsx.core.exceptions.HttpStatusCode.UNPROCESSABLE_ENTITY_422;
import static uk.nhs.nhsx.virology.result.VirologyResultRequest.*;

public class VirologyResultValidator {

    public static void validateTestResult(String testResult, String testEndDate) {
        validateTestResult(testResult);
        validateDate(testEndDate);
    }

    private static void validateDate(String date) {
        try {
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(date, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            LocalTime time = zonedDateTime.toLocalTime();
            if (time.getHour() != 0 || time.getMinute() != 0 || time.getSecond() != 0) {
                throw new ApiResponseException(
                    UNPROCESSABLE_ENTITY_422, "validation error: Invalid date, time must be set to 00:00:00"
                );
            }
        } catch (DateTimeParseException e) {
            throw new ApiResponseException(UNPROCESSABLE_ENTITY_422, "validation error: Invalid date format");
        }
    }

    private static void validateTestResult(String testResult) {
        if (!NPEX_POSITIVE.contentEquals(testResult) &&
            !NPEX_NEGATIVE.contentEquals(testResult) &&
            !NPEX_VOID.contentEquals(testResult)) {
            throw new ApiResponseException(UNPROCESSABLE_ENTITY_422, "validation error: Invalid test result value");
        }
    }
}
