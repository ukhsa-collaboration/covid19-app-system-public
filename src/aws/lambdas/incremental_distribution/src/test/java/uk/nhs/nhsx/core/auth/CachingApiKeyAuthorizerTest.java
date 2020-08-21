package uk.nhs.nhsx.core.auth;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class CachingApiKeyAuthorizerTest {

    private boolean result = true;

    private final AtomicInteger count = new AtomicInteger();

    private String expected = "name";
    private final ApiKeyAuthorizer delegate = (k) -> {
        count.incrementAndGet();
        assertThat(k.keyName, equalTo(expected));
        return result;
    };

    private final ApiKeyAuthorizer authorizer = new CachingApiKeyAuthorizer(delegate);

    @Test
    public void cachesPositiveResult() throws Exception {
        result = true;
        assertThat(authorizer.authorize(ApiKey.of("name", "value")), is(true));
        assertThat(count.get(), is(1));
        assertThat(authorizer.authorize(ApiKey.of("name", "value")), is(true));
        assertThat(count.get(), is(1));
    }

    @Test
    public void cachesNegativeResult() throws Exception {
        result = false;
        assertThat(authorizer.authorize(ApiKey.of("name", "value")), is(false));
        assertThat(count.get(), is(1));
        assertThat(authorizer.authorize(ApiKey.of("name", "value")), is(false));
        assertThat(count.get(), is(1));
    }

    @Test
    public void doesNotMixUpResultsByKeyName() throws Exception {
        result = true;
        assertThat(authorizer.authorize(ApiKey.of("name", "value")), is(true));
        assertThat(count.get(), is(1));

        result = false;
        expected = "somekey";
        assertThat(authorizer.authorize(ApiKey.of("somekey", "somevalue")), is(false));

        assertThat(count.get(), is(2));
    }

    @Test
    public void doesNotMixUpResultsByKeyValue() throws Exception {
        result = true;
        assertThat(authorizer.authorize(ApiKey.of("name", "value")), is(true));
        assertThat(count.get(), is(1));

        result = false;
        assertThat(authorizer.authorize(ApiKey.of("name", "othervalue")), is(false));
        
        assertThat(authorizer.authorize(ApiKey.of("name", "value")), is(true));

        assertThat(count.get(), is(2));
    }
}