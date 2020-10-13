package uk.nhs.nhsx.keyfederation;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.nhs.nhsx.core.Environment;
import uk.nhs.nhsx.core.SystemClock;
import uk.nhs.nhsx.core.aws.s3.AwsS3;
import uk.nhs.nhsx.core.aws.s3.AwsS3Client;
import uk.nhs.nhsx.core.aws.secretsmanager.AwsSecretManager;
import uk.nhs.nhsx.core.aws.secretsmanager.SecretManager;
import uk.nhs.nhsx.core.aws.secretsmanager.SecretValue;
import uk.nhs.nhsx.diagnosiskeydist.SubmissionRepository;
import uk.nhs.nhsx.diagnosiskeydist.s3.SubmissionFromS3Repository;
import uk.nhs.nhsx.keyfederation.download.DiagnosisKeysDownloadService;
import uk.nhs.nhsx.keyfederation.upload.DiagnosisKeysUploadService;
import uk.nhs.nhsx.keyfederation.upload.JWS;

import java.time.Instant;
import java.util.function.Supplier;

import static uk.nhs.nhsx.core.ObjectKeyFilter.excludeKeyWithPrefix;


public class Handler implements RequestHandler<ScheduledEvent, String> {

    private static final Logger logger = LogManager.getLogger(Handler.class);

    private final Supplier<Instant> clock;
    private final KeyFederationConfig config;
    private final InteropClient interopClient;
    private final BatchTagService batchTagService;
    private final SubmissionRepository submissionRepository;
    private final AwsS3 awsS3Client;

    public Handler() {
        this(
            SystemClock.CLOCK,
            KeyFederationConfig.fromEnvironment(Environment.fromSystem()),
            new AwsSecretManager(),
            new AwsS3Client()
        );
    }

    public Handler(Supplier<Instant> clock, KeyFederationConfig config, SecretManager secretManager, AwsS3 awsS3Client) {
        this(
            clock,
            config,
            new BatchTagDynamoDBService(config.stateTableName),
            () -> buildInteropClient(config, secretManager),
            awsS3Client
        );
    }

    public Handler(Supplier<Instant> clock, KeyFederationConfig config, BatchTagService batchTagService, Supplier<InteropClient> interopClientSupplier, AwsS3 awsS3Client) {
        this.clock = clock;
        this.config = config;
        this.interopClient = interopClientSupplier.get();
        this.batchTagService = batchTagService;
        this.submissionRepository = new SubmissionFromS3Repository(awsS3Client, excludeKeyWithPrefix(config.federatedKeyPrefix));
        this.awsS3Client = awsS3Client;
    }

    private static InteropClient buildInteropClient(KeyFederationConfig config, SecretManager secretManager) {
        SecretValue privateKeySecretValue = secretManager.getSecret(config.interopPrivateKeySecretName)
            .orElseThrow(() -> {
                logger.error("Unable to retrieve private key from secrets storage");
                return new RuntimeException("Missing private key");
            });

        SecretValue authTokenSecretValue = secretManager.getSecret(config.interopAuthTokenSecretName)
            .orElseThrow(() -> {
                logger.error("Unable to retrieve authorization token from secrets storage");
                return new RuntimeException("Missing authorization token");
            });

        return new InteropClient(config.interopBaseUrl, authTokenSecretValue.value, new JWS(privateKeySecretValue.value));
    }

    @Override
    public String handleRequest(ScheduledEvent input, Context context) {

        if (config.downloadEnabled) {
            try {
                new DiagnosisKeysDownloadService(
                    clock,
                    interopClient,
                    new FederatedKeyUploader(awsS3Client, config.submissionBucketName, config.federatedKeyPrefix, clock, config.validRegions),
                    batchTagService
                ).downloadAndSave();
            } catch (Exception e) {
                logger.error("Download keys failed with error", e);
            }
        } else {
            logger.info("Download to interop has been disabled, skipping this step");
        }

        if (config.uploadEnabled) {
            try {
                new DiagnosisKeysUploadService(
                    interopClient,
                    submissionRepository,
                    batchTagService,
                    config.region
                ).uploadRequest();
            } catch (Exception e) {
                logger.error("Upload keys failed with error", e);
            }
        } else {
            logger.info("Upload to interop has been disabled, skipping this step");
        }

        return "success";
    }

}


