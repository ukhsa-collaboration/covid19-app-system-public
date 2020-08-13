package uk.nhs.nhsx.core.auth;

public interface ApiKeyAuthorizer {
    boolean authorize(ApiKey key);
}
