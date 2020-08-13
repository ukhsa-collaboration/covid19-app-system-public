package uk.nhs.nhsx.testresultsupload;

import uk.nhs.nhsx.core.exceptions.ApiResponseException;

import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

import static uk.nhs.nhsx.core.exceptions.HttpStatusCode.UNPROCESSABLE_ENTITY_422;

public class NPEXTestResultValidator {


    public static void validateTestResult(NPEXTestResult testResult) {
        validateToken(testResult.ctaToken);
        validateTestResult(testResult.testResult);
        validateDate(testResult.testEndDate);
    }

    public static void validateDate(String date){
        try {
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(date, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            LocalTime time = zonedDateTime.toLocalTime();
            if( time.getHour() != 0 || time.getMinute() != 0 || time.getSecond() != 0){
                throw new ApiResponseException(UNPROCESSABLE_ENTITY_422, "validation error: Invalid date, time must be set to 00:00:00");
            }
        } catch (DateTimeParseException e) {
            throw new ApiResponseException(UNPROCESSABLE_ENTITY_422, "validation error: Invalid date format");
        }
    }

    public static void validateToken(String token){
        if (Pattern.compile("[^a-z0-9]").matcher(token).find()){
            throw new ApiResponseException(UNPROCESSABLE_ENTITY_422, "validation error: Token Failed Validation");
        }
    }

    private static void validateTestResult(String testResult){
        if (!"POSITIVE".contentEquals(testResult) && !"NEGATIVE".contentEquals(testResult) && !"VOID".contentEquals(testResult)){
            throw new ApiResponseException(UNPROCESSABLE_ENTITY_422, "validation error: Invalid test result value");
        }
    }
}
