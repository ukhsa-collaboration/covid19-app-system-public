package uk.nhs.nhsx.core.routing;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.nhs.nhsx.core.HttpResponses;
import uk.nhs.nhsx.core.auth.Authenticator;
import uk.nhs.nhsx.core.auth.ResponseSigner;
import uk.nhs.nhsx.core.exceptions.ApiResponseException;

import java.util.Optional;
import java.util.function.Consumer;

public class StandardHandlers {

    private static final Logger logger = LoggerFactory.getLogger(StandardHandlers.class);

    public static Routing.Handler withoutSignedResponses(Authenticator authenticator, Routing.Handler delegate) {
        return authorisedBy(authenticator,
            catchExceptions(delegate)
        );
    }

    public static Routing.Handler withSignedResponses(Authenticator authenticator, ResponseSigner signer, Routing.Handler delegate) {
        return authorisedBy(authenticator,
            signedBy(signer, catchExceptions(delegate))
        );
    }

    public static Routing.Handler authorisedBy(Authenticator authenticator, Routing.Handler delegate) {
        return (r) -> {
            String authorization = r.getHeaders().get("authorization");
            if (authorization == null) return HttpResponses.forbidden();
            return !authenticator.isAuthenticated(authorization) ? HttpResponses.forbidden() : delegate.handle(r);
        };
    }

    public static Routing.Handler signedBy(ResponseSigner signer, Routing.Handler delegate) {
        return (r) -> {
            APIGatewayProxyResponseEvent response = delegate.handle(r);
            signer.sign(r, response);
            return response;
        };
    }

    public static Routing.Handler responseFilter(Routing.Handler delegate, Consumer<APIGatewayProxyResponseEvent> fiddler) {
        return (r) -> {
            APIGatewayProxyResponseEvent response = delegate.handle(r);
            fiddler.accept(response);
            return response;
        };
    }

    public static Routing.Handler expectingContentType(ContentType contentType, Routing.Handler handler) {
        return r -> {
            Optional<String> given = Optional.ofNullable(r.getHeaders().get("Content-Type"));
            if (given.isPresent()) {
                return given
                    .filter(c -> contentType.getMimeType().equals(ContentType.parse(c).getMimeType()))
                    .map(c -> handler.handle(r))
                    .orElse(HttpResponses.unprocessableEntity());
            } else {
                return HttpResponses.badRequest();
            }
        };
    }

    public static Routing.Handler catchExceptions(Routing.Handler delegate) {
        return (r) -> {
            try {
                return delegate.handle(r);
            } catch (ApiResponseException e) {
                logger.info("Failed to handle request: error={}, returned status code={}", e.getMessage(), e.statusCode.code);
                return HttpResponses.withStatusCodeAndBody(e.statusCode, e.getMessage());
            } catch (Exception e) {
                logger.error("Uncaught exception {}", e.getMessage(), e);
                return HttpResponses.internalServerError();
            }
        };
    }
}
