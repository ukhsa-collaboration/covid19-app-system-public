package uk.nhs.nhsx.keyfederation.upload;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.nhs.nhsx.core.Environment;
import uk.nhs.nhsx.core.StandardSigning;
import uk.nhs.nhsx.core.aws.s3.AwsS3;
import uk.nhs.nhsx.core.aws.s3.AwsS3Client;
import uk.nhs.nhsx.core.aws.secretsmanager.AwsSecretManager;
import uk.nhs.nhsx.core.aws.secretsmanager.SecretManager;
import uk.nhs.nhsx.core.aws.ssm.AwsSsmParameters;
import uk.nhs.nhsx.core.signature.Signer;
import uk.nhs.nhsx.diagnosiskeydist.s3.SubmissionFromS3Repository;
import uk.nhs.nhsx.keyfederation.BatchTagDynamoDBService;
import uk.nhs.nhsx.keyfederation.BatchTagService;
import uk.nhs.nhsx.keyfederation.InteropClient;

import java.util.function.Supplier;

import static uk.nhs.nhsx.core.ObjectKeyFilter.includeMobileAndAllowedPrefixes;

/**
 * Key Federation upload lambda
 * <p>
 * doc/architecture/api-contracts/diagnosis-key-federation.md
 */
public class KeyFederationUploadHandler implements RequestHandler<ScheduledEvent, String> {

    private static final Logger logger = LogManager.getLogger(KeyFederationUploadHandler.class);

    private final KeyFederationUploadConfig config;
    private final Supplier<InteropClient> interopClient;
    private final BatchTagService batchTagService;
    private final AwsS3 awsS3Client;

    public KeyFederationUploadHandler() {
        this(
            KeyFederationUploadConfig.fromEnvironment(Environment.fromSystem()),
            new AwsSecretManager(),
            new AwsS3Client()
        );
    }

    public KeyFederationUploadHandler(KeyFederationUploadConfig config,
                   SecretManager secretManager,
                   AwsS3 awsS3Client) {
        this(config,
            new BatchTagDynamoDBService(config.stateTableName),
            () -> buildInteropClient(config, secretManager),
            awsS3Client
        );
    }

    public KeyFederationUploadHandler(KeyFederationUploadConfig config,
                   BatchTagService batchTagService,
                   Supplier<InteropClient> interopClient,
                   AwsS3 awsS3Client) {
        this.config = config;
        this.interopClient = interopClient;
        this.batchTagService = batchTagService;
        this.awsS3Client = awsS3Client;
    }

    private static InteropClient buildInteropClient(KeyFederationUploadConfig config, SecretManager secretManager) {

        var authTokenSecretValue = secretManager.getSecret(config.interopAuthTokenSecretName)
            .orElseThrow(() -> {
                logger.error("Unable to retrieve authorization token from secrets storage");
                return new RuntimeException("Missing authorization token");
            });

        Signer signer = StandardSigning.signContentWithKeyFromParameter(new AwsSsmParameters(), config.signingKeyParameterName);

        return new InteropClient(config.interopBaseUrl, authTokenSecretValue.value, new JWS(signer));
    }

    @Override
    public String handleRequest(ScheduledEvent input, Context context) {
        long start = System.currentTimeMillis();
        int processedSubmissions = loadKeysAndUploadToFederatedServer(context);
        long uploadDuration = System.currentTimeMillis() - start;

        logger.info("InteropConnectorStats: Processed and uploaded keys from {} submission JSONs in {} seconds", processedSubmissions, (uploadDuration / 1000.));

        return "success";
    }

    private int loadKeysAndUploadToFederatedServer(Context context) {
        if (config.uploadFeatureFlag.isEnabled()) {
            try {
                var objectKeyFilter = includeMobileAndAllowedPrefixes(config.federatedKeyUploadPrefixes);
                var submissionRepository = new SubmissionFromS3Repository(awsS3Client, objectKeyFilter);

                return new DiagnosisKeysUploadService(
                    interopClient.get(),
                    submissionRepository,
                    batchTagService,
                    config.region,
                    config.uploadRiskLevelDefaultEnabled,
                    config.uploadRiskLevelDefault,
                    config.initialUploadHistoryDays,
                    config.maxUploadBatchSize,
                    config.maxSubsequentBatchUploadCount,
                    context
                ).loadKeysAndUploadToFederatedServer();
            } catch (Exception e) {
                logger.error("Upload keys failed with error", e);
                throw new RuntimeException(e);
            }
        } else {
            logger.info("Upload to interop has been disabled, skipping this step");
        }

        return 0;
    }
}


