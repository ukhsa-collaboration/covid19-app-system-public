package uk.nhs.nhsx.virology.persistence;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.nhs.nhsx.core.HttpResponses;

public abstract class VirologyResultPersistOperation {

    private static final Logger logger = LogManager.getLogger(VirologyResultPersistOperation.class);

    public abstract APIGatewayProxyResponseEvent toHttpResponse();

    public static class Success extends VirologyResultPersistOperation {
        @Override
        public APIGatewayProxyResponseEvent toHttpResponse() {
            return HttpResponses.accepted("successfully processed");
        }
    }

    public static class TransactionFailed extends VirologyResultPersistOperation {
        private final String message;

        public TransactionFailed(String message) {
            this.message = message;
        }

        @Override
        public APIGatewayProxyResponseEvent toHttpResponse() {
            logger.warn(message);
            return HttpResponses.conflict();
        }
    }

    public static class OrderNotFound extends VirologyResultPersistOperation {
        @Override
        public APIGatewayProxyResponseEvent toHttpResponse() {
            logger.warn("Failed to persist test result to database. Call to get test order table did not return complete result.");
            return HttpResponses.badRequest();
        }
    }
}
