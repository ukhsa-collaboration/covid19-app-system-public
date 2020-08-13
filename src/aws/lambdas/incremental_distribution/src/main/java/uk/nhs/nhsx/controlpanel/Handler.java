package uk.nhs.nhsx.controlpanel;

import com.amazonaws.regions.Regions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;

import java.util.List;

/**
 * Scheduling strategy:
 * - cron(tbd)
 */
public class Handler implements RequestHandler<ScheduledEvent, String> {
    private static final Logger logger = LoggerFactory.getLogger(Handler.class);

    public String handleRequest(ScheduledEvent input, Context context) {
        try {
        	logger.info("Begin: Athena Queries");

        	List<String> queryExecutionIds = new AthenaService(Regions.EU_WEST_2, logger).executeAllQueries();

            logger.info("Query Execution Ids: ".concat(String.join(", ", queryExecutionIds)));

            logger.info("Success: Athena Queries");

            return "success";
        } catch (Exception e) {
        	logger.error("Failed: Athena Queries", e);
        	throw new RuntimeException(e);
        }
    }
}
