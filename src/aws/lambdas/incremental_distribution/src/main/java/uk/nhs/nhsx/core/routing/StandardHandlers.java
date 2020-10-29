package uk.nhs.nhsx.core.routing;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.nhs.nhsx.core.Environment;
import uk.nhs.nhsx.core.HttpResponses;
import uk.nhs.nhsx.core.auth.Authenticator;
import uk.nhs.nhsx.core.auth.ResponseSigner;
import uk.nhs.nhsx.core.exceptions.ApiResponseException;

import java.util.Base64;
import java.util.Optional;
import java.util.function.Consumer;

public class StandardHandlers {

    private static final Logger logger = LogManager.getLogger(StandardHandlers.class);
    
    private static final Environment.EnvironmentKey<String> MAINTENANCE_MODE = Environment.EnvironmentKey.string("MAINTENANCE_MODE");
    private static final Environment.EnvironmentKey<String> CUSTOM_OAI = Environment.EnvironmentKey.string("custom_oai");
    

    public static Routing.Handler withoutSignedResponses(Environment environment, Authenticator authenticator, Routing.Handler delegate) {
        return defaultStack(environment, authenticator, catchExceptions(delegate));
    }

    public static Routing.Handler withSignedResponses(Environment environment, Authenticator authenticator, ResponseSigner signer, Routing.Handler delegate) {
        return defaultStack(environment, authenticator, signedBy(signer, catchExceptions(delegate)));
    }

    private static Routing.Handler defaultStack(Environment environment, Authenticator authenticator, Routing.Handler handler) {
        return loggingIncomingRequests(
                filteringWhileMaintenanceModeEnabled(environment,
                requiringAuthorizationHeader(
                    requiringCustomAccessIdentity(environment,
                        authorisedBy(authenticator,
                            handler
                        )
                    )
                )
            )
        );
    }

    public static Routing.Handler loggingIncomingRequests(Routing.Handler delegate) {
        return r -> {
            String keyName =  apiKeyNameFrom(r.getHeaders().get("authorization")).orElse("none");
            String requestId = Optional.ofNullable(r.getHeaders().get("Request-Id")).orElse("none");
            logger.info("Received http request: method={}, path={},requestId={},apiKeyName={}", r.getHttpMethod(), r.getPath(),requestId,keyName);
            return delegate.handle(r);
        };
    }

    public static Routing.Handler filteringWhileMaintenanceModeEnabled(Environment environment, Routing.Handler delegate) {
        if (Boolean.parseBoolean(environment.access.required(MAINTENANCE_MODE))) {
            return r -> HttpResponses.serviceUnavailable();
        }
        else {
            return delegate;
        }
    }

    public static Routing.Handler requiringAuthorizationHeader(Routing.Handler delegate) {
        return r -> Optional.ofNullable(r.getHeaders().get("authorization")).map(x -> delegate.handle(r)).orElse(HttpResponses.forbidden());
    }

    public static Routing.Handler requiringCustomAccessIdentity(Environment environment, Routing.Handler delegate) {
        Optional<String> maybeRequiredOai = environment.access.optional(CUSTOM_OAI);

        return maybeRequiredOai.map(
            requiredOai -> (Routing.Handler) request -> {
                if (requiredOai.equals(request.getHeaders().get("x-custom-oai"))) {
                    logger.debug("Access to API Gateway via CloudFront facade (with x-custom-oai): method={}, path={}", request.getHttpMethod(), request.getPath());
                    return delegate.handle(request);
                } else {
                    logger.warn("Direct access to API Gateway (without x-custom-oai): method={}, path={}", request.getHttpMethod(), request.getPath());
                    return HttpResponses.forbidden();
                }
            }
        ).orElse(
            request -> {
                logger.error("API protection not configured (no expected x-custom-oai): method={}, path={}", request.getHttpMethod(), request.getPath());
                return delegate.handle(request);
            }
        );
    }

    public static Routing.Handler authorisedBy(Authenticator authenticator, Routing.Handler delegate) {
        return r -> Optional.ofNullable(r.getHeaders().get("authorization"))
            .filter(authenticator::isAuthenticated)
            .map(a -> delegate.handle(r))
            .orElse(HttpResponses.forbidden());
    }

    public static Routing.Handler signedBy(ResponseSigner signer, Routing.Handler delegate) {
        return request -> {
            APIGatewayProxyResponseEvent response = delegate.handle(request);
            signer.sign(request, response);
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

    public static Optional<String> apiKeyNameFrom(String authorizationHeader){
        return Optional.ofNullable(authorizationHeader)
            .filter(it -> it.startsWith("Bearer "))
            .map(it -> it.replaceFirst("Bearer ", ""))
            .flatMap(it -> {
                try {
                    byte[] decode = Base64.getDecoder().decode(it);
                    return Optional.of(new String(decode));
                } catch (IllegalArgumentException e) {
                    return Optional.empty();
                }
            })
            .flatMap(it -> {
                int colonPos = it.indexOf(":");
                if (colonPos == -1) return Optional.empty();
                return Optional.of(it.substring(0, colonPos));
            });
    }
}
