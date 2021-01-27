package uk.nhs.nhsx.virology.lookup;

import com.fasterxml.jackson.annotation.JsonCreator;
import uk.nhs.nhsx.virology.Country;
import uk.nhs.nhsx.virology.TestResultPollingToken;

public class VirologyLookupRequestV2 {

    public final TestResultPollingToken testResultPollingToken;
    public final Country country;

    @JsonCreator
    public VirologyLookupRequestV2(TestResultPollingToken testResultPollingToken, Country country) {
        this.testResultPollingToken = testResultPollingToken;
        this.country = country;
    }
}
