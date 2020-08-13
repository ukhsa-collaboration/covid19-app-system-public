package uk.nhs.nhsx.controlpanel;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.athena.AmazonAthena;
import com.amazonaws.services.athena.AmazonAthenaClient;
import com.amazonaws.services.athena.model.*;
import org.slf4j.Logger;
import uk.nhs.nhsx.diagnosiskeydist.utils.ConfigurationUtility;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AthenaService {
    private final Regions region;
    private final Logger logger;
    private final String workgroup = ConfigurationUtility.WORKGROUP;
    private final Map<String, String> namedQueryExecutions = new HashMap<>();

    AthenaService(Regions region, Logger logger){
        this.region = region;
        this.logger = logger;
    }

    public List<String> executeAllQueries() {
        AmazonAthena athenaClient = AmazonAthenaClient.builder()
                .withRegion(this.region)
                .build();

        logger.info("Retrieving named queries.");

        List<String> namedQueryIds = athenaClient.listNamedQueries(
                new ListNamedQueriesRequest().withWorkGroup(workgroup)
        ).getNamedQueryIds();

        logger.info("Total named queries for workgroup "
                .concat(workgroup)
                .concat(": ")
                .concat(Integer.toString(namedQueryIds.size())));
        logger.info("Named query ids: ".concat(namedQueryIds.toString()));

        List<NamedQuery> namedQueries = namedQueryIds.stream().map(
                namedQueryId -> athenaClient.getNamedQuery(new GetNamedQueryRequest().withNamedQueryId(namedQueryId)).getNamedQuery()
        ).collect(Collectors.toList());

        logger.info("Named queries retrieved.");

        createTableRequests(athenaClient, namedQueries);

        createDataQueryRequests(athenaClient, namedQueries);

        return namedQueries.stream().map(namedQuery -> namedQueryExecutions.get(namedQuery.getNamedQueryId()))
                .collect(Collectors.toList());
    }

    private void createDataQueryRequests(AmazonAthena athenaClient, List<NamedQuery> namedQueries) {
        Stream<NamedQuery> queryStream = namedQueries.stream();
        queryStream.forEach(
                namedQuery -> {
                    String executionId = submitDataQueryRequest(athenaClient, namedQuery);
                    if(!executionId.equals("NOOP")) namedQueryExecutions.put(namedQuery.getNamedQueryId(), executionId);
                }
        );
    }

    private void createTableRequests(AmazonAthena athenaClient, List<NamedQuery> namedQueries) {
        Stream<NamedQuery> queryStream = namedQueries.stream();
        queryStream.forEach(
                namedQuery -> {
                    String executionId = submitCreateTableRequest(athenaClient, namedQuery);
                    if(!executionId.equals("NOOP")) namedQueryExecutions.put(namedQuery.getNamedQueryId(), executionId);
                }
        );
    }

    private String submitCreateTableRequest(AmazonAthena client, NamedQuery query) {
        String queryName = query.getName();
        if(queryName.equals("create_table")) {
            logger.info("Beginning Create Table Execution request for: ".concat(queryName));
            return submitAthenaQuery(client, query);
        }

        return "NOOP";
    }

    private String submitDataQueryRequest(AmazonAthena client, NamedQuery query) {
        String queryName = query.getName();
        if(!queryName.equals("create_table")) {
            logger.info("Beginning Data Query Execution request for: ".concat(queryName));
            return submitAthenaQuery(client, query);
        }

        return "NOOP";
    }

    private String submitAthenaQuery(AmazonAthena client, NamedQuery query) {
        QueryExecutionContext context = new QueryExecutionContext()
                .withDatabase(query.getDatabase());

        String outputLocation = "s3://".concat(ConfigurationUtility.OUTPUT_STORE).concat("/public/query-results/").concat(query.getName().split("_db_")[1]);

        ResultConfiguration resultConfiguration = new ResultConfiguration()
                .withOutputLocation(outputLocation);

        StartQueryExecutionRequest queryExecutionRequest = new StartQueryExecutionRequest()
                .withWorkGroup(workgroup)
                .withQueryString(query.getQueryString())
                .withQueryExecutionContext(context)
                .withResultConfiguration(resultConfiguration);

        StartQueryExecutionResult result = client.startQueryExecution(queryExecutionRequest);
        logger.info("Query execution started: ".concat(query.getName()));
        return result.getQueryExecutionId();
    }
}
