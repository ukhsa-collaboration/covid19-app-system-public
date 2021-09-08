package uk.nhs.nhsx.analyticssubmission

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.http4k.core.Method.POST
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.hasEntry
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import uk.nhs.nhsx.analyticssubmission.model.ClientAnalyticsSubmissionPayload
import uk.nhs.nhsx.core.Json
import uk.nhs.nhsx.core.SystemClock
import uk.nhs.nhsx.core.TestEnvironments
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.testhelper.ContextBuilder.Companion.aContext
import uk.nhs.nhsx.testhelper.assertions.AwsRuntimeAssertions.ProxyRequest.body
import uk.nhs.nhsx.testhelper.assertions.AwsRuntimeAssertions.ProxyRequest.headers
import uk.nhs.nhsx.testhelper.assertions.AwsRuntimeAssertions.ProxyRequest.method
import uk.nhs.nhsx.testhelper.assertions.AwsRuntimeAssertions.ProxyRequest.path
import uk.nhs.nhsx.testhelper.assertions.isSameAs
import uk.nhs.nhsx.testhelper.assertions.withCaptured

class AnalyticsSubmissionQueuedHandlerTest {

    private val events = RecordingEvents()
    private val analyticsSubmissionHandler = mockk<AnalyticsSubmissionHandler>()

    private val queuedHandler = AnalyticsSubmissionQueuedHandler(
        TestEnvironments.TEST.apply(
            mapOf(
                "MAINTENANCE_MODE" to "false",
                "custom_oai" to "OAI"
            )
        ),
        SystemClock.CLOCK,
        events,
        analyticsSubmissionHandler
    )

    @Test
    fun `handle valid request`() {
        val requestEventSlot = slot<APIGatewayProxyRequestEvent>()

        every {
            analyticsSubmissionHandler.handleRequest(
                capture(requestEventSlot),
                any()
            )
        } returns APIGatewayProxyResponseEvent()

        val event = SQSEvent().apply {
            records = listOf(SQSEvent.SQSMessage().apply { body = apiGatewayEventJson })
        }

        queuedHandler.handleRequest(event, aContext())

        expectThat(requestEventSlot).withCaptured {
            method.isSameAs(POST)
            path.isEqualTo("/submission/mobile-analytics")
            headers.hasEntry("User-Agent", "Custom User Agent String")
            body.get("json") { Json.readJsonOrNull<ClientAnalyticsSubmissionPayload>(this) }.isNotNull()
        }
    }

    @Test
    fun `handle valid request null body`() {
        val requestEventSlot = slot<APIGatewayProxyRequestEvent>()

        every {
            analyticsSubmissionHandler.handleRequest(
                capture(requestEventSlot),
                any()
            )
        } returns APIGatewayProxyResponseEvent()

        val event = SQSEvent().apply {
            records = listOf(SQSEvent.SQSMessage().apply {
                body = """
                    { 
                        "headers": { "HEADER": "HEADER_VALUE" },
                        "httpMethod": "POST",
                        "path": "/submission/mobile-analytics",
                        "body": null
                    }
                """.trimIndent()
            })
        }

        queuedHandler.handleRequest(event, aContext())

        expectThat(requestEventSlot).withCaptured {
            method.isSameAs(POST)
            path.isEqualTo("/submission/mobile-analytics")
            headers.hasEntry("HEADER", "HEADER_VALUE")
        }
    }

