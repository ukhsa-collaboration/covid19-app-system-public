package uk.nhs.nhsx;

import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey;
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKeyPayload;
import uk.nhs.nhsx.isolationpayment.model.IsolationResponse;
import uk.nhs.nhsx.isolationpayment.model.IsolationToken;
import uk.nhs.nhsx.isolationpayment.model.TokenStatus;
import uk.nhs.nhsx.virology.TestResultPollingToken;
import uk.nhs.nhsx.virology.persistence.TestResult;

import static java.util.Arrays.asList;

public class TestData {

    public static final String STORED_KEYS_PAYLOAD =
        "{\"temporaryExposureKeys\":[" +
            "{\"key\":\"W2zb3BeMWt6Xr2u0ABG32Q==\",\"rollingStartNumber\":12345,\"rollingPeriod\":144,\"transmissionRisk\":7}," +
            "{\"key\":\"kzQt9Lf3xjtAlMtm7jkSqw==\",\"rollingStartNumber\":12499,\"rollingPeriod\":144,\"transmissionRisk\":7}" +
            "]}";

    public static final String STORED_KEYS_PAYLOAD_SUBMISSION =
        "{\"temporaryExposureKeys\":[" +
            "{\"key\":\"W2zb3BeMWt6Xr2u0ABG32Q==\",\"rollingStartNumber\":2666736,\"rollingPeriod\":144,\"transmissionRisk\":7}," +
            "{\"key\":\"kzQt9Lf3xjtAlMtm7jkSqw==\",\"rollingStartNumber\":2664864,\"rollingPeriod\":144,\"transmissionRisk\":7}" +
            "]}";

    public static final String STORED_KEYS_PAYLOAD_DAYS_SINCE_ONSET =
        "{\"temporaryExposureKeys\":[" +
            "{\"key\":\"W2zb3BeMWt6Xr2u0ABG32Q==\",\"rollingStartNumber\":12345,\"rollingPeriod\":144,\"transmissionRisk\":7,\"daysSinceOnsetOfSymptoms\":1}," +
            "{\"key\":\"kzQt9Lf3xjtAlMtm7jkSqw==\",\"rollingStartNumber\":12499,\"rollingPeriod\":144,\"transmissionRisk\":7,\"daysSinceOnsetOfSymptoms\":4}" +
            "]}";

    public static final String STORED_KEYS_PAYLOAD_DAYS_SINCE_ONSET_SUBMISSION =
        "{\"temporaryExposureKeys\":[" +
            "{\"key\":\"W2zb3BeMWt6Xr2u0ABG32Q==\",\"rollingStartNumber\":2666736,\"rollingPeriod\":144,\"transmissionRisk\":7,\"daysSinceOnsetOfSymptoms\":1}," +
            "{\"key\":\"kzQt9Lf3xjtAlMtm7jkSqw==\",\"rollingStartNumber\":2664864,\"rollingPeriod\":144,\"transmissionRisk\":7,\"daysSinceOnsetOfSymptoms\":4}" +
            "]}";

    public static final String STORED_KEYS_PAYLOAD_WITH_RISK_LEVEL =
        "{\"temporaryExposureKeys\":[" +
        "{\"key\":\"W2zb3BeMWt6Xr2u0ABG32Q==\",\"rollingStartNumber\":2666736,\"rollingPeriod\":144,\"transmissionRisk\":5}," +
        "{\"key\":\"kzQt9Lf3xjtAlMtm7jkSqw==\",\"rollingStartNumber\":2664864,\"rollingPeriod\":144,\"transmissionRisk\":4}" +
        "]}";
    public static final String STORED_KEYS_PAYLOAD_ONE_KEY =
        "{\"temporaryExposureKeys\":[" +
            "{\"key\":\"W2zb3BeMWt6Xr2u0ABG32Q==\",\"rollingStartNumber\":2666736,\"rollingPeriod\":144,\"transmissionRisk\":7}" +
            "]}";

    public final static String STORED_FEDERATED_KEYS_PAYLOAD_NI = "{\"temporaryExposureKeys\":[{\"key\":\"W2zb3BeMWt6Xr2u0ABG32Q==\",\"rollingStartNumber\":2666736,\"rollingPeriod\":144,\"transmissionRisk\":3},{\"key\":\"B3xb3BeMWt6Xr2u0ABG45F==\",\"rollingStartNumber\":2666874,\"rollingPeriod\":144,\"transmissionRisk\":6}]}";
    public final static String STORED_FEDERATED_KEYS_PAYLOAD_IE = "{\"temporaryExposureKeys\":[{\"key\":\"kzQt9Lf3xjtAlMtm7jkSqw==\",\"rollingStartNumber\":2666868,\"rollingPeriod\":144,\"transmissionRisk\":4}]}";

