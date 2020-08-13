package uk.nhs.nhsx.testresultsupload;

import org.junit.Test;
import uk.nhs.nhsx.core.exceptions.ApiResponseException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

public class NPEXTestResultUploadServiceTest {
    private final NPEXTestResultPersistenceService persistenceService = mock(NPEXTestResultPersistenceService.class);

    @Test
    public void persistsResultCorrectlyForPositiveResult(){
        NPEXTestResultUploadService uploadService = new NPEXTestResultUploadService(persistenceService);
        NPEXTestResult testResult = new NPEXTestResult("405002323","2020-04-23T00:00:00Z","POSITIVE");
        uploadService.accept(testResult);
        verify(persistenceService, times(1)).persistPositiveTestResult(testResult);

        verifyNoMoreInteractions(persistenceService);
    }

    @Test
    public void persistsResultCorrectlyForNegativeResult(){
        NPEXTestResultUploadService uploadService = new NPEXTestResultUploadService(persistenceService);
        NPEXTestResult testResult = new NPEXTestResult("405002323","2020-04-23T00:00:00Z","NEGATIVE");
        uploadService.accept(testResult);
        verify(persistenceService, times(1)).persistNegativeOrVoidTestResult(testResult);
        verifyNoMoreInteractions(persistenceService);
    }

    @Test
    public void persistsResultCorrectlyForVoidResult(){
        NPEXTestResultUploadService uploadService = new NPEXTestResultUploadService(persistenceService);
        NPEXTestResult testResult = new NPEXTestResult("405002323","2020-04-23T00:00:00Z","VOID");
        uploadService.accept(testResult);
        verify(persistenceService, times(1)).persistNegativeOrVoidTestResult(testResult);
        verifyNoMoreInteractions(persistenceService);
    }

    //Throws Exception with invalid test result value
    @Test
    public void throwsExceptionWithInvalidTestResult(){
        NPEXTestResultUploadService uploadService = new NPEXTestResultUploadService(persistenceService);
        NPEXTestResult testResult = new NPEXTestResult("405002323","2020-04-23T00:00:00Z","ORANGE");
        assertThatThrownBy(() -> uploadService.accept(testResult))
            .isInstanceOf(ApiResponseException.class)
            .hasMessage("validation error: Invalid test result value");
        verifyNoInteractions(persistenceService);
    }


}
