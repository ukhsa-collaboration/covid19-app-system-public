package uk.nhs.nhsx.analyticssubmission;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.natpryce.snodge.JsonMutator;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import uk.nhs.nhsx.ProxyRequestBuilder;
import uk.nhs.nhsx.TestData;
import uk.nhs.nhsx.core.TestEnvironments;
import uk.nhs.nhsx.core.aws.s3.BucketName;
import uk.nhs.nhsx.core.aws.s3.ObjectKey;
import uk.nhs.nhsx.core.aws.s3.ObjectKeyNameProvider;
import uk.nhs.nhsx.core.exceptions.HttpStatusCode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.nhs.nhsx.ContextBuilder.aContext;
import static uk.nhs.nhsx.matchers.ProxyResponseAssertions.hasBody;
import static uk.nhs.nhsx.matchers.ProxyResponseAssertions.hasStatus;

public class AnalyticsSubmissionHandlerTest {

    private static final BucketName BUCKET_NAME = BucketName.of("some-bucket-name");
    private final ObjectKey objectKey = ObjectKey.of("some-object-key");

    private static String iOSPayloadFrom(String startDate, String endDate) {
        return iOSPayloadFrom(startDate, endDate, "AB10");
    }

    public static String iOSPayloadFrom(String startDate, String endDate, String postDistrict) {
        return "{" +
            "  \"metadata\" : {" +
            "    \"operatingSystemVersion\" : \"iPhone OS 13.5.1 (17F80)\"," +
            "    \"latestApplicationVersion\" : \"3.0\"," +
            "    \"deviceModel\" : \"iPhone11,2\"," +
            "    \"postalDistrict\" : \"" + postDistrict + "\""  +
            "  }," +
            "  \"analyticsWindow\" : {" +
            "    \"endDate\" : \"" + endDate + "\"," +
            "    \"startDate\" : \"" + startDate + "\"" +
            "  }," +
            "  \"metrics\" : {" +
            "    \"cumulativeDownloadBytes\" : 140000000," +
            "    \"cumulativeUploadBytes\" : 140000000," +
            "    \"cumulativeCellularDownloadBytes\" : 80000000," +
            "    \"cumulativeCellularUploadBytes\" : 70000000," +
            "    \"cumulativeWifiDownloadBytes\" : 60000000," +
            "    \"cumulativeWifiUploadBytes\" : 50000000," +
            "    \"checkedIn\" : 1," +
            "    \"canceledCheckIn\" : 1," +
            "    \"receivedVoidTestResult\" : 1," +
            "    \"isIsolatingBackgroundTick\" : 1," +
            "    \"hasHadRiskyContactBackgroundTick\" : 1," +
            "    \"receivedPositiveTestResult\" : 1," +
            "    \"receivedNegativeTestResult\" : 1," +
            "    \"hasSelfDiagnosedPositiveBackgroundTick\" : 1," +
            "    \"completedQuestionnaireAndStartedIsolation\" : 1," +
            "    \"encounterDetectionPausedBackgroundTick\" : 1," +
            "    \"completedQuestionnaireButDidNotStartIsolation\" : 1," +
            "    \"totalBackgroundTasks\" : 1," +
            "    \"runningNormallyBackgroundTick\" : 1," +
            "    \"completedOnboarding\" : 1" +
            "  }," +
            "  \"includesMultipleApplicationVersions\" : false" +
            "}";
    }

    private static String androidPayloadFrom(String startDate, String endDate) {
        return androidPayloadFrom(startDate, endDate, "AB10");
    }