    public static final StoredTemporaryExposureKeyPayload STORED_KEYS_PAYLOAD_DESERIALIZED =
        new StoredTemporaryExposureKeyPayload(
            asList(
                new StoredTemporaryExposureKey("W2zb3BeMWt6Xr2u0ABG32Q==", 12345, 144, 7),
                new StoredTemporaryExposureKey("kzQt9Lf3xjtAlMtm7jkSqw==", 12499, 144, 7)
            )
        );

    public static final StoredTemporaryExposureKeyPayload STORED_KEYS_PAYLOAD_DESERIALIZED_DAYS_SINCE_ONSET =
        new StoredTemporaryExposureKeyPayload(
            asList(
                new StoredTemporaryExposureKey("W2zb3BeMWt6Xr2u0ABG32Q==", 12345, 144, 7, 1),
                new StoredTemporaryExposureKey("kzQt9Lf3xjtAlMtm7jkSqw==", 12499, 144, 7, 4)
            )
        );

    public static final String RISKY_VENUES_UPLOAD_PAYLOAD =
        "# venue_id, start_time, end_time\n" +
            "\"CD2\", \"2019-07-04T13:33:03Z\", \"2019-07-04T15:56:00Z\"\n" +
            "\"CD3\", \"2019-07-06T19:33:03Z\", \"2019-07-06T21:01:07Z\"\n" +
            "\"CD4\", \"2019-07-08T20:05:52Z\", \"2019-07-08T22:35:56Z\"";

    public static final String STORED_RISKY_VENUES_UPLOAD_PAYLOAD =
        "{\"venues\":[" +
            "{\"id\":\"CD2\",\"riskyWindow\":{\"from\":\"2019-07-04T13:33:03Z\",\"until\":\"2019-07-04T15:56:00Z\"},\"messageType\":\"M1\"}," +
            "{\"id\":\"CD3\",\"riskyWindow\":{\"from\":\"2019-07-06T19:33:03Z\",\"until\":\"2019-07-06T21:01:07Z\"},\"messageType\":\"M1\"}," +
            "{\"id\":\"CD4\",\"riskyWindow\":{\"from\":\"2019-07-08T20:05:52Z\",\"until\":\"2019-07-08T22:35:56Z\"},\"messageType\":\"M1\"}" +
            "]}";

