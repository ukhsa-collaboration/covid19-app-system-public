package uk.nhs.nhsx.testresultsupload;

import org.junit.Test;
import uk.nhs.nhsx.core.exceptions.ApiResponseException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class NpexTestResultValidatorTest {

    @Test
    public void validTestResultPassesChecks(){
        NpexTestResult testResult = new NpexTestResult("405002323","2020-04-23T00:00:00Z","NEGATIVE");
        NpexTestResultValidator.validateTestResult(testResult);
    }

    @Test
    public void testResultWithInvalidDateThrowsException(){
        NpexTestResult testResult = new NpexTestResult("405002323","2020-0F-23T09:11:32Z","NEGATIVE");
        assertThatThrownBy(() ->  NpexTestResultValidator.validateTestResult(testResult))
            .isInstanceOf(ApiResponseException.class)
            .hasMessage("validation error: Invalid date format");
    }

    @Test
    public void testResultWithInvalidTokenThrowsException(){
        NpexTestResult testResult = new NpexTestResult("4050A23-3","2020-09-23T09:11:32Z","NEGATIVE");
        assertThatThrownBy(() ->  NpexTestResultValidator.validateTestResult(testResult))
            .isInstanceOf(ApiResponseException.class)
            .hasMessage("validation error: Token Failed Validation");
    }

    @Test
    public void testResultWithInvalidTestResultThrowsException(){
        NpexTestResult testResult = new NpexTestResult("405023d","2020-0F-23T09:11:32Z","INCORRECT");
        assertThatThrownBy(() ->  NpexTestResultValidator.validateTestResult(testResult))
            .isInstanceOf(ApiResponseException.class)
            .hasMessage("validation error: Invalid test result value");
    }

    @Test
    public void validDateAccepted(){
        NpexTestResultValidator.validateDate("2020-04-23T00:00:00Z");
    }

    @Test
    public void dateWithInvalidTimeThrowsException(){
        assertThatThrownBy(() ->  NpexTestResultValidator.validateDate("2020-04-23T11:09:53Z"))
            .isInstanceOf(ApiResponseException.class)
            .hasMessage("validation error: Invalid date, time must be set to 00:00:00");
    }

    @Test
    public void invalidDateThrowsException(){
        assertThatThrownBy(() ->  NpexTestResultValidator.validateDate("2020-04-23T1F:09:53Z"))
            .isInstanceOf(ApiResponseException.class)
            .hasMessage("validation error: Invalid date format");
    }

    @Test
    public void validTokenPassesChecks(){
        NpexTestResultValidator.validateToken("4r5fs00p329");
    }

    @Test
    public void tokenWithCapsThrowsException(){
        assertThatThrownBy(() ->   NpexTestResultValidator.validateToken("4r5fA00p329"))
            .isInstanceOf(ApiResponseException.class)
            .hasMessage("validation error: Token Failed Validation");
    }

    @Test
    public void tokenWithSymbolThrowsException(){
        assertThatThrownBy(() ->   NpexTestResultValidator.validateToken("4r5f-00p329"))
            .isInstanceOf(ApiResponseException.class)
            .hasMessage("validation error: Token Failed Validation");
    }
}
