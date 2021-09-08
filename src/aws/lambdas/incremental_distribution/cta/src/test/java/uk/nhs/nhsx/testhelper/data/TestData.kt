package uk.nhs.nhsx.testhelper.data

import com.amazonaws.services.logs.model.ResultField
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKeyPayload
import uk.nhs.nhsx.domain.TestEndDate
import uk.nhs.nhsx.domain.TestKit
import uk.nhs.nhsx.domain.TestKit.LAB_RESULT
import uk.nhs.nhsx.domain.TestKit.RAPID_RESULT
import uk.nhs.nhsx.domain.TestResult.Negative
import uk.nhs.nhsx.domain.TestResult.Positive
import uk.nhs.nhsx.domain.TestResult.Void
import uk.nhs.nhsx.domain.TestResultPollingToken
import uk.nhs.nhsx.virology.persistence.TestState
import uk.nhs.nhsx.virology.persistence.TestState.AvailableTestResult

object TestData {

    const val STORED_KEYS_PAYLOAD = """{"temporaryExposureKeys":[{"key":"W2zb3BeMWt6Xr2u0ABG32Q==","rollingStartNumber":12345,"rollingPeriod":144,"transmissionRisk":7},{"key":"kzQt9Lf3xjtAlMtm7jkSqw==","rollingStartNumber":12499,"rollingPeriod":144,"transmissionRisk":7}]}"""
    const val STORED_KEYS_PAYLOAD_SUBMISSION = """{"temporaryExposureKeys":[{"key":"W2zb3BeMWt6Xr2u0ABG32Q==","rollingStartNumber":2666736,"rollingPeriod":144,"transmissionRisk":7},{"key":"kzQt9Lf3xjtAlMtm7jkSqw==","rollingStartNumber":2664864,"rollingPeriod":144,"transmissionRisk":7}]}"""
    const val STORED_KEYS_PAYLOAD_DAYS_SINCE_ONSET = """{"temporaryExposureKeys":[{"key":"W2zb3BeMWt6Xr2u0ABG32Q==","rollingStartNumber":12345,"rollingPeriod":144,"transmissionRisk":7,"daysSinceOnsetOfSymptoms":1},{"key":"kzQt9Lf3xjtAlMtm7jkSqw==","rollingStartNumber":12499,"rollingPeriod":144,"transmissionRisk":7,"daysSinceOnsetOfSymptoms":4}]}"""
    const val STORED_KEYS_PAYLOAD_DAYS_SINCE_ONSET_SUBMISSION = """{"temporaryExposureKeys":[{"key":"W2zb3BeMWt6Xr2u0ABG32Q==","rollingStartNumber":2666736,"rollingPeriod":144,"transmissionRisk":7,"daysSinceOnsetOfSymptoms":1},{"key":"kzQt9Lf3xjtAlMtm7jkSqw==","rollingStartNumber":2664864,"rollingPeriod":144,"transmissionRisk":7,"daysSinceOnsetOfSymptoms":4}]}"""
    const val STORED_KEYS_PAYLOAD_WITH_RISK_LEVEL = """{"temporaryExposureKeys":[{"key":"W2zb3BeMWt6Xr2u0ABG32Q==","rollingStartNumber":2666736,"rollingPeriod":144,"transmissionRisk":5},{"key":"kzQt9Lf3xjtAlMtm7jkSqw==","rollingStartNumber":2664864,"rollingPeriod":144,"transmissionRisk":4}]}"""
    const val STORED_KEYS_PAYLOAD_ONE_KEY = """{"temporaryExposureKeys":[{"key":"W2zb3BeMWt6Xr2u0ABG32Q==","rollingStartNumber":2666736,"rollingPeriod":144,"transmissionRisk":7}]}"""
    const val STORED_FEDERATED_KEYS_PAYLOAD_NI = """{"temporaryExposureKeys":[{"key":"W2zb3BeMWt6Xr2u0ABG32Q==","rollingStartNumber":2666736,"rollingPeriod":144,"transmissionRisk":3,"daysSinceOnsetOfSymptoms":0},{"key":"B3xb3BeMWt6Xr2u0ABG45F==","rollingStartNumber":2666874,"rollingPeriod":144,"transmissionRisk":6,"daysSinceOnsetOfSymptoms":0}]}"""
    const val STORED_FEDERATED_KEYS_PAYLOAD_IE = """{"temporaryExposureKeys":[{"key":"kzQt9Lf3xjtAlMtm7jkSqw==","rollingStartNumber":2666868,"rollingPeriod":144,"transmissionRisk":4,"daysSinceOnsetOfSymptoms":0}]}"""

    val STORED_KEYS_PAYLOAD_DESERIALIZED = StoredTemporaryExposureKeyPayload(
        listOf(
            StoredTemporaryExposureKey("W2zb3BeMWt6Xr2u0ABG32Q==", 12345, 144, 7),
            StoredTemporaryExposureKey("kzQt9Lf3xjtAlMtm7jkSqw==", 12499, 144, 7)
        )
    )

