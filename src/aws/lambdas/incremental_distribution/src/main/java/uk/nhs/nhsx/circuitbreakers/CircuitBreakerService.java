package uk.nhs.nhsx.circuitbreakers;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import uk.nhs.nhsx.circuitbreakers.utils.TokenGenerator;
import uk.nhs.nhsx.core.HttpResponses;
import uk.nhs.nhsx.core.Jackson;
import uk.nhs.nhsx.core.aws.ssm.Parameter;
import uk.nhs.nhsx.core.exceptions.ApiResponseException;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static uk.nhs.nhsx.core.exceptions.HttpStatusCode.NOT_FOUND_404;

public class CircuitBreakerService {

    private static final Pattern CIRCUIT_BREAKER_RESOLUTION_PATH_PATTERN = Pattern.compile("/circuit-breaker/[\\-\\w]+/resolution/(?<pollingToken>.*)");

    private final Parameter<ApprovalStatus> initial;
    private final Parameter<ApprovalStatus> poll;

    public CircuitBreakerService(Parameter<ApprovalStatus> initial, Parameter<ApprovalStatus> poll) {
        this.initial = initial;
        this.poll = poll;
    }

    public APIGatewayProxyResponseEvent getApprovalToken() {
        String token = TokenGenerator.getToken();

        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setApprovalToken(token);
        tokenResponse.setApproval(initial.value().getName());

        return HttpResponses.ok(Jackson.toJson(tokenResponse));
    }

    public APIGatewayProxyResponseEvent getResolution(String path) {
        Optional<String> pollingToken = extractPollingToken(path);

        if (!pollingToken.isPresent()) {
            throw new ApiResponseException(NOT_FOUND_404, "Circuit Breaker request submitted without approval token");
        }
        ResolutionResponse resolutionResponse = new ResolutionResponse();
        ApprovalStatus status = poll.value();
        resolutionResponse.setApproval(status.getName());

        return HttpResponses.ok(Jackson.toJson(resolutionResponse));
    }

    public static Optional<String> extractPollingToken(String path) {
        return Optional.ofNullable(path).flatMap(it -> {
            Matcher matcher = CIRCUIT_BREAKER_RESOLUTION_PATH_PATTERN.matcher(it);
            if (matcher.matches()) {
                String pollingToken = matcher.group("pollingToken");
                if (pollingToken != null && !pollingToken.isEmpty()) return Optional.of(pollingToken);
            }
            return Optional.empty();
        });
    }

    public static Predicate<String> startsWith(String path) {
        return (candidate) -> candidate.startsWith(path);
    }
}
