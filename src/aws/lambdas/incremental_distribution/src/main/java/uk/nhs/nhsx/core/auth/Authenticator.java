package uk.nhs.nhsx.core.auth;

public interface Authenticator {
    boolean isAuthenticated(String authorizationHeader);
}
