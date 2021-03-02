package uk.nhs.nhsx.virology;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import uk.nhs.nhsx.core.Environment;
import uk.nhs.nhsx.core.Jackson;
import uk.nhs.nhsx.core.SystemObjectMapper;
import uk.nhs.nhsx.core.aws.s3.AwsS3Client;
import uk.nhs.nhsx.core.aws.s3.BucketName;
import uk.nhs.nhsx.core.events.CtaTokensGenerated;
import uk.nhs.nhsx.core.events.Events;
import uk.nhs.nhsx.core.events.PrintingJsonEvents;
import uk.nhs.nhsx.virology.order.TokensGenerator;
import uk.nhs.nhsx.virology.persistence.VirologyPersistenceService;
import uk.nhs.nhsx.virology.tokengen.CtaProcessorRequest;
import uk.nhs.nhsx.virology.tokengen.VirologyProcessorService;
import uk.nhs.nhsx.virology.tokengen.VirologyProcessorStore;

import java.util.Map;

import static uk.nhs.nhsx.core.SystemClock.CLOCK;

public class VirologyProcessorHandler implements RequestHandler<Map<String, String>, String> {

    private static final int MAX_RETRY_COUNT = 3;

    private final VirologyProcessorService virologyProcessorService;
    private final Events events;

    @SuppressWarnings("unused")
    public VirologyProcessorHandler() {
        this(virologyProcessorService(Environment.fromSystem(), new PrintingJsonEvents(CLOCK)), new PrintingJsonEvents(CLOCK));
    }

    public VirologyProcessorHandler(VirologyProcessorService virologyProcessorService, Events events) {
        this.virologyProcessorService = virologyProcessorService;
        this.events = events;
    }

    @Override
    public String handleRequest(Map<String, String> event, Context context) {
        events.emit(getClass(), new CtaTokensGenerated(event));
        var ctaProcessorEvent = SystemObjectMapper.MAPPER.convertValue(event, CtaProcessorRequest.class);

        var result = virologyProcessorService.generateAndStoreTokens(ctaProcessorEvent);
        events.emit(getClass(), new CtaTokensGenerationComplete(result));
        return Jackson.toJson(result.toResponse());
    }

    private static VirologyProcessorService virologyProcessorService(Environment environment, Events events) {
        return new VirologyProcessorService(
            new VirologyService(
                new VirologyPersistenceService(
                    AmazonDynamoDBClientBuilder.defaultClient(),
                    VirologyConfig.fromEnvironment(environment),
                    events
                ),
                new TokensGenerator(),
                CLOCK,
                new VirologyPolicyConfig(),
                events
            ),
            new VirologyProcessorStore(
                new AwsS3Client(events),
                BucketName.of(System.getenv("virology_tokens_bucket_name"))
            ),
            CLOCK,
            MAX_RETRY_COUNT,
            events
        );
    }
}