    val STORED_KEYS_PAYLOAD_DESERIALIZED_DAYS_SINCE_ONSET = StoredTemporaryExposureKeyPayload(
        listOf(
            StoredTemporaryExposureKey("W2zb3BeMWt6Xr2u0ABG32Q==", 12345, 144, 7, 1),
            StoredTemporaryExposureKey("kzQt9Lf3xjtAlMtm7jkSqw==", 12499, 144, 7, 4)
        )
    )

    const val RISKY_VENUES_UPLOAD_PAYLOAD = """# venue_id, start_time, end_time, message_type, optional_parameter
                                                "CD2", "2019-07-04T13:33:03Z", "2019-07-04T15:56:00Z", "M1", ""
                                                "CD3", "2019-07-06T19:33:03Z", "2019-07-06T21:01:07Z", "M1", ""
                                                "CD4", "2019-07-08T20:05:52Z", "2019-07-08T22:35:56Z", "M1", """""

    const val STORED_RISKY_VENUES_UPLOAD_PAYLOAD = """{"venues":[{"id":"CD2","riskyWindow":{"from":"2019-07-04T13:33:03Z","until":"2019-07-04T15:56:00Z"},"messageType":"M1"},{"id":"CD3","riskyWindow":{"from":"2019-07-06T19:33:03Z","until":"2019-07-06T21:01:07Z"},"messageType":"M1"},{"id":"CD4","riskyWindow":{"from":"2019-07-08T20:05:52Z","until":"2019-07-08T22:35:56Z"},"messageType":"M1"}]}"""
    const val EXPOSURE_NOTIFICATION_CIRCUIT_BREAKER_PAYLOAD = """ {"matchedKeyCount" : 2,
 "daysSinceLastExposure": 3,
 "maximumRiskScore" : 150
 }"""

    val positiveLabResult = AvailableTestResult(
        TestResultPollingToken.of("abc"),
        TestEndDate.of(2020, 4, 23),
        Positive,
        LAB_RESULT
    )
    val pendingTestResult = TestState.PendingTestResult(
        TestResultPollingToken.of("abc"),
        LAB_RESULT
    )
    val positiveRapidResult = AvailableTestResult(
        TestResultPollingToken.of("abc"),
        TestEndDate.of(2020, 4, 23),
        Positive,
        RAPID_RESULT
    )

    fun positiveResultFor(token: TestResultPollingToken, testKit: TestKit = LAB_RESULT): AvailableTestResult =
        AvailableTestResult(
            token,
            TestEndDate.of(2020, 4, 23),
            Positive,
            testKit
        )

    fun negativeResultFor(testKit: TestKit): AvailableTestResult =
        AvailableTestResult(
            TestResultPollingToken.of("abc"),
            TestEndDate.of(2020, 4, 23),
            Negative,
            testKit
        )

    fun voidResultFor(testKit: TestKit): AvailableTestResult =
        AvailableTestResult(
            TestResultPollingToken.of("abc"),
            TestEndDate.of(2020, 4, 23),
            Void,
            testKit
        )

    const val tokenGenPayloadV1 = """{"testEndDate":"2020-09-07T00:00:00Z","testResult":"POSITIVE"}"""
    const val tokenGenPayloadV2 = """{"testEndDate":"2020-09-07T00:00:00Z","testResult":"POSITIVE","testKit":"RAPID_RESULT"}"""
    const val tokenGenSelfReportedPayloadV2 = """{"testEndDate":"2020-09-07T00:00:00Z","testResult":"POSITIVE","testKit":"RAPID_SELF_REPORTED"}"""

    const val testLabResultV1 = """{"ctaToken": "cc8f0b6z","testEndDate": "2020-04-23T00:00:00Z","testResult": "NEGATIVE"}"""
    const val rapidLabResultV2 = """{"ctaToken": "cc8f0b6z","testEndDate": "2020-04-23T00:00:00Z","testResult": "POSITIVE","testKit": "RAPID_RESULT"}"""
    const val rapidSelfReportedResultV2 = """{"ctaToken": "cc8f0b6z","testEndDate": "2020-04-23T00:00:00Z","testResult": "POSITIVE","testKit": "RAPID_SELF_REPORTED"}"""

    const val tokenStatusPayloadV2 = """{"ctaToken":"cc8f0b6z"}"""

