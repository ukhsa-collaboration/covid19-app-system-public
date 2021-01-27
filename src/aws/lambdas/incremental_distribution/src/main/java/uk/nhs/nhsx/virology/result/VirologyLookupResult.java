package uk.nhs.nhsx.virology.result;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.nhs.nhsx.core.HttpResponses;
import uk.nhs.nhsx.core.Jackson;
import uk.nhs.nhsx.virology.lookup.VirologyLookupResponse;
import uk.nhs.nhsx.virology.lookup.VirologyLookupResponseV2;

public abstract class VirologyLookupResult {

    private static final Logger logger = LogManager.getLogger(VirologyLookupResult.class);

    public abstract APIGatewayProxyResponseEvent toHttpResponse();

    public static class Available extends VirologyLookupResult {
        public final VirologyLookupResponse virologyLookupResponse;

        public Available(VirologyLookupResponse virologyLookupResponse) {
            this.virologyLookupResponse = virologyLookupResponse;
        }

        @Override
        public APIGatewayProxyResponseEvent toHttpResponse() {
            logger.info("Virology result found");
            return HttpResponses.ok(Jackson.toJson(virologyLookupResponse));
        }
    }

    public static class AvailableV2 extends VirologyLookupResult {
        public final VirologyLookupResponseV2 virologyLookupResponse;

        public AvailableV2(VirologyLookupResponseV2 virologyLookupResponse) {
            this.virologyLookupResponse = virologyLookupResponse;
        }

        @Override
        public APIGatewayProxyResponseEvent toHttpResponse() {
            logger.info("Virology result found");
            return HttpResponses.ok(Jackson.toJson(virologyLookupResponse));
        }
    }

    public static class Pending extends VirologyLookupResult {

        @Override
        public APIGatewayProxyResponseEvent toHttpResponse() {
            logger.info("Virology result not available yet");
            return HttpResponses.noContent();
        }

    }

    public static class NotFound extends VirologyLookupResult {

        @Override
        public APIGatewayProxyResponseEvent toHttpResponse() {
            logger.info("Virology result not found");
            return HttpResponses.notFound(
                "Test result lookup submitted for unknown testResultPollingToken"
            );
        }
    }
}
