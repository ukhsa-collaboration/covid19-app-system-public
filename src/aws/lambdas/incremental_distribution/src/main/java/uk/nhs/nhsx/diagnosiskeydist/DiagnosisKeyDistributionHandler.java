package uk.nhs.nhsx.diagnosiskeydist;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.nhs.nhsx.core.Environment;
import uk.nhs.nhsx.core.EnvironmentKeys;
import uk.nhs.nhsx.core.ObjectKeyFilters;
import uk.nhs.nhsx.core.SystemClock;
import uk.nhs.nhsx.core.aws.cloudfront.AwsCloudFront;
import uk.nhs.nhsx.core.aws.cloudfront.AwsCloudFrontClient;
import uk.nhs.nhsx.core.aws.s3.AwsS3;
import uk.nhs.nhsx.core.aws.s3.AwsS3Client;
import uk.nhs.nhsx.core.aws.ssm.AwsSsmParameters;
import uk.nhs.nhsx.core.aws.ssm.Parameters;
import uk.nhs.nhsx.diagnosiskeydist.keydistribution.UploadToS3KeyDistributor;
import uk.nhs.nhsx.diagnosiskeydist.s3.SubmissionFromS3Repository;

import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

import static uk.nhs.nhsx.core.Environment.EnvironmentKey;
import static uk.nhs.nhsx.core.StandardSigning.datedSigner;
import static uk.nhs.nhsx.core.StandardSigning.signContentWithKeyFromParameter;

/**
 * Scheduling strategy:
 * - cron(47 1,3,5,7,9,11,13,15,17,19,21,23 * * ? *)
 * <p>
 * Dependencies
 * - Submissions bucket: submissions must be deleted >14 days after upload (S3 Lifecycle Policy)
 * <p>
 * Error handling strategy:
 * - Fail fast (e.g. error in one thread -> stop all threads immediately)
 * - Log errors
 * - Error alerting (ERROR entries in log group -> error metric -> error alert)
 */
public class DiagnosisKeyDistributionHandler implements RequestHandler<ScheduledEvent, String> {

    private static final Logger logger = LogManager.getLogger(DiagnosisKeyDistributionHandler.class);

    private static final EnvironmentKey<List<String>> DIAGNOSIS_KEY_SUBMISSION_PREFIXES = EnvironmentKey.strings("DIAGNOSIS_KEY_SUBMISSION_PREFIXES");
    private static final EnvironmentKey<String> MOBILE_APP_BUNDLE_ID = EnvironmentKey.string("MOBILE_APP_BUNDLE_ID");

    private final Environment environment;
    private final BatchProcessingConfig batchProcessingConfig;
    private final Supplier<Instant> clock;
    private final AwsS3 awsS3Client;
    private final Parameters parameters;
    private final AwsCloudFront awsCloudFrontClient;

    @SuppressWarnings("unused")
    public DiagnosisKeyDistributionHandler() {
        this(
            Environment.fromSystem(),
            SystemClock.CLOCK,
            new AwsS3Client(),
            new AwsSsmParameters(),
            new AwsCloudFrontClient()
        );
    }

    public DiagnosisKeyDistributionHandler(Environment environment,
                                           Supplier<Instant> clock,
                                           AwsS3 awsS3Client,
                                           Parameters parameters,
                                           AwsCloudFront awsCloudFrontClient) {
        this.clock = clock;
        this.environment = environment;
        this.awsS3Client = awsS3Client;
        this.parameters = parameters;
        this.awsCloudFrontClient = awsCloudFrontClient;
        this.batchProcessingConfig = BatchProcessingConfig.fromEnvironment(this.environment);
    }

    public String handleRequest(ScheduledEvent input, Context context) {
        try {
            logger.info("Begin: Key distribution batch");

            var allowedPrefixes = environment.access.required(DIAGNOSIS_KEY_SUBMISSION_PREFIXES);
            var objectKeyFilter = ObjectKeyFilters.batched().withPrefixes(allowedPrefixes);

            var submissionBucket = environment.access.required(EnvironmentKeys.SUBMISSION_BUCKET_NAME);
            var submissionRepository = new SubmissionFromS3Repository(awsS3Client, objectKeyFilter, submissionBucket);

            new DistributionService(
                submissionRepository,
                new ExposureProtobuf(environment.access.required(MOBILE_APP_BUNDLE_ID)),
                new UploadToS3KeyDistributor(awsS3Client, datedSigner(clock, parameters, batchProcessingConfig.ssmMetaDataSigningKeyParameterName)),
                signContentWithKeyFromParameter(parameters, batchProcessingConfig.ssmAGSigningKeyParameterName),
                awsCloudFrontClient,
                awsS3Client,
                batchProcessingConfig
            ).distributeKeys(clock.get());

            logger.info("Success: Key distribution batch");

            return "success";
        } catch (Exception e) {
            logger.error("Failed: Key distribution batch", e);
            throw new RuntimeException(e);
        }
    }
}
