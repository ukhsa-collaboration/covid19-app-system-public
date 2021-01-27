package uk.nhs.nhsx.keyfederation.download;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.nhs.nhsx.core.Environment;
import uk.nhs.nhsx.core.StandardSigning;
import uk.nhs.nhsx.core.SystemClock;
import uk.nhs.nhsx.core.aws.s3.AwsS3;
import uk.nhs.nhsx.core.aws.s3.AwsS3Client;
import uk.nhs.nhsx.core.aws.s3.S3Storage;
import uk.nhs.nhsx.core.aws.secretsmanager.AwsSecretManager;
import uk.nhs.nhsx.core.aws.secretsmanager.SecretManager;
import uk.nhs.nhsx.core.aws.ssm.AwsSsmParameters;
import uk.nhs.nhsx.core.signature.Signer;
import uk.nhs.nhsx.keyfederation.*;
import uk.nhs.nhsx.keyfederation.upload.JWS;

import java.time.Instant;
import java.util.function.Supplier;

/**
 * Key Federation download lambda
 * <p>
 * doc/architecture/api-contracts/diagnosis-key-federation.md
 */
public class KeyFederationDownloadHandler implements RequestHandler<ScheduledEvent, String> {

    private static final Logger logger = LogManager.getLogger(KeyFederationDownloadHandler.class);

    private final Supplier<Instant> clock;
    private final KeyFederationDownloadConfig config;
    private final Supplier<InteropClient> interopClient;
    private final BatchTagService batchTagService;
    private final S3Storage awsS3Client;

    @SuppressWarnings("unused")
    public KeyFederationDownloadHandler() {
        this(
            SystemClock.CLOCK,
            KeyFederationDownloadConfig.fromEnvironment(Environment.fromSystem()),
            new AwsSecretManager(),
            new AwsS3Client()
        );
    }

    public KeyFederationDownloadHandler(Supplier<Instant> clock,
                   KeyFederationDownloadConfig config,
                   SecretManager secretManager,
                   AwsS3 awsS3Client) {
        this(
            clock,
            config,
            new BatchTagDynamoDBService(config.stateTableName),
            () -> buildInteropClient(config, secretManager),
            awsS3Client
        );
    }

    public KeyFederationDownloadHandler(Supplier<Instant> clock,
                   KeyFederationDownloadConfig config,
                   BatchTagService batchTagService,
                   Supplier<InteropClient> interopClient,
                   S3Storage awsS3Client) {
        this.clock = clock;
        this.config = config;
        this.interopClient = interopClient;
        this.batchTagService = batchTagService;
        this.awsS3Client = awsS3Client;
    }

    private static InteropClient buildInteropClient(KeyFederationDownloadConfig config, SecretManager secretManager) {

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
        int processedBatches = downloadFromFederatedServerAndStoreKeys(context);
        long downloadDuration = System.currentTimeMillis() - start;

        logger.info("InteropConnectorStats: Downloaded and processed {} key batches in {} seconds", processedBatches, (downloadDuration / 1000.));

        return "success";
    }

    private int downloadFromFederatedServerAndStoreKeys(Context context) {
        if (config.downloadFeatureFlag.isEnabled()) {
            try {
                return new DiagnosisKeysDownloadService(
                    clock,
                    interopClient.get(),
                    new FederatedKeyUploader(
                        awsS3Client,
                        config.submissionBucketName,
                        config.federatedKeyDownloadPrefix,
                        clock,
                        config.validOrigins),
                    batchTagService,
                    config.downloadRiskLevelDefaultEnabled,
                    config.downloadRiskLevelDefault,
                    config.initialDownloadHistoryDays,
                    config.maxSubsequentBatchDownloadCount,
                    context
                ).downloadFromFederatedServerAndStoreKeys();
            } catch (Exception e) {
                logger.error("Download keys failed with error", e);
                throw new RuntimeException(e);
            }
        } else {
            logger.info("Download to interop has been disabled, skipping this step");
        }

        return 0;
    }
}


