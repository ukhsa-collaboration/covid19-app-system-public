package uk.nhs.nhsx.activationsubmission;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.http.entity.ContentType;
import uk.nhs.nhsx.activationsubmission.persist.DynamoDBActivationCodes;
import uk.nhs.nhsx.activationsubmission.persist.Environment;
import uk.nhs.nhsx.activationsubmission.persist.ExpiringPersistedActivationCodeLookup;
import uk.nhs.nhsx.activationsubmission.persist.TableNamingStrategy;
import uk.nhs.nhsx.activationsubmission.reporting.DynamoDbReporting;
import uk.nhs.nhsx.activationsubmission.validate.ActivationCodeValidator;
import uk.nhs.nhsx.activationsubmission.validate.CrockfordActivationCodeValidator;
import uk.nhs.nhsx.activationsubmission.validate.DevelopmentEnvironmentActivationValidator;
import uk.nhs.nhsx.activationsubmission.validate.PersistedActivationCodeValidator;
import uk.nhs.nhsx.core.HttpResponses;
import uk.nhs.nhsx.core.SystemClock;
import uk.nhs.nhsx.core.auth.ApiName;
import uk.nhs.nhsx.core.auth.Authenticator;
import uk.nhs.nhsx.core.auth.ResponseSigner;
import uk.nhs.nhsx.core.routing.Routing;
import uk.nhs.nhsx.core.routing.RoutingHandler;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

import static uk.nhs.nhsx.core.Jackson.deserializeMaybe;
import static uk.nhs.nhsx.core.StandardSigning.signResponseWithKeyGivenInSsm;
import static uk.nhs.nhsx.core.auth.StandardAuthentication.awsAuthentication;
import static uk.nhs.nhsx.core.routing.Routing.path;
import static uk.nhs.nhsx.core.routing.Routing.routes;
import static uk.nhs.nhsx.core.routing.StandardHandlers.expectingContentType;
import static uk.nhs.nhsx.core.routing.StandardHandlers.withSignedResponses;

public class Handler extends RoutingHandler {

    private final Routing.Handler handler;

    public Handler() {
        this(Environment.fromSystem(), SystemClock.CLOCK);
    }

    public Handler(Environment environment, Supplier<Instant> clock) {
        this(awsAuthentication(ApiName.Mobile), suitableValidatorFor(clock, environment), signResponseWithKeyGivenInSsm(clock, environment));
    }

    public Handler(Authenticator authenticator, ActivationCodeValidator validator, ResponseSigner signer) {
        this.handler = withSignedResponses(
            authenticator,
            signer,
            routes(
                path(Routing.Method.POST, "/activation/request",
                    expectingContentType(ContentType.APPLICATION_JSON, (r) ->
                        deserializeMaybe(r.getBody(), ActivationRequest.class)
                            .filter(ar -> validator.validate(ar.activationCode))
                            .map(v -> HttpResponses.ok())
                            .orElse(HttpResponses.badRequest())
                    )
                )));
    }

    @Override
    public Routing.Handler handler() {
        return handler;
    }

    private static ActivationCodeValidator suitableValidatorFor(Supplier<Instant> clock, Environment environment) {
        AmazonDynamoDB db = AmazonDynamoDBClientBuilder.standard()
            .build();

        TableNamingStrategy namingStrategy = TableNamingStrategy.ENVIRONMENTAL.apply(environment);

        ActivationCodeValidator storedCodes = new PersistedActivationCodeValidator(
            new ExpiringPersistedActivationCodeLookup(
                clock,
                Duration.ofMinutes(2),
                new DynamoDBActivationCodes(db, namingStrategy, clock),
                new DynamoDbReporting(db, namingStrategy, clock)
            ));

        ActivationCodeValidator codes = environment.whenNonProduction(
            "Special Activation Codes",
            (e) -> storedCodes.or(new DevelopmentEnvironmentActivationValidator())
        ).orElse(storedCodes);

        return new CrockfordActivationCodeValidator().and(codes);
    }

    public static class ActivationRequest {
        public final ActivationCode activationCode;

        @JsonCreator // required because of https://github.com/FasterXML/jackson-databind/issues/1498
        public ActivationRequest(ActivationCode activationCode) {
            this.activationCode = activationCode;
        }
    }
}
