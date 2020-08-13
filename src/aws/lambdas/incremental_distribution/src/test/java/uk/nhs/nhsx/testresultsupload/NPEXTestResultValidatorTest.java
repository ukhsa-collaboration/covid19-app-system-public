package uk.nhs.nhsx.testresultsupload;

import org.junit.Test;
import uk.nhs.nhsx.core.exceptions.ApiResponseException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class NPEXTestResultValidatorTest {

    @Test
    public void validTestResultPassesChecks(){
        NPEXTestResult testResult = new NPEXTestResult("405002323","2020-04-23T00:00:00Z","NEGATIVE");
        NPEXTestResultValidator.validateTestResult(testResult);
    }

    @Test
    public void testResultWithInvalidDateThrowsException(){
        NPEXTestResult testResult = new NPEXTestResult("405002323","2020-0F-23T09:11:32Z","NEGATIVE");
        assertThatThrownBy(() ->  NPEXTestResultValidator.validateTestResult(testResult))
            .isInstanceOf(ApiResponseException.class)
            .hasMessage("validation error: Invalid date format");
    }

    @Test
    public void testResultWithInvalidTokenThrowsException(){
        NPEXTestResult testResult = new NPEXTestResult("4050A23-3","2020-09-23T09:11:32Z","NEGATIVE");
        assertThatThrownBy(() ->  NPEXTestResultValidator.validateTestResult(testResult))
            .isInstanceOf(ApiResponseException.class)
            .hasMessage("validation error: Token Failed Validation");
    }

    @Test
    public void testResultWithInvalidTestResultThrowsException(){
        NPEXTestResult testResult = new NPEXTestResult("405023d","2020-0F-23T09:11:32Z","INCORRECT");
        assertThatThrownBy(() ->  NPEXTestResultValidator.validateTestResult(testResult))
            .isInstanceOf(ApiResponseException.class)
            .hasMessage("validation error: Invalid test result value");
    }

    @Test
    public void validDateAccepted(){
        NPEXTestResultValidator.validateDate("2020-04-23T00:00:00Z");
    }

    @Test
    public void dateWithInvalidTimeThrowsException(){
        assertThatThrownBy(() ->  NPEXTestResultValidator.validateDate("2020-04-23T11:09:53Z"))
            .isInstanceOf(ApiResponseException.class)
            .hasMessage("validation error: Invalid date, time must be set to 00:00:00");
    }

    @Test
    public void invalidDateThrowsException(){
        assertThatThrownBy(() ->  NPEXTestResultValidator.validateDate("2020-04-23T1F:09:53Z"))
            .isInstanceOf(ApiResponseException.class)
            .hasMessage("validation error: Invalid date format");
    }

    @Test
    public void validTokenPassesChecks(){
        NPEXTestResultValidator.validateToken("4r5fs00p329");
    }

    @Test
    public void tokenWithCapsThrowsException(){
        assertThatThrownBy(() ->   NPEXTestResultValidator.validateToken("4r5fA00p329"))
            .isInstanceOf(ApiResponseException.class)
            .hasMessage("validation error: Token Failed Validation");
    }

    @Test
    public void tokenWithSymbolThrowsException(){
        assertThatThrownBy(() ->   NPEXTestResultValidator.validateToken("4r5f-00p329"))
            .isInstanceOf(ApiResponseException.class)
            .hasMessage("validation error: Token Failed Validation");
    }
}