    private val apiGatewayEventJson = """
{
  "body": "{\"analyticsWindow\":{\"startDate\":\"2021-04-06T18:03:21Z\",\"endDate\":\"2021-04-08T18:03:21Z\"},\"metadata\":{\"postalDistrict\":\"AL1\",\"deviceModel\":\"df7b99da-30a4-4ab4-b644-8b1ae43f85eb\",\"operatingSystemVersion\":\"iPhone OS 13.5.1 (17F80)\",\"latestApplicationVersion\":\"3.0\",\"localAuthority\":\"E07000240\"},\"metrics\":{\"cumulativeDownloadBytes\":0,\"cumulativeUploadBytes\":1,\"cumulativeCellularDownloadBytes\":2,\"cumulativeCellularUploadBytes\":3,\"cumulativeWifiDownloadBytes\":4,\"cumulativeWifiUploadBytes\":5,\"checkedIn\":6,\"canceledCheckIn\":7,\"receivedVoidTestResult\":8,\"isIsolatingBackgroundTick\":9,\"hasHadRiskyContactBackgroundTick\":10,\"receivedPositiveTestResult\":11,\"receivedNegativeTestResult\":12,\"hasSelfDiagnosedPositiveBackgroundTick\":13,\"completedQuestionnaireAndStartedIsolation\":14,\"encounterDetectionPausedBackgroundTick\":15,\"completedQuestionnaireButDidNotStartIsolation\":16,\"totalBackgroundTasks\":17,\"runningNormallyBackgroundTick\":18,\"completedOnboarding\":19,\"receivedVoidTestResultEnteredManually\":20,\"receivedPositiveTestResultEnteredManually\":21,\"receivedNegativeTestResultEnteredManually\":22,\"receivedVoidTestResultViaPolling\":23,\"receivedPositiveTestResultViaPolling\":24,\"receivedNegativeTestResultViaPolling\":25,\"hasSelfDiagnosedBackgroundTick\":26,\"hasTestedPositiveBackgroundTick\":27,\"isIsolatingForSelfDiagnosedBackgroundTick\":28,\"isIsolatingForTestedPositiveBackgroundTick\":29,\"isIsolatingForHadRiskyContactBackgroundTick\":30,\"receivedRiskyContactNotification\":31,\"startedIsolation\":32,\"receivedPositiveTestResultWhenIsolatingDueToRiskyContact\":33,\"receivedActiveIpcToken\":34,\"haveActiveIpcTokenBackgroundTick\":35,\"selectedIsolationPaymentsButton\":36,\"launchedIsolationPaymentsApplication\":37,\"receivedPositiveLFDTestResultViaPolling\":38,\"receivedNegativeLFDTestResultViaPolling\":39,\"receivedVoidLFDTestResultViaPolling\":40,\"receivedPositiveLFDTestResultEnteredManually\":41,\"receivedNegativeLFDTestResultEnteredManually\":42,\"receivedVoidLFDTestResultEnteredManually\":43,\"hasTestedLFDPositiveBackgroundTick\":44,\"isIsolatingForTestedLFDPositiveBackgroundTick\":45,\"totalExposureWindowsNotConsideredRisky\":46,\"totalExposureWindowsConsideredRisky\":47,\"acknowledgedStartOfIsolationDueToRiskyContact\":48,\"hasRiskyContactNotificationsEnabledBackgroundTick\":49,\"totalRiskyContactReminderNotifications\":50,\"receivedUnconfirmedPositiveTestResult\":51,\"isIsolatingForUnconfirmedTestBackgroundTick\":52,\"launchedTestOrdering\":53,\"didHaveSymptomsBeforeReceivedTestResult\":54,\"didRememberOnsetSymptomsDateBeforeReceivedTestResult\":55,\"didAskForSymptomsOnPositiveTestEntry\":56,\"declaredNegativeResultFromDCT\":57,\"receivedPositiveSelfRapidTestResultViaPolling\":58,\"receivedNegativeSelfRapidTestResultViaPolling\":59,\"receivedVoidSelfRapidTestResultViaPolling\":60,\"receivedPositiveSelfRapidTestResultEnteredManually\":61,\"receivedNegativeSelfRapidTestResultEnteredManually\":62,\"receivedVoidSelfRapidTestResultEnteredManually\":63,\"isIsolatingForTestedSelfRapidPositiveBackgroundTick\":64,\"hasTestedSelfRapidPositiveBackgroundTick\":65,\"receivedRiskyVenueM1Warning\":66,\"receivedRiskyVenueM2Warning\":67,\"hasReceivedRiskyVenueM2WarningBackgroundTick\":68,\"totalAlarmManagerBackgroundTasks\":69,\"missingPacketsLast7Days\":70},\"includesMultipleApplicationVersions\":false}",
  "resource": "/{proxy+}",
  "path": "/submission/mobile-analytics",
  "httpMethod": "POST",
  "isBase64Encoded": true,
  "queryStringParameters": {
    "foo": "bar"
  },
  "multiValueQueryStringParameters": {
    "foo": [
      "bar"
    ]
  },
  "pathParameters": {
    "proxy": "/path/to/resource"
  },
  "stageVariables": {
    "baz": "qux"
  },
  "headers": {
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
    "Accept-Encoding": "gzip, deflate, sdch",
    "Accept-Language": "en-US,en;q=0.8",
    "Cache-Control": "max-age=0",
    "CloudFront-Forwarded-Proto": "https",
    "CloudFront-Is-Desktop-Viewer": "true",
    "CloudFront-Is-Mobile-Viewer": "false",
    "CloudFront-Is-SmartTV-Viewer": "false",
    "CloudFront-Is-Tablet-Viewer": "false",
    "CloudFront-Viewer-Country": "US",
    "Host": "1234567890.execute-api.eu-west-2.amazonaws.com",
    "Upgrade-Insecure-Requests": "1",
    "User-Agent": "Custom User Agent String",
    "Via": "1.1 08f323deadbeefa7af34d5feb414ce27.cloudfront.net (CloudFront)",
    "X-Amz-Cf-Id": "cDehVQoZnx43VYQb9j2-nvCh-9z396Uhbp027Y2JvkCPNLmGJHqlaA==",
    "X-Forwarded-For": "127.0.0.1, 127.0.0.2",
    "X-Forwarded-Port": "443",
    "X-Forwarded-Proto": "https"
  },
  "multiValueHeaders": {
    "Accept": [
      "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"
    ],
    "Accept-Encoding": [
      "gzip, deflate, sdch"
    ],
    "Accept-Language": [
      "en-US,en;q=0.8"
    ],
    "Cache-Control": [
      "max-age=0"
    ],
    "CloudFront-Forwarded-Proto": [
      "https"
    ],
    "CloudFront-Is-Desktop-Viewer": [
      "true"
    ],
    "CloudFront-Is-Mobile-Viewer": [
      "false"
    ],
    "CloudFront-Is-SmartTV-Viewer": [
      "false"
    ],
    "CloudFront-Is-Tablet-Viewer": [
      "false"
    ],
    "CloudFront-Viewer-Country": [
      "US"
    ],
    "Host": [
      "0123456789.execute-api.eu-west-2.amazonaws.com"
    ],
    "Upgrade-Insecure-Requests": [
      "1"
    ],
    "User-Agent": [
      "Custom User Agent String"
    ],
    "Via": [
      "1.1 08f323deadbeefa7af34d5feb414ce27.cloudfront.net (CloudFront)"
    ],
    "X-Amz-Cf-Id": [
      "cDehVQoZnx43VYQb9j2-nvCh-9z396Uhbp027Y2JvkCPNLmGJHqlaA=="
    ],
    "X-Forwarded-For": [
      "127.0.0.1, 127.0.0.2"
    ],
    "X-Forwarded-Port": [
      "443"
    ],
    "X-Forwarded-Proto": [
      "https"
    ]
  },
  "requestContext": {
    "accountId": "123456789012",
    "resourceId": "123456",
    "stage": "prod",
    "requestId": "c6af9ac6-7b61-11e6-9a41-93e8deadbeef",
    "requestTime": "09/Apr/2015:12:34:56 +0000",
    "requestTimeEpoch": 1428582896000,
    "identity": {
      "cognitoIdentityPoolId": null,
      "accountId": null,
      "cognitoIdentityId": null,
      "caller": null,
      "accessKey": null,
      "sourceIp": "127.0.0.1",
      "cognitoAuthenticationType": null,
      "cognitoAuthenticationProvider": null,
      "userArn": null,
      "userAgent": "Custom User Agent String",
      "user": null
    },
    "path": "/prod/path/to/resource",
    "resourcePath": "/{proxy+}",
    "httpMethod": "POST",
    "apiId": "1234567890",
    "protocol": "HTTP/1.1"
  }
}
    """.trimIndent()

}
