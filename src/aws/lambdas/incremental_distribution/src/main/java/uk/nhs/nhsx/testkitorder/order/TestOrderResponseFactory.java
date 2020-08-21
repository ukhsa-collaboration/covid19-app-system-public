package uk.nhs.nhsx.testkitorder.order;

import java.util.Map;

public class TestOrderResponseFactory {

    public enum TestKitRequestType {ORDER, REGISTER}

    private final Map<TestKitRequestType, String> forwardingUrl;

    public TestOrderResponseFactory(String orderWebsite, String registerWebsite) {
        this.forwardingUrl = Map.of(
            TestKitRequestType.ORDER, orderWebsite,
            TestKitRequestType.REGISTER, registerWebsite
        );
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
        return this.forwardingUrl.get(testKitRequestType) + "?ctaToken=" + tokens.ctaToken;
    }
}
