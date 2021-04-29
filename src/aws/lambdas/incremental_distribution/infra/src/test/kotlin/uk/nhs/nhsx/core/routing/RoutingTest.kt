package uk.nhs.nhsx.core.routing

import com.amazonaws.HttpMethod
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.HttpResponses
import uk.nhs.nhsx.core.auth.Authenticator
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.core.exceptions.HttpStatusCode.OK_200
import uk.nhs.nhsx.core.routing.authorisedBy
import uk.nhs.nhsx.core.routing.catchExceptions
import uk.nhs.nhsx.testhelper.ContextBuilder.TestContext
import uk.nhs.nhsx.testhelper.ProxyRequestBuilder.Companion.request
import uk.nhs.nhsx.testhelper.matchers.ProxyResponseAssertions.hasStatus
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
                .withMethod(HttpMethod.POST)
                .withBearerToken("something")
                .withPath("/a")
                .build(),
            TestContext()
        )

        MatcherAssert.assertThat(
            response,
            hasStatus(OK_200)
        )
        MatcherAssert.assertThat(chosen.get(), CoreMatchers.equalTo(1))
    }
}
