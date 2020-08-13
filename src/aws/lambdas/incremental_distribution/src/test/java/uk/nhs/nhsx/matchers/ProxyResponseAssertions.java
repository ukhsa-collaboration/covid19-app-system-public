package uk.nhs.nhsx.matchers;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import uk.nhs.nhsx.core.exceptions.HttpStatusCode;

public class ProxyResponseAssertions {

    public static Matcher<APIGatewayProxyResponseEvent> hasStatus(HttpStatusCode code) {
        return new TypeSafeDiagnosingMatcher<APIGatewayProxyResponseEvent>() {
            @Override
            protected boolean matchesSafely(APIGatewayProxyResponseEvent event, Description description) {
                Integer actual = event.getStatusCode();
                description.appendText("status was ").appendValue(actual);
                return actual != null && actual.equals(code.code);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("code ").appendValue(code);
            }
        };
    }

    public static Matcher<APIGatewayProxyResponseEvent> hasHeader(String name, Matcher<String> value) {
        return new TypeSafeDiagnosingMatcher<APIGatewayProxyResponseEvent>() {
            @Override
            protected boolean matchesSafely(APIGatewayProxyResponseEvent event, Description mismatch) {
                String actual = event.getHeaders().get(name);
                if (!value.matches(actual)) {
                    mismatch.appendText("body").appendText(" ");
                    value.describeMismatch(actual, mismatch);
                    return false;
                } else {
                    return true;
                }
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("header ").appendValue(name).appendText(" ").appendDescriptionOf(value);
            }
        };
    }

    public static Matcher<APIGatewayProxyResponseEvent> hasBody(Matcher<String> body) {
        return new TypeSafeDiagnosingMatcher<APIGatewayProxyResponseEvent>() {
            @Override
            protected boolean matchesSafely(APIGatewayProxyResponseEvent event, Description mismatch) {
                String actual = event.getBody();
                if (!body.matches(actual)) {
                    mismatch.appendText("body").appendText(" ");
                    body.describeMismatch(actual, mismatch);
                    return false;
                } else {
                    return true;
                }
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("body ").appendDescriptionOf(body);
            }
        };
    }
}
