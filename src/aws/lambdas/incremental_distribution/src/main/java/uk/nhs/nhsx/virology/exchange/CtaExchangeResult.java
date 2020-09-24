package uk.nhs.nhsx.virology.exchange;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.nhs.nhsx.core.HttpResponses;
import uk.nhs.nhsx.core.Jackson;

public abstract class CtaExchangeResult {

    private static final Logger logger = LogManager.getLogger(CtaExchangeResult.class);

    public abstract APIGatewayProxyResponseEvent toHttpResponse();

    public static class Available extends CtaExchangeResult {
        public final CtaExchangeResponse ctaExchangeResponse;

        public Available(CtaExchangeResponse ctaExchangeResponse) {
            this.ctaExchangeResponse = ctaExchangeResponse;
        }

        @Override
        public APIGatewayProxyResponseEvent toHttpResponse() {
            logger.info("Cta token exchange: ctaToken found");
            return HttpResponses.ok(Jackson.toJson(ctaExchangeResponse));
        }
    }

    public static class Pending extends CtaExchangeResult {
        @Override
        public APIGatewayProxyResponseEvent toHttpResponse() {
            logger.info("Cta token exchange: virology result not available yet");
            return HttpResponses.noContent();
        }

    }

    public static class NotFound extends CtaExchangeResult {
        @Override
        public APIGatewayProxyResponseEvent toHttpResponse() {
            logger.warn("Cta token exchange: ctaToken not found");
            return HttpResponses.notFound();
        }
    }
}