package uk.nhs.nhsx.emptysubmission;

import uk.nhs.nhsx.core.Environment;
import uk.nhs.nhsx.core.HttpResponses;
import uk.nhs.nhsx.core.auth.ApiName;
import uk.nhs.nhsx.core.auth.Authenticator;
import uk.nhs.nhsx.core.routing.Routing;
import uk.nhs.nhsx.core.routing.RoutingHandler;

import static uk.nhs.nhsx.core.auth.StandardAuthentication.awsAuthentication;
import static uk.nhs.nhsx.core.routing.Routing.Method.POST;
import static uk.nhs.nhsx.core.routing.Routing.path;
import static uk.nhs.nhsx.core.routing.Routing.routes;
import static uk.nhs.nhsx.core.routing.StandardHandlers.withoutSignedResponses;

public class EmptySubmissionHandler extends RoutingHandler {

    private final Routing.Handler handler;

    @SuppressWarnings("unused")
    public EmptySubmissionHandler() {
        this(Environment.fromSystem());
    }

    public EmptySubmissionHandler(Environment environment) {
        this(environment, awsAuthentication(ApiName.Mobile));
    }

    public EmptySubmissionHandler(Environment environment,
                                  Authenticator authenticator) {
        handler = withoutSignedResponses(
            environment,
            authenticator,
            routes(
                path(POST, "/submission/empty-submission", (r) ->
                    HttpResponses.ok()
                )
            )
        );
    }

    @Override
    public Routing.Handler handler() {
        return handler;
    }
}
