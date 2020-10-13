package uk.nhs.nhsx.virology;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.nhs.nhsx.core.SystemClock;
import uk.nhs.nhsx.core.SystemObjectMapper;
import uk.nhs.nhsx.core.aws.s3.AwsS3Client;
import uk.nhs.nhsx.core.aws.s3.BucketName;
import uk.nhs.nhsx.virology.order.TokensGenerator;
import uk.nhs.nhsx.virology.persistence.VirologyDynamoService;
import uk.nhs.nhsx.virology.tokengen.CtaProcessorEvent;
import uk.nhs.nhsx.virology.tokengen.VirologyProcessorService;
import uk.nhs.nhsx.virology.tokengen.VirologyProcessorStore;

import java.util.Map;

public class VirologyProcessorHandler implements RequestHandler<Map<String, String>, String> {

    private static final Logger logger = LogManager.getLogger(VirologyProcessorHandler.class);
    private final VirologyProcessorService virologyProcessorService;

    public VirologyProcessorHandler() {
        this(virologyProcessorService());
    }

    public VirologyProcessorHandler(VirologyProcessorService virologyProcessorService) {
        this.virologyProcessorService = virologyProcessorService;
    }

    @Override
    public String handleRequest(Map<String, String> event, Context context) {
        logger.info("EVENT: {}", event);
        var ctaProcessorEvent = SystemObjectMapper.MAPPER.convertValue(event, CtaProcessorEvent.class);
        virologyProcessorService.generateAndStoreTokens(ctaProcessorEvent);
        return "success";
    }

    private static VirologyProcessorService virologyProcessorService() {
        return new VirologyProcessorService(
            new VirologyService(
                new VirologyDynamoService(
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
            SystemClock.CLOCK
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
