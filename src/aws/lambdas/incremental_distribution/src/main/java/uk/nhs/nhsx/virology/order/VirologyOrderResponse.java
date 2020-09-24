package uk.nhs.nhsx.virology.order;

import com.fasterxml.jackson.annotation.JsonCreator;
import uk.nhs.nhsx.virology.persistence.TestOrder;

public class VirologyOrderResponse {

    public final String websiteUrlWithQuery;
    public final String tokenParameterValue;
    public final String testResultPollingToken;
    public final String diagnosisKeySubmissionToken;

    @JsonCreator
    public VirologyOrderResponse(String websiteUrlWithQuery,
                                 String tokenParameterValue,
                                 String testResultPollingToken,
                                 String diagnosisKeySubmissionToken) {
        this.websiteUrlWithQuery = websiteUrlWithQuery;
        this.tokenParameterValue = tokenParameterValue;
        this.testResultPollingToken = testResultPollingToken;
        this.diagnosisKeySubmissionToken = diagnosisKeySubmissionToken;
    }

    public VirologyOrderResponse(String websiteUrlWithQuery, TestOrder testOrder) {
        this(websiteUrlWithQuery,
            testOrder.ctaToken.value,
            testOrder.testResultPollingToken.value,
            testOrder.diagnosisKeySubmissionToken.value
        );
    }
}