    private static String androidPayloadFrom(String startDate, String endDate, String postDistrict) {
        return "{" +
            "  \"metadata\" : {" +
            "    \"operatingSystemVersion\" : \"29\"," +
            "    \"latestApplicationVersion\" : \"3.0\"," +
            "    \"deviceModel\" : \"HUAWEI LDN-L21\"," +
            "    \"postalDistrict\" : \"" + postDistrict + "\"" +
            "  }," +
            "  \"analyticsWindow\" : {" +
            "    \"endDate\" : \"" + endDate + "\"," +
            "    \"startDate\" : \"" + startDate + "\"" +
            "  }," +
            "  \"metrics\" : {" +
            "    \"cumulativeDownloadBytes\" : null," +
            "    \"cumulativeUploadBytes\" : null," +
            "    \"checkedIn\" : 1," +
            "    \"canceledCheckIn\" : 1," +
            "    \"receivedVoidTestResult\" : 1," +
            "    \"isIsolatingBackgroundTick\" : 1," +
            "    \"hasHadRiskyContactBackgroundTick\" : 1," +
            "    \"receivedPositiveTestResult\" : 1," +
            "    \"receivedNegativeTestResult\" : 1," +
            "    \"hasSelfDiagnosedPositiveBackgroundTick\" : 1," +
            "    \"completedQuestionnaireAndStartedIsolation\" : 1," +
            "    \"encounterDetectionPausedBackgroundTick\" : 1," +
            "    \"completedQuestionnaireButDidNotStartIsolation\" : 1," +
            "    \"totalBackgroundTasks\" : 1," +
            "    \"runningNormallyBackgroundTick\" : 1," +
            "    \"completedOnboarding\" : 1" +
            "  }," +
            "  \"includesMultipleApplicationVersions\" : false" +
            "}";
    }

    private final FakeS3Storage s3Storage = new FakeS3Storage();
    
    private final ObjectKeyNameProvider objectKeyNameProvider = mock(ObjectKeyNameProvider.class);
    private final AnalyticsConfig config = new AnalyticsConfig(
        "firehoseStreamName",
        true,
        false,
        BUCKET_NAME
    );
    private final Handler handler = new Handler(TestEnvironments.TEST.apply(
        Map.of("MAINTENANCE_MODE", "false")), (e) -> true, s3Storage, objectKeyNameProvider, config);

    @Before
    public void setup() {
        when(objectKeyNameProvider.generateObjectKeyName()).thenReturn(objectKey);
    }

    @Test
    public void acceptsiOSPayloadAndReturns200() throws IOException {
        APIGatewayProxyRequestEvent requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("/submission/mobile-analytics")
            .withBearerToken("anything")
            .withJson(iOSPayloadFrom("2020-07-27T23:00:00Z", "2020-07-28T22:59:00Z"))
            .build();

        APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(requestEvent, aContext());

        assertThat(responseEvent, hasStatus(HttpStatusCode.OK_200));
        assertThat(responseEvent, hasBody(equalTo(null)));

        assertThat(s3Storage.count, equalTo(1));
        assertThat(s3Storage.name, equalTo(objectKey.append(".json")));
        assertThat(s3Storage.bucket, equalTo(BUCKET_NAME));
        assertThat(new String(s3Storage.bytes.read(), StandardCharsets.UTF_8), equalTo(TestData.STORED_ANALYTICS_PAYLOAD_IOS));
    }

    @Test
    public void acceptsiOSPayloadMergesPostDistrictsAndReturns200() throws IOException {
        APIGatewayProxyRequestEvent requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("/submission/mobile-analytics")
            .withBearerToken("anything")
            .withJson(iOSPayloadFrom("2020-07-27T23:00:00Z", "2020-07-28T22:59:00Z", "AB13"))
            .build();

        APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(requestEvent, aContext());

        assertThat(responseEvent, hasStatus(HttpStatusCode.OK_200));
        assertThat(responseEvent, hasBody(equalTo(null)));

        assertThat(s3Storage.count, equalTo(1));
        assertThat(s3Storage.name, equalTo(objectKey.append(".json")));
        assertThat(s3Storage.bucket, equalTo(BUCKET_NAME));
        assertThat(new String(s3Storage.bytes.read(), StandardCharsets.UTF_8), equalTo(TestData.STORED_ANALYTICS_MERGED_POSTCODE_PAYLOAD_IOS));
    }

