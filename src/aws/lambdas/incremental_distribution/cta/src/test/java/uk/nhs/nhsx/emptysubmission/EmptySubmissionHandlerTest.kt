package uk.nhs.nhsx.emptysubmission

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import uk.nhs.nhsx.core.TestEnvironments
import uk.nhs.nhsx.core.auth.Authenticator
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.testhelper.ContextBuilder.TestContext

class EmptySubmissionHandlerTest {

    private val events = RecordingEvents()

    private val environment = TestEnvironments.TEST.apply(
        mapOf(
            "MAINTENANCE_MODE" to "false",
            "custom_oai" to "OAI"
        )
    )

    private val request = APIGatewayProxyRequestEvent().apply {
        httpMethod = "POST"
        path = "/submission/empty-submission"
        headers = mapOf("authorization" to "hello", "x-custom-oai" to "OAI")
    }

    @Test
    fun `always returns OK if authenticated`() {
        val alwaysAuthenticated = Authenticator { true }

        val response = EmptySubmissionHandler(environment, events, alwaysAuthenticated)
            .handler()
            .invoke(request, TestContext())

        expectThat(response)
            .get(APIGatewayProxyResponseEvent::getStatusCode)
            .isEqualTo(200)
    }

    @Test
    fun `always returns FORBIDDEN if not authenticated`() {
        val alwaysAuthenticated = Authenticator { false }

        val response = EmptySubmissionHandler(environment, events, alwaysAuthenticated)
            .handler()
            .invoke(request, TestContext())

        expectThat(response)
            .get(APIGatewayProxyResponseEvent::getStatusCode)
            .isEqualTo(403)
    }
}
