package uk.nhs.nhsx.core.routing

import com.amazonaws.HttpMethod
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.HttpResponses
import uk.nhs.nhsx.core.auth.Authenticator
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.core.exceptions.HttpStatusCode
import uk.nhs.nhsx.core.routing.StandardHandlers.authorisedBy
import uk.nhs.nhsx.core.routing.StandardHandlers.catchExceptions
import uk.nhs.nhsx.testhelper.ContextBuilder.TestContext
import uk.nhs.nhsx.testhelper.ProxyRequestBuilder
import uk.nhs.nhsx.testhelper.matchers.ProxyResponseAssertions.hasStatus
import java.util.concurrent.atomic.AtomicInteger

class RoutingTest {

    @Test
    fun someExample() {
        val chosen = AtomicInteger()
        val authenticator = Authenticator { true }
        val handler = catchExceptions(RecordingEvents(), authorisedBy(authenticator, Routing.routes(
            Routing.path(Routing.Method.POST, "/a") { _, _ ->
                chosen.set(1)
                HttpResponses.ok()
            },
            Routing.path(Routing.Method.POST, "/b") { _, _ ->
                chosen.set(2)
                HttpResponses.ok()
            }
        )))

        val response = handler(
            ProxyRequestBuilder.request()
                .withMethod(HttpMethod.POST)
                .withBearerToken("something")
                .withPath("/a")
                .build(),
            TestContext()
        )

        assertThat(response, hasStatus(HttpStatusCode.OK_200))
        assertThat(chosen.get(), equalTo(1))
    }
}