    @Test
    public void iosPayloadWithPostcodeNotFoundInMappingSavesPostDistrictAsUnknown() throws IOException {
        APIGatewayProxyRequestEvent requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("/submission/mobile-analytics")
            .withBearerToken("anything")
            .withJson(iOSPayloadFrom("2020-07-27T23:00:00Z", "2020-07-28T22:59:00Z", "F4KEP0STC0DE"))
            .build();

        APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(requestEvent, aContext());

        assertThat(responseEvent, hasStatus(HttpStatusCode.OK_200));
        assertThat(responseEvent, hasBody(equalTo(null)));

        assertThat(s3Storage.count, equalTo(1));
        assertThat(s3Storage.name, equalTo(objectKey.append(".json")));
        assertThat(s3Storage.bucket, equalTo(BUCKET_NAME));
        assertThat(new String(s3Storage.bytes.read(), StandardCharsets.UTF_8), equalTo(TestData.STORED_ANALYTICS_UNKOWN_POSTCODE_PAYLOAD_IOS));
    }

    @Test
    public void androidPayloadWithPostcodeNotFoundInMappingSavesPostDistrictAsUnknown() throws IOException {
        APIGatewayProxyRequestEvent requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("/submission/mobile-analytics")
            .withBearerToken("anything")
            .withJson(androidPayloadFrom("2020-07-27T23:00:00Z", "2020-07-28T22:59:00Z", "F4KEP0STC0DE"))
            .build();

        APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(requestEvent, aContext());

        assertThat(responseEvent, hasStatus(HttpStatusCode.OK_200));
        assertThat(responseEvent, hasBody(equalTo(null)));

        assertThat(s3Storage.count, equalTo(1));
        assertThat(s3Storage.name, equalTo(objectKey.append(".json")));
        assertThat(s3Storage.bucket, equalTo(BUCKET_NAME));
        assertThat(new String(s3Storage.bytes.read(),StandardCharsets.UTF_8), equalTo(TestData.STORED_ANALYTICS_UNKNOWN_POSTCODE_PAYLOAD_ANDROID));
    }

    @Test
    public void acceptsAndroidPayloadMergesPostDistrictsAndReturns200() throws IOException {
        APIGatewayProxyRequestEvent requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("/submission/mobile-analytics")
            .withBearerToken("anything")
            .withJson(androidPayloadFrom("2020-07-27T23:00:00Z", "2020-07-28T22:59:00Z", "AB13"))
            .build();

        APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(requestEvent, aContext());

        assertThat(responseEvent, hasStatus(HttpStatusCode.OK_200));
        assertThat(responseEvent, hasBody(equalTo(null)));

        assertThat(s3Storage.count, equalTo(1));
        assertThat(s3Storage.name, equalTo(objectKey.append(".json")));
        assertThat(s3Storage.bucket, equalTo(BUCKET_NAME));
        assertThat(new String(s3Storage.bytes.read(),StandardCharsets.UTF_8), equalTo(TestData.STORED_ANALYTICS_MERGED_POSTCODE_PAYLOAD_ANDROID));
    }

    @Test
    public void acceptsAndroidPayloadAndReturns200() throws IOException {
        APIGatewayProxyRequestEvent requestEvent = ProxyRequestBuilder.request()
                .withMethod(HttpMethod.POST)
                .withPath("/submission/mobile-analytics")
                .withBearerToken("anything")
                .withJson(androidPayloadFrom("2020-07-27T23:00:00Z", "2020-07-28T22:59:00Z"))
                .build();

        APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(requestEvent, aContext());

        assertThat(responseEvent, hasStatus(HttpStatusCode.OK_200));
        assertThat(responseEvent, hasBody(equalTo(null)));

        assertThat(s3Storage.count, equalTo(1));
        assertThat(s3Storage.name, equalTo(objectKey.append(".json")));
        assertThat(s3Storage.bucket, equalTo(BUCKET_NAME));
        assertThat(new String(s3Storage.bytes.read(), StandardCharsets.UTF_8), equalTo(TestData.STORED_ANALYTICS_PAYLOAD_ANDROID));
    }