    public static final String STORED_ANALYTICS_PAYLOAD_IOS_NEW_METRICS =
        "{\"startDate\":\"2020-07-27T23:00:00Z\",\"endDate\":\"2020-07-28T22:59:00Z\",\"postalDistrict\":\"AB10\",\"deviceModel\":\"iPhone11,2\",\"operatingSystemVersion\":\"iPhone OS 13.5.1 (17F80)\",\"latestApplicationVersion\":\"3.0\",\"cumulativeDownloadBytes\":140000000,\"cumulativeUploadBytes\":140000000,\"cumulativeCellularDownloadBytes\":80000000,\"cumulativeCellularUploadBytes\":70000000,\"cumulativeWifiDownloadBytes\":60000000,\"cumulativeWifiUploadBytes\":50000000,\"checkedIn\":1,\"canceledCheckIn\":1,\"receivedVoidTestResult\":1,\"isIsolatingBackgroundTick\":1,\"hasHadRiskyContactBackgroundTick\":1,\"receivedPositiveTestResult\":1,\"receivedNegativeTestResult\":1,\"hasSelfDiagnosedPositiveBackgroundTick\":1,\"completedQuestionnaireAndStartedIsolation\":1,\"encounterDetectionPausedBackgroundTick\":1,\"completedQuestionnaireButDidNotStartIsolation\":1,\"totalBackgroundTasks\":1,\"runningNormallyBackgroundTick\":1,\"completedOnboarding\":1,\"includesMultipleApplicationVersions\":false,\"receivedVoidTestResultEnteredManually\":1,\"receivedPositiveTestResultEnteredManually\":1,\"receivedNegativeTestResultEnteredManually\":0,\"receivedVoidTestResultViaPolling\":0,\"receivedPositiveTestResultViaPolling\":0,\"receivedNegativeTestResultViaPolling\":1,\"hasSelfDiagnosedBackgroundTick\":4,\"hasTestedPositiveBackgroundTick\":5,\"isIsolatingForSelfDiagnosedBackgroundTick\":6,\"isIsolatingForTestedPositiveBackgroundTick\":3,\"isIsolatingForHadRiskyContactBackgroundTick\":13}";
    public static final String STORED_ANALYTICS_PAYLOAD_IOS =
        "{\"startDate\":\"2020-07-27T23:00:00Z\",\"endDate\":\"2020-07-28T22:59:00Z\",\"postalDistrict\":\"AB10\",\"deviceModel\":\"iPhone11,2\",\"operatingSystemVersion\":\"iPhone OS 13.5.1 (17F80)\",\"latestApplicationVersion\":\"3.0\",\"cumulativeDownloadBytes\":140000000,\"cumulativeUploadBytes\":140000000,\"cumulativeCellularDownloadBytes\":80000000,\"cumulativeCellularUploadBytes\":70000000,\"cumulativeWifiDownloadBytes\":60000000,\"cumulativeWifiUploadBytes\":50000000,\"checkedIn\":1,\"canceledCheckIn\":1,\"receivedVoidTestResult\":1,\"isIsolatingBackgroundTick\":1,\"hasHadRiskyContactBackgroundTick\":1,\"receivedPositiveTestResult\":1,\"receivedNegativeTestResult\":1,\"hasSelfDiagnosedPositiveBackgroundTick\":1,\"completedQuestionnaireAndStartedIsolation\":1,\"encounterDetectionPausedBackgroundTick\":1,\"completedQuestionnaireButDidNotStartIsolation\":1,\"totalBackgroundTasks\":1,\"runningNormallyBackgroundTick\":1,\"completedOnboarding\":1,\"includesMultipleApplicationVersions\":false,\"receivedVoidTestResultEnteredManually\":null,\"receivedPositiveTestResultEnteredManually\":null,\"receivedNegativeTestResultEnteredManually\":null,\"receivedVoidTestResultViaPolling\":null,\"receivedPositiveTestResultViaPolling\":null,\"receivedNegativeTestResultViaPolling\":null,\"hasSelfDiagnosedBackgroundTick\":null,\"hasTestedPositiveBackgroundTick\":null,\"isIsolatingForSelfDiagnosedBackgroundTick\":null,\"isIsolatingForTestedPositiveBackgroundTick\":null,\"isIsolatingForHadRiskyContactBackgroundTick\":null}";
    public static final String STORED_ANALYTICS_MERGED_POSTCODE_PAYLOAD_IOS =
        "{\"startDate\":\"2020-07-27T23:00:00Z\",\"endDate\":\"2020-07-28T22:59:00Z\",\"postalDistrict\":\"AB13_AB14\",\"deviceModel\":\"iPhone11,2\",\"operatingSystemVersion\":\"iPhone OS 13.5.1 (17F80)\",\"latestApplicationVersion\":\"3.0\",\"cumulativeDownloadBytes\":140000000,\"cumulativeUploadBytes\":140000000,\"cumulativeCellularDownloadBytes\":80000000,\"cumulativeCellularUploadBytes\":70000000,\"cumulativeWifiDownloadBytes\":60000000,\"cumulativeWifiUploadBytes\":50000000,\"checkedIn\":1,\"canceledCheckIn\":1,\"receivedVoidTestResult\":1,\"isIsolatingBackgroundTick\":1,\"hasHadRiskyContactBackgroundTick\":1,\"receivedPositiveTestResult\":1,\"receivedNegativeTestResult\":1,\"hasSelfDiagnosedPositiveBackgroundTick\":1,\"completedQuestionnaireAndStartedIsolation\":1,\"encounterDetectionPausedBackgroundTick\":1,\"completedQuestionnaireButDidNotStartIsolation\":1,\"totalBackgroundTasks\":1,\"runningNormallyBackgroundTick\":1,\"completedOnboarding\":1,\"includesMultipleApplicationVersions\":false,\"receivedVoidTestResultEnteredManually\":null,\"receivedPositiveTestResultEnteredManually\":null,\"receivedNegativeTestResultEnteredManually\":null,\"receivedVoidTestResultViaPolling\":null,\"receivedPositiveTestResultViaPolling\":null,\"receivedNegativeTestResultViaPolling\":null,\"hasSelfDiagnosedBackgroundTick\":null,\"hasTestedPositiveBackgroundTick\":null,\"isIsolatingForSelfDiagnosedBackgroundTick\":null,\"isIsolatingForTestedPositiveBackgroundTick\":null,\"isIsolatingForHadRiskyContactBackgroundTick\":null}";
    public static final String STORED_ANALYTICS_UNKNOWN_POSTCODE_PAYLOAD_IOS =
        "{\"startDate\":\"2020-07-27T23:00:00Z\",\"endDate\":\"2020-07-28T22:59:00Z\",\"postalDistrict\":\"UNKNOWN\",\"deviceModel\":\"iPhone11,2\",\"operatingSystemVersion\":\"iPhone OS 13.5.1 (17F80)\",\"latestApplicationVersion\":\"3.0\",\"cumulativeDownloadBytes\":140000000,\"cumulativeUploadBytes\":140000000,\"cumulativeCellularDownloadBytes\":80000000,\"cumulativeCellularUploadBytes\":70000000,\"cumulativeWifiDownloadBytes\":60000000,\"cumulativeWifiUploadBytes\":50000000,\"checkedIn\":1,\"canceledCheckIn\":1,\"receivedVoidTestResult\":1,\"isIsolatingBackgroundTick\":1,\"hasHadRiskyContactBackgroundTick\":1,\"receivedPositiveTestResult\":1,\"receivedNegativeTestResult\":1,\"hasSelfDiagnosedPositiveBackgroundTick\":1,\"completedQuestionnaireAndStartedIsolation\":1,\"encounterDetectionPausedBackgroundTick\":1,\"completedQuestionnaireButDidNotStartIsolation\":1,\"totalBackgroundTasks\":1,\"runningNormallyBackgroundTick\":1,\"completedOnboarding\":1,\"includesMultipleApplicationVersions\":false,\"receivedVoidTestResultEnteredManually\":null,\"receivedPositiveTestResultEnteredManually\":null,\"receivedNegativeTestResultEnteredManually\":null,\"receivedVoidTestResultViaPolling\":null,\"receivedPositiveTestResultViaPolling\":null,\"receivedNegativeTestResultViaPolling\":null,\"hasSelfDiagnosedBackgroundTick\":null,\"hasTestedPositiveBackgroundTick\":null,\"isIsolatingForSelfDiagnosedBackgroundTick\":null,\"isIsolatingForTestedPositiveBackgroundTick\":null,\"isIsolatingForHadRiskyContactBackgroundTick\":null}";

