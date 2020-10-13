package uk.nhs.nhsx.diagnosiskeyssubmission;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.natpryce.snodge.JsonMutator;
import org.junit.Test;
import uk.nhs.nhsx.ProxyRequestBuilder;
import uk.nhs.nhsx.TestData;
import uk.nhs.nhsx.core.Environment;
import uk.nhs.nhsx.analyticssubmission.FakeS3Storage;
import uk.nhs.nhsx.core.aws.dynamodb.AwsDynamoClient;
import uk.nhs.nhsx.core.aws.s3.ObjectKey;
import uk.nhs.nhsx.core.aws.s3.ObjectKeyNameProvider;
import uk.nhs.nhsx.core.exceptions.HttpStatusCode;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;
import static uk.nhs.nhsx.ContextBuilder.aContext;
import static uk.nhs.nhsx.matchers.ProxyResponseAssertions.*;

public class DiagnosisKeysSubmissionHandlerTest {

    private static final String SUBMISSION_DIAGNOSIS_KEYS_PATH = "/submission/diagnosis-keys";

    private final String uuid = "dd3aa1bf-4c91-43bb-afb6-12d0b5dcad43";

    private final String payloadJson = "{" +
        "  \"diagnosisKeySubmissionToken\": \"" + uuid + "\"," +
        "  \"temporaryExposureKeys\": [" +
        "    {" +
        "      \"key\": \"W2zb3BeMWt6Xr2u0ABG32Q==\"," +
        "      \"rollingStartNumber\": 12345," +
        "      \"rollingPeriod\": 144" +
        "    }," +
        "    {" +
        "      \"key\": \"kzQt9Lf3xjtAlMtm7jkSqw==\"," +
        "      \"rollingStartNumber\": 12499," +
        "      \"rollingPeriod\": 144" +
        "    }" +
        "  ]" +
        "}";

    private final String payloadJsonWithDaysSinceOnset = "{" +
        "  \"diagnosisKeySubmissionToken\": \"" + uuid + "\"," +
        "  \"temporaryExposureKeys\": [" +
        "    {" +
        "      \"key\": \"W2zb3BeMWt6Xr2u0ABG32Q==\"," +
        "      \"rollingStartNumber\": 12345," +
        "      \"rollingPeriod\": 144," +
        "      \"daysSinceOnsetOfSymptoms\": 1" +
        "    }," +
        "    {" +
        "      \"key\": \"kzQt9Lf3xjtAlMtm7jkSqw==\"," +
        "      \"rollingStartNumber\": 12499," +
        "      \"rollingPeriod\": 144," +
        "      \"daysSinceOnsetOfSymptoms\": 4" +
        "    }" +
        "  ]" +
        "}";

    private final String payloadJsonWithRiskLevel = "{" +
        "  \"diagnosisKeySubmissionToken\": \"" + uuid + "\"," +
        "  \"temporaryExposureKeys\": [" +
        "    {" +
        "        \"key\": \"W2zb3BeMWt6Xr2u0ABG32Q==\"," +
        "        \"rollingStartNumber\": 12345, " +
        "        \"rollingPeriod\": 144," +
        "        \"transmissionRiskLevel\": 5" +
        "    }," +
        "    {" +
        "        \"key\": \"kzQt9Lf3xjtAlMtm7jkSqw==\"," +
        "        \"rollingStartNumber\": 12499, " +
        "        \"rollingPeriod\": 144," +
        "        \"transmissionRiskLevel\": 4" +
        "    }" +
        "   ]" +
        "}";

    @SuppressWarnings("serial")
	private final Map<String, String> environmentSettings = new HashMap<>() {{
        put("submission_tokens_table", "stt");
        put("SUBMISSION_STORE", "store");
        put("MAINTENANCE_MODE", "FALSE");
    }};

    private final Environment environment = Environment.fromName("test", Environment.Access.TEST.apply(environmentSettings));