    val expectedCtaTokenGenStats = listOf(
        listOf(
            ResultField()
                .withField("start_date")
                .withValue("2021-05-04 01:00:00.000"),
            ResultField()
                .withField("test_type")
                .withValue("RAPID_SELF_REPORTED"),
            ResultField()
                .withField("source")
                .withValue("Eng"),
            ResultField()
                .withField("result_type")
                .withValue("NEGATIVE"),
            ResultField()
                .withField("total")
                .withValue("100")
        ),
        listOf(
            ResultField()
                .withField("start_date")
                .withValue("2021-05-04 01:00:00.000"),
            ResultField()
                .withField("test_type")
                .withValue("RAPID_SELF_REPORTED"),
            ResultField()
                .withField("source")
                .withValue("Wls"),
            ResultField()
                .withField("result_type")
                .withValue("POSITIVE"),
            ResultField()
                .withField("total")
                .withValue("450")
        )
    )
    val expectedCircuitBreakerLogs = listOf(
        listOf(
            ResultField()
                .withField("start_of_hour")
                .withValue("2021-01-20 16:00:00.000"),
            ResultField()
                .withField("exposure_notification_cb_count")
                .withValue("324"),
            ResultField()
                .withField("iOS_exposure_notification_cb_count")
                .withValue("143"),
            ResultField()
                .withField("android_exposure_notification_cb_count")
                .withValue("181"),
            ResultField()
                .withField("unique_request_ids")
                .withValue("0"),
            ResultField()
                .withField("app_version")
                .withValue("0.1.2")
        ),
        listOf(
            ResultField()
                .withField("start_of_hour")
                .withValue("2021-01-20 15:00:00.000"),
            ResultField()
                .withField("exposure_notification_cb_count")
                .withValue("324"),
            ResultField()
                .withField("iOS_exposure_notification_cb_count")
                .withValue("143"),
            ResultField()
                .withField("android_exposure_notification_cb_count")
                .withValue("181"),
            ResultField()
                .withField("unique_request_ids")
                .withValue("0"),
            ResultField()
                .withField("app_version")
                .withValue("3.4.5")
        )
    )

    val expectedInterOpDownloadStats = listOf(
        listOf(
            ResultField()
                .withField("start_of_hour")
                .withValue("2021-01-20 16:00:00.000"),
            ResultField()
                .withField("origin")
                .withValue("GB-SCO"),
            ResultField()
                .withField("test_type")
                .withValue("0"),
            ResultField()
                .withField("number_of_keys_downloaded")
                .withValue("100"),
            ResultField()
                .withField("number_of_keys_imported")
                .withValue("100")
        ),
        listOf(
            ResultField()
                .withField("start_of_hour")
                .withValue("2021-01-20 17:00:00.000"),
            ResultField()
                .withField("origin")
                .withValue("JE"),
            ResultField()
                .withField("test_type")
                .withValue("1"),
            ResultField()
                .withField("number_of_keys_downloaded")
                .withValue("101"),
            ResultField()
                .withField("number_of_keys_imported")
                .withValue("99"),
        )
    )

    val expectedInterOpUploadStats = listOf(
        listOf(
            ResultField()
                .withField("start_of_hour")
                .withValue("2021-01-20 16:00:00.000"),
            ResultField()
                .withField("test_type")
                .withValue("0"),
            ResultField()
                .withField("number_of_keys_uploaded")
                .withValue("100")
        ),
        listOf(
            ResultField()
                .withField("start_of_hour")
                .withValue("2021-01-20 17:00:00.000"),
            ResultField()
                .withField("test_type")
                .withValue("1"),
            ResultField()
                .withField("number_of_keys_uploaded")
                .withValue("101")
        )
    )
    val expectedCtaExchangeStats = listOf(
        listOf(
            ResultField()
                .withField("start_date")
                .withValue("2021-05-04 01:00:00.000"),
            ResultField()
                .withField("test_type")
                .withValue("RAPID_SELF_REPORTED"),
            ResultField()
                .withField("platform")
                .withValue("iOS"),
            ResultField()
                .withField("token_age_range")
                .withValue("GREATER_THAN_48_HOURS"),
            ResultField()
                .withField("source")
                .withValue("England"),
            ResultField()
                .withField("total")
                .withValue("300"),
            ResultField()
                .withField("app_version")
                .withValue("4.10")
        ),
        listOf(
            ResultField()
                .withField("start_date")
                .withValue("2021-05-04 01:00:00.000"),
            ResultField()
                .withField("test_type")
                .withValue("RAPID_SELF_REPORTED"),
            ResultField()
                .withField("platform")
                .withValue("Android"),
            ResultField()
                .withField("token_age_range")
                .withValue("GREATER_THAN_48_HOURS"),
            ResultField()
                .withField("source")
                .withValue("England"),
            ResultField()
                .withField("total")
                .withValue("200"),
            ResultField()
                .withField("app_version")
                .withValue("4.10")
        )
    )

    val expectedCtaTokenStatusStats = listOf(
        listOf(
            ResultField()
                .withField("start_date")
                .withValue("2021-05-04 01:00:00.000"),
            ResultField()
                .withField("test_type")
                .withValue("RAPID_SELF_REPORTED"),
            ResultField()
                .withField("source")
                .withValue("Eng"),
            ResultField()
                .withField("total")
                .withValue("200")
        ),
        listOf(
            ResultField()
                .withField("start_date")
                .withValue("2021-05-04 01:00:00.000"),
            ResultField()
                .withField("test_type")
                .withValue("LAB_RESULT"),
            ResultField()
                .withField("source")
                .withValue("Wls"),
            ResultField()
                .withField("total")
                .withValue("100")
        )
    )

}
