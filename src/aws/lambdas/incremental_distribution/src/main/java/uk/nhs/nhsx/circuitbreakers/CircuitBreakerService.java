package uk.nhs.nhsx.circuitbreakers;

import uk.nhs.nhsx.circuitbreakers.utils.TokenGenerator;
import uk.nhs.nhsx.core.Jackson;
import uk.nhs.nhsx.core.aws.ssm.Parameter;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CircuitBreakerService {

    private static final Pattern CIRCUIT_BREAKER_RESOLUTION_PATH_PATTERN = Pattern.compile("/circuit-breaker/[\\-\\w]+/resolution/(?<pollingToken>.*)");

    private final Parameter<ApprovalStatus> initial;
    private final Parameter<ApprovalStatus> poll;

    public CircuitBreakerService(Parameter<ApprovalStatus> initial, Parameter<ApprovalStatus> poll) {
        this.initial = initial;
        this.poll = poll;
    }

    public CircuitBreakerResult getApprovalToken() {
        String token = TokenGenerator.getToken();

        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setApprovalToken(token);
        tokenResponse.setApproval(initial.value().getName());

        return CircuitBreakerResult.ok(Jackson.toJson(tokenResponse));
    }

    public CircuitBreakerResult getResolution(String path) {
        Optional<String> pollingToken = extractPollingToken(path);

        if (pollingToken.isEmpty()) {
            return CircuitBreakerResult.missingPollingTokenError();
        }

        ResolutionResponse resolutionResponse = new ResolutionResponse();
        ApprovalStatus status = poll.value();
        resolutionResponse.setApproval(status.getName());

        return CircuitBreakerResult.ok(Jackson.toJson(resolutionResponse));
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