    private final FakeS3Storage s3Storage = new FakeS3Storage();
    private final AwsDynamoClient awsDynamoClient = mock(AwsDynamoClient.class);
    private final ObjectKeyNameProvider objectKeyNameProvider = mock(ObjectKeyNameProvider.class);
    private final Handler handler = new Handler(
        environment,
        e -> true,
        (req, resp) -> resp.getHeaders().put("signed", "yup"),
        s3Storage,
        awsDynamoClient,
        objectKeyNameProvider
    );

    @Test
    public void acceptsPayloadAndReturns200() throws Exception {
        ObjectKey objectKey = ObjectKey.of("some-object-key");

        String hashKey = "diagnosisKeySubmissionToken";
        when(objectKeyNameProvider.generateObjectKeyName()).thenReturn(objectKey);
        when(awsDynamoClient.getItem("stt", hashKey, uuid))
            .thenReturn(Item.fromJSON("{\"" + hashKey + "\": \"" + uuid + "\"}"));

        APIGatewayProxyResponseEvent responseEvent = responseFor(payloadJson);

        assertThat(responseEvent, hasStatus(HttpStatusCode.OK_200));
        assertThat(responseEvent, hasBody(equalTo(null)));
        assertThat(responseEvent, hasHeader("signed", equalTo("yup")));

        assertThat(s3Storage.count, equalTo(1));
        assertThat(s3Storage.name, equalTo(objectKey.append(".json")));
        assertThat(s3Storage.bucket.value, equalTo("store"));
        assertThat(new String(s3Storage.bytes.read(), StandardCharsets.UTF_8), equalTo(TestData.STORED_KEYS_PAYLOAD));
    }

    @Test
    public void acceptsPayloadWithDaysSinceOnset() throws Exception {
        ObjectKey objectKey = ObjectKey.of("some-object-key");

        String hashKey = "diagnosisKeySubmissionToken";
        when(objectKeyNameProvider.generateObjectKeyName()).thenReturn(objectKey);
        when(awsDynamoClient.getItem("stt", hashKey, uuid))
            .thenReturn(Item.fromJSON("{\"" + hashKey + "\": \"" + uuid + "\"}"));

        APIGatewayProxyResponseEvent responseEvent = responseFor(payloadJsonWithDaysSinceOnset);

        assertThat(responseEvent, hasStatus(HttpStatusCode.OK_200));
        assertThat(responseEvent, hasBody(equalTo(null)));
        assertThat(responseEvent, hasHeader("signed", equalTo("yup")));

        assertThat(s3Storage.count, equalTo(1));
        assertThat(s3Storage.name, equalTo(objectKey.append(".json")));
        assertThat(s3Storage.bucket.value, equalTo("store"));
        assertThat(new String(s3Storage.bytes.read(), StandardCharsets.UTF_8), equalTo(TestData.STORED_KEYS_PAYLOAD_DAYS_SINCE_ONSET));
    }

    @Test
    public void AcceptsPayloadWithRiskLevelAndReturns200() throws Exception {
        ObjectKey objectKey = ObjectKey.of("some-object-key");

        String hashKey = "diagnosisKeySubmissionToken";
        when(objectKeyNameProvider.generateObjectKeyName()).thenReturn(objectKey);
        when(awsDynamoClient.getItem("stt", hashKey, uuid))
            .thenReturn(Item.fromJSON("{\"" + hashKey + "\": \"" + uuid + "\"}"));

        APIGatewayProxyResponseEvent responseEvent = responseFor(payloadJsonWithRiskLevel);

        assertThat(responseEvent, hasStatus(HttpStatusCode.OK_200));
        assertThat(responseEvent, hasBody(equalTo(null)));
        assertThat(responseEvent, hasHeader("signed", equalTo("yup")));

        assertThat(s3Storage.count, equalTo(1));
        assertThat(s3Storage.name, equalTo(objectKey.append(".json")));
        assertThat(s3Storage.bucket.value, equalTo("store"));
        assertThat(new String(s3Storage.bytes.read(), StandardCharsets.UTF_8), equalTo(TestData.STORED_KEYS_PAYLOAD_WITH_RISK_LEVEL));
    }


