package uk.nhs.nhsx.core.routing

import com.amazonaws.HttpMethod.POST
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.HttpResponses
import uk.nhs.nhsx.core.auth.Authenticator
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.core.exceptions.HttpStatusCode.OK_200
import uk.nhs.nhsx.testhelper.ContextBuilder.TestContext
import uk.nhs.nhsx.testhelper.ProxyRequestBuilder.request
import uk.nhs.nhsx.testhelper.matchers.ProxyResponseAssertions.hasStatus
import uk.nhs.nhsx.testhelper.withBearerToken
import uk.nhs.nhsx.testhelper.withMethod
import java.util.concurrent.atomic.AtomicInteger

class RoutingTest {

    @Test
    fun someExample() {
        val chosen = AtomicInteger()
        val authenticator = Authenticator { true }
        val handler =
            catchExceptions(
                RecordingEvents(),
                authorisedBy(authenticator, Routing.routes(
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
            request()
                .withMethod(POST)
                .withBearerToken("something")
                .withPath("/a"),
            TestContext()
        )

        MatcherAssert.assertThat(
            response,
            hasStatus(OK_200)
        )
        MatcherAssert.assertThat(chosen.get(), CoreMatchers.equalTo(1))
    }
}
