package uk.nhs.nhsx.core.auth;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

public interface ResponseSigner {
    void sign(APIGatewayProxyRequestEvent request, APIGatewayProxyResponseEvent response);
}