    public static final String STORED_ANALYTICS_PAYLOAD_ANDROID =
        "{\"startDate\":\"2020-07-27T23:00:00Z\",\"endDate\":\"2020-07-28T22:59:00Z\",\"postalDistrict\":\"AB10\",\"deviceModel\":\"HUAWEI LDN-L21\",\"operatingSystemVersion\":\"29\",\"latestApplicationVersion\":\"3.0\",\"cumulativeDownloadBytes\":null,\"cumulativeUploadBytes\":null,\"cumulativeCellularDownloadBytes\":null,\"cumulativeCellularUploadBytes\":null,\"cumulativeWifiDownloadBytes\":null,\"cumulativeWifiUploadBytes\":null,\"checkedIn\":1,\"canceledCheckIn\":1,\"receivedVoidTestResult\":1,\"isIsolatingBackgroundTick\":1,\"hasHadRiskyContactBackgroundTick\":1,\"receivedPositiveTestResult\":1,\"receivedNegativeTestResult\":1,\"hasSelfDiagnosedPositiveBackgroundTick\":1,\"completedQuestionnaireAndStartedIsolation\":1,\"encounterDetectionPausedBackgroundTick\":1,\"completedQuestionnaireButDidNotStartIsolation\":1,\"totalBackgroundTasks\":1,\"runningNormallyBackgroundTick\":1,\"completedOnboarding\":1,\"includesMultipleApplicationVersions\":false,\"receivedVoidTestResultEnteredManually\":null,\"receivedPositiveTestResultEnteredManually\":null,\"receivedNegativeTestResultEnteredManually\":null,\"receivedVoidTestResultViaPolling\":null,\"receivedPositiveTestResultViaPolling\":null,\"receivedNegativeTestResultViaPolling\":null,\"hasSelfDiagnosedBackgroundTick\":null,\"hasTestedPositiveBackgroundTick\":null,\"isIsolatingForSelfDiagnosedBackgroundTick\":null,\"isIsolatingForTestedPositiveBackgroundTick\":null,\"isIsolatingForHadRiskyContactBackgroundTick\":null}";
    public static final String STORED_ANALYTICS_MERGED_POSTCODE_PAYLOAD_ANDROID =
        "{\"startDate\":\"2020-07-27T23:00:00Z\",\"endDate\":\"2020-07-28T22:59:00Z\",\"postalDistrict\":\"AB13_AB14\",\"deviceModel\":\"HUAWEI LDN-L21\",\"operatingSystemVersion\":\"29\",\"latestApplicationVersion\":\"3.0\",\"cumulativeDownloadBytes\":null,\"cumulativeUploadBytes\":null,\"cumulativeCellularDownloadBytes\":null,\"cumulativeCellularUploadBytes\":null,\"cumulativeWifiDownloadBytes\":null,\"cumulativeWifiUploadBytes\":null,\"checkedIn\":1,\"canceledCheckIn\":1,\"receivedVoidTestResult\":1,\"isIsolatingBackgroundTick\":1,\"hasHadRiskyContactBackgroundTick\":1,\"receivedPositiveTestResult\":1,\"receivedNegativeTestResult\":1,\"hasSelfDiagnosedPositiveBackgroundTick\":1,\"completedQuestionnaireAndStartedIsolation\":1,\"encounterDetectionPausedBackgroundTick\":1,\"completedQuestionnaireButDidNotStartIsolation\":1,\"totalBackgroundTasks\":1,\"runningNormallyBackgroundTick\":1,\"completedOnboarding\":1,\"includesMultipleApplicationVersions\":false,\"receivedVoidTestResultEnteredManually\":null,\"receivedPositiveTestResultEnteredManually\":null,\"receivedNegativeTestResultEnteredManually\":null,\"receivedVoidTestResultViaPolling\":null,\"receivedPositiveTestResultViaPolling\":null,\"receivedNegativeTestResultViaPolling\":null,\"hasSelfDiagnosedBackgroundTick\":null,\"hasTestedPositiveBackgroundTick\":null,\"isIsolatingForSelfDiagnosedBackgroundTick\":null,\"isIsolatingForTestedPositiveBackgroundTick\":null,\"isIsolatingForHadRiskyContactBackgroundTick\":null}";

