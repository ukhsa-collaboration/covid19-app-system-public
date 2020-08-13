package uk.nhs.nhsx.testkitorder;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TestOrderResponseFactory {

    public enum TestKitRequestType { ORDER, REGISTER }

    private final Map<TestKitRequestType, String> forwardingUrl;

    public TestOrderResponseFactory(String orderWebsite, String registerWebsite) {
        Map<TestKitRequestType, String> forwardingUrl = new HashMap<>();
        forwardingUrl.put(TestKitRequestType.ORDER, orderWebsite);
        forwardingUrl.put(TestKitRequestType.REGISTER, registerWebsite);
        this.forwardingUrl = Collections.unmodifiableMap(forwardingUrl);
    }

    public TestOrderResponse createTestOrderResponse(TokensGenerator.TestOrderTokens tokens,
                                                     TestKitRequestType testKitRequestType) {
        return new TestOrderResponse(
            forwardingUrl(tokens, testKitRequestType),
            tokens.ctaToken,
            tokens.testResultPollingToken,
            tokens.diagnosisKeySubmissionToken
        );
    }

    private String forwardingUrl(TokensGenerator.TestOrderTokens tokens,
                                 TestKitRequestType testKitRequestType) {
        return Optional.ofNullable(this.forwardingUrl.get(testKitRequestType))
                .map(url -> url + "?ctaToken=" + tokens.ctaToken)
                .orElseThrow(() -> new RuntimeException("Did not specify a test kit request type"));
    }


}
