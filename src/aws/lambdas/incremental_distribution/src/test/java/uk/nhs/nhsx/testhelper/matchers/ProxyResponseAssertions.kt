package uk.nhs.nhsx.testhelper.matchers

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeDiagnosingMatcher
import uk.nhs.nhsx.core.exceptions.HttpStatusCode

object ProxyResponseAssertions {

    fun hasStatus(code: HttpStatusCode): Matcher<APIGatewayProxyResponseEvent> {
        return object : TypeSafeDiagnosingMatcher<APIGatewayProxyResponseEvent>() {
            override fun matchesSafely(event: APIGatewayProxyResponseEvent, description: Description): Boolean {
                val actual = event.statusCode
                description.appendText("status was ").appendValue(actual)
                return actual != null && actual == code.code
            }

            override fun describeTo(description: Description) {
                description.appendText("code ").appendValue(code)
            }
        }
    }

    fun hasHeader(name: String, value: Matcher<String>): Matcher<APIGatewayProxyResponseEvent> {
        return object : TypeSafeDiagnosingMatcher<APIGatewayProxyResponseEvent>() {
            override fun matchesSafely(event: APIGatewayProxyResponseEvent, mismatch: Description): Boolean {
                val actual = event.headers[name]
                return if (!value.matches(actual)) {
                    mismatch.appendText("body").appendText(" ")
                    value.describeMismatch(actual, mismatch)
                    false
                } else {
                    true
                }
            }

            override fun describeTo(description: Description) {
                description.appendText("header ").appendValue(name).appendText(" ").appendDescriptionOf(value)
            }
        }
    }

    fun hasHeader(name: String): Matcher<APIGatewayProxyResponseEvent> {
        return object : TypeSafeDiagnosingMatcher<APIGatewayProxyResponseEvent>() {
            override fun matchesSafely(event: APIGatewayProxyResponseEvent, mismatch: Description): Boolean =
                event.headers[name] != null

            override fun describeTo(description: Description) {
                description.appendText("header ").appendValue(name)
            }
        }
    }

    fun hasBody(body: Matcher<String>): Matcher<APIGatewayProxyResponseEvent> {
        return object : TypeSafeDiagnosingMatcher<APIGatewayProxyResponseEvent>() {
            override fun matchesSafely(event: APIGatewayProxyResponseEvent, mismatch: Description): Boolean {
                val actual = event.body
                return if (!body.matches(actual)) {
                    mismatch.appendText("body").appendText(" ")
                    body.describeMismatch(actual, mismatch)
                    false
                } else {
                    true
                }
            }

            override fun describeTo(description: Description) {
                description.appendText("body ").appendDescriptionOf(body)
            }
        }
    }
}
