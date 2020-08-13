package uk.nhs.nhsx.core;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class ContentTypes {

    public static Optional<String> contentTypeMaybe(APIGatewayProxyRequestEvent request) {
        return Stream.of("Content-Type", "content-type")
            .map(it -> request.getHeaders().get(it))
            .filter(Objects::nonNull)
            .findFirst();
    }

    public static boolean isTextCsv(APIGatewayProxyRequestEvent request) {
        return contentTypeMaybe(request)
            .filter(it -> it.equals("text/csv"))
            .isPresent();
    }
}