    @Test
    public void notFoundWhenPathIsWrong() {
        APIGatewayProxyRequestEvent requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath("dodgy")
            .withBearerToken("anything")
            .withJson(payloadJson)
            .build();

        APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(requestEvent, aContext());

        assertThat(responseEvent, hasStatus(HttpStatusCode.NOT_FOUND_404));
        assertThat(responseEvent, hasBody(equalTo(null)));
        verifyNoMockInteractions();
    }

    @Test
    public void methodNotAllowedWhenMethodIsWrong() {
        APIGatewayProxyRequestEvent requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.GET)
            .withPath(SUBMISSION_DIAGNOSIS_KEYS_PATH)
            .withBearerToken("anything")
            .withJson(payloadJson)
            .build();

        APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(requestEvent, aContext());

        assertThat(responseEvent, hasStatus(HttpStatusCode.METHOD_NOT_ALLOWED_405));
        assertThat(responseEvent, hasBody(equalTo(null)));
        verifyNoMockInteractions();
    }

    @Test
    public void okWhenEmptyBodyPreventingAbuse() {
        APIGatewayProxyResponseEvent responseEvent = responseFor("");

        assertThat(responseEvent, hasStatus(HttpStatusCode.OK_200));
        assertThat(responseEvent, hasBody(equalTo(null)));
        verifyNoMockInteractions();
    }

    @Test
    public void okWhenInvalidJsonPreventingAbuse() {
        APIGatewayProxyResponseEvent responseEvent = responseFor("{");

        assertThat(responseEvent, hasStatus(HttpStatusCode.OK_200));
        assertThat(responseEvent, hasBody(equalTo(null)));
        verifyNoMockInteractions();
    }

    @Test
    public void okWhenEmptyJsonPreventingAbuse() {
        APIGatewayProxyResponseEvent responseEvent = responseFor("{}");

        assertThat(responseEvent, hasStatus(HttpStatusCode.OK_200));
        assertThat(responseEvent, hasBody(equalTo(null)));
        verifyNoMockInteractions();
    }

    @Test
    public void handlesRandomValues() {
        String originalJson = payloadJson;
        new JsonMutator()
            .forStrings()
            .mutate(originalJson, 100)
            .forEach(json -> {
                if (!json.equals(originalJson)) {
                    APIGatewayProxyResponseEvent response = responseFor(json);
                    assertThat(response, hasStatus(HttpStatusCode.OK_200));
                    assertThat(response, hasBody(equalTo(null)));
                }
            });
    }

    @Test
    public void handlesRandomValuesWithRiskLevel() {
        String originalJson = payloadJsonWithRiskLevel;
        new JsonMutator()
            .forStrings()
            .mutate(originalJson, 100)
            .forEach(json -> {
                if (!json.equals(originalJson)) {
                    APIGatewayProxyResponseEvent response = responseFor(json);
                    assertThat(response, hasStatus(HttpStatusCode.OK_200));
                    assertThat(response, hasBody(equalTo(null)));
                }
            });
    }

    private APIGatewayProxyResponseEvent responseFor(String requestPayload) {
        APIGatewayProxyRequestEvent requestEvent = ProxyRequestBuilder.request()
            .withMethod(HttpMethod.POST)
            .withPath(SUBMISSION_DIAGNOSIS_KEYS_PATH)
            .withBearerToken("anything")
            .withBody(requestPayload)
            .build();

        return handler.handleRequest(requestEvent, aContext());
    }

    private void verifyNoMockInteractions() {
        assertThat(s3Storage.count, equalTo(0));
        verifyNoInteractions(awsDynamoClient);
        verifyNoInteractions(objectKeyNameProvider);
    }
}