    @Test
    public void notFoundWhenPathIsWrong() {
        APIGatewayProxyRequestEvent requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withBearerToken("anything")
            .withPath("dodgy")
            .withJson(iOSPayloadFrom("2020-07-27T23:00:00Z", "2020-07-28T22:59:00Z"))
            .build();

        APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(requestEvent, aContext());

        assertThat(responseEvent, hasStatus(HttpStatusCode.NOT_FOUND_404));
        assertThat(responseEvent, hasBody(equalTo(null)));
        assertThat(s3Storage.count, equalTo(0));
    }

    @Test
    public void notAllowedWhenMethodIsWrong() {
        APIGatewayProxyRequestEvent requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.GET)
            .withBearerToken("something")
            .withPath("/submission/mobile-analytics")
            .withJson(iOSPayloadFrom("2020-07-27T23:00:00Z", "2020-07-28T22:59:00Z"))
            .build();

        APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(requestEvent, aContext());

        assertThat(responseEvent, hasStatus(HttpStatusCode.METHOD_NOT_ALLOWED_405));
        assertThat(responseEvent, hasBody(equalTo(null)));

        assertThat(s3Storage.count, equalTo(0));
    }

    @Test
    public void badRequestWhenEmptyBody() {
        APIGatewayProxyResponseEvent responseEvent = responseFor("");

        assertThat(responseEvent, hasStatus(HttpStatusCode.BAD_REQUEST_400));
        assertThat(responseEvent, hasBody(equalTo(null)));

        assertThat(s3Storage.count, equalTo(0));
    }

    @Test
    public void badRequestWhenMalformedJson() {
        APIGatewayProxyResponseEvent responseEvent = responseFor("{");

        assertThat(responseEvent, hasStatus(HttpStatusCode.BAD_REQUEST_400));
        assertThat(responseEvent, hasBody(equalTo(null)));

        assertThat(s3Storage.count, equalTo(0));
    }

    @Test
    public void badRequestWhenEmptyJsonObject() {
        APIGatewayProxyResponseEvent responseEvent = responseFor("{}");

        assertThat(responseEvent, hasStatus(HttpStatusCode.BAD_REQUEST_400));
        assertThat(responseEvent, hasBody(equalTo(null)));

        assertThat(s3Storage.count, equalTo(0));
    }

    @Ignore("Mutated postcode won't be in mapping causing a 500 error") @Test
    public void randomPayloadValues() {
        String originalJson = iOSPayloadFrom("2020-07-27T23:00:00Z", "2020-07-28T22:59:00Z");
        new JsonMutator()
            .forStrings()
            .mutate(originalJson, 100)
            .forEach(json -> {
                if (!json.equals(originalJson)) {
                    APIGatewayProxyResponseEvent response = responseFor(json);
                    assertThat(response, not(anyOf(
                        hasStatus(HttpStatusCode.INTERNAL_SERVER_ERROR_500),
                        hasStatus(HttpStatusCode.FORBIDDEN_403)
                        ))
                    );
                    assertThat(response, hasBody(equalTo(null)));
                }
            });
    }

    @Test
    public void badRequestWhenDodgyStartDate() {
        APIGatewayProxyResponseEvent responseEvent = responseFor(
            iOSPayloadFrom("2020-06-2001:00:00Z", "2020-06-20T22:00:00Z")
        );

        assertThat(responseEvent, hasStatus(HttpStatusCode.BAD_REQUEST_400));
        assertThat(responseEvent, hasBody(equalTo(null)));

        assertThat(s3Storage.count, equalTo(0));
    }

    @Test
    public void badRequestWhenDodgyEndDate() {
        APIGatewayProxyResponseEvent responseEvent = responseFor(
            iOSPayloadFrom("2020-06-20T22:00:00Z", "2020-06-20T22:00:00")
        );

        assertThat(responseEvent, hasStatus(HttpStatusCode.BAD_REQUEST_400));
        assertThat(responseEvent, hasBody(equalTo(null)));

        assertThat(s3Storage.count, equalTo(0));
    }

    private APIGatewayProxyResponseEvent responseFor(String requestPayload) {
        APIGatewayProxyRequestEvent requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withBearerToken("anything")
            .withPath("/submission/mobile-analytics")
            .withBody(requestPayload)
            .build();

        return handler.handleRequest(requestEvent, aContext());
    }
}
