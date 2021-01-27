package uk.nhs.nhsx.virology;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.nhs.nhsx.core.Jackson;
import uk.nhs.nhsx.core.SystemClock;
import uk.nhs.nhsx.core.SystemObjectMapper;
import uk.nhs.nhsx.core.aws.s3.AwsS3Client;
import uk.nhs.nhsx.core.aws.s3.BucketName;
import uk.nhs.nhsx.virology.order.TokensGenerator;
import uk.nhs.nhsx.virology.persistence.VirologyPersistenceService;
import uk.nhs.nhsx.virology.tokengen.CtaProcessorRequest;
import uk.nhs.nhsx.virology.tokengen.VirologyProcessorService;
import uk.nhs.nhsx.virology.tokengen.VirologyProcessorStore;

import java.util.Map;

public class VirologyProcessorHandler implements RequestHandler<Map<String, String>, String> {

    private static final Logger logger = LogManager.getLogger(VirologyProcessorHandler.class);
    private final VirologyProcessorService virologyProcessorService;
    private static final int MAX_RETRY_COUNT = 3;

    @SuppressWarnings("unused")
    public VirologyProcessorHandler() {
        this(virologyProcessorService());
    }

    public VirologyProcessorHandler(VirologyProcessorService virologyProcessorService) {
        this.virologyProcessorService = virologyProcessorService;
    }

    @Override
    public String handleRequest(Map<String, String> event, Context context) {
        logger.info("EVENT: {}", event);
        var ctaProcessorEvent = SystemObjectMapper.MAPPER.convertValue(event, CtaProcessorRequest.class);
        var result = virologyProcessorService.generateAndStoreTokens(ctaProcessorEvent);
        logger.info("RESULT: {}", event);
        return Jackson.toJson(result.toResponse());
    }

    private static VirologyProcessorService virologyProcessorService() {
        return new VirologyProcessorService(
            new VirologyService(
                new VirologyPersistenceService(
                    AmazonDynamoDBClientBuilder.defaultClient(),
                    virologyConfig()
                ),
                new TokensGenerator(),
                SystemClock.CLOCK
            ),
            new VirologyProcessorStore(
                new AwsS3Client(),
                BucketName.of(System.getenv("virology_tokens_bucket_name"))
            ),
            SystemClock.CLOCK,
            MAX_RETRY_COUNT
        );
    }

    private static VirologyConfig virologyConfig() {
        return new VirologyConfig(
            System.getenv("test_orders_table"),
            System.getenv("test_results_table"),
            System.getenv("submission_tokens_table"),
            System.getenv("test_orders_index"),
            VirologyConfig.MAX_TOKEN_PERSISTENCE_RETRY_COUNT
        );
    }
}