    public static final String STORED_ANALYTICS_UNKNOWN_POSTCODE_PAYLOAD_ANDROID =
        "{\"startDate\":\"2020-07-27T23:00:00Z\",\"endDate\":\"2020-07-28T22:59:00Z\",\"postalDistrict\":\"UNKNOWN\",\"deviceModel\":\"HUAWEI LDN-L21\",\"operatingSystemVersion\":\"29\",\"latestApplicationVersion\":\"3.0\",\"cumulativeDownloadBytes\":null,\"cumulativeUploadBytes\":null,\"cumulativeCellularDownloadBytes\":null,\"cumulativeCellularUploadBytes\":null,\"cumulativeWifiDownloadBytes\":null,\"cumulativeWifiUploadBytes\":null,\"checkedIn\":1,\"canceledCheckIn\":1,\"receivedVoidTestResult\":1,\"isIsolatingBackgroundTick\":1,\"hasHadRiskyContactBackgroundTick\":1,\"receivedPositiveTestResult\":1,\"receivedNegativeTestResult\":1,\"hasSelfDiagnosedPositiveBackgroundTick\":1,\"completedQuestionnaireAndStartedIsolation\":1,\"encounterDetectionPausedBackgroundTick\":1,\"completedQuestionnaireButDidNotStartIsolation\":1,\"totalBackgroundTasks\":1,\"runningNormallyBackgroundTick\":1,\"completedOnboarding\":1,\"includesMultipleApplicationVersions\":false,\"receivedVoidTestResultEnteredManually\":null,\"receivedPositiveTestResultEnteredManually\":null,\"receivedNegativeTestResultEnteredManually\":null,\"receivedVoidTestResultViaPolling\":null,\"receivedPositiveTestResultViaPolling\":null,\"receivedNegativeTestResultViaPolling\":null,\"hasSelfDiagnosedBackgroundTick\":null,\"hasTestedPositiveBackgroundTick\":null,\"isIsolatingForSelfDiagnosedBackgroundTick\":null,\"isIsolatingForTestedPositiveBackgroundTick\":null,\"isIsolatingForHadRiskyContactBackgroundTick\":null}";

    public static final String EXPOSURE_NOTIFICATION_CIRCUIT_BREAKER_PAYLOAD =
        " {\"matchedKeyCount\" : 2,\n" +
        " \"daysSinceLastExposure\": 3,\n" +
        " \"maximumRiskScore\" : 150\n" +
        " }";

    public static TestResult positiveTestResultFor(TestResultPollingToken token) { 
        return new TestResult(token.value, "2020-04-23T18:34:03Z", "POSITIVE", "available"); 
    }
    public static final TestResult positiveTestResult = new TestResult("abc", "2020-04-23T18:34:03Z", "POSITIVE", "available");
    public static final TestResult pendingTestResult = new TestResult("abc", "", "", "pending");
}
