package uk.nhs.nhsx.keyfederation.download

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.kms.AWSKMSClientBuilder
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder
import uk.nhs.nhsx.core.Clock
import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.Handler
import uk.nhs.nhsx.core.StandardSigningFactory
import uk.nhs.nhsx.core.SystemClock.CLOCK
import uk.nhs.nhsx.core.aws.s3.AwsS3Client
import uk.nhs.nhsx.core.aws.s3.S3Storage
import uk.nhs.nhsx.core.aws.secretsmanager.AwsSecretManager
import uk.nhs.nhsx.core.aws.secretsmanager.SecretManager
import uk.nhs.nhsx.core.aws.ssm.AwsSsmParameters
import uk.nhs.nhsx.core.events.Event
import uk.nhs.nhsx.core.events.EventCategory
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.InfoEvent
import uk.nhs.nhsx.core.handler.SchedulingHandler
import uk.nhs.nhsx.core.events.PrintingJsonEvents
import uk.nhs.nhsx.keyfederation.BatchTagDynamoDBService
import uk.nhs.nhsx.keyfederation.BatchTagService
import uk.nhs.nhsx.keyfederation.FederatedKeyUploader
import uk.nhs.nhsx.keyfederation.InteropClient
import uk.nhs.nhsx.keyfederation.upload.JWS

/**
 * Key Federation download lambda
 *
 *
 * doc/architecture/api-contracts/key-federation/diagnosis-key-federation-connector.md
 */
class KeyFederationDownloadHandler @JvmOverloads constructor(
    private val clock: Clock = CLOCK,
    events: Events = PrintingJsonEvents(clock),
    private val config: KeyFederationDownloadConfig = KeyFederationDownloadConfig.fromEnvironment(Environment.fromSystem()),
    private val batchTagService: BatchTagService = BatchTagDynamoDBService(
        config.stateTableName,
        AmazonDynamoDBClientBuilder.defaultClient()
    ),
    private val secretManager: SecretManager = AwsSecretManager(AWSSecretsManagerClientBuilder.defaultClient()),
    private val interopClient: InteropClient = buildInteropClient(config, secretManager, events, CLOCK),
    private val awsS3Client: S3Storage = AwsS3Client(events)
) : SchedulingHandler(events) {

    private fun downloadFromFederatedServerAndStoreKeys(context: Context) =
        if (config.downloadFeatureFlag.isEnabled()) {
            try {
                DiagnosisKeysDownloadService(
                    clock,
                    interopClient,
                    FederatedKeyUploader(
                        awsS3Client,
                        config.submissionBucketName,
                        config.federatedKeyDownloadPrefix,
                        clock,
                        config.validOrigins,
                        events
                    ),
                    batchTagService,
                    config.downloadRiskLevelDefaultEnabled,
                    config.downloadRiskLevelDefault,
                    config.initialDownloadHistoryDays,
                    config.maxSubsequentBatchDownloadCount,
                    context,
                    events
                ).downloadFromFederatedServerAndStoreKeys()
            } catch (e: Exception) {
                throw RuntimeException("Download keys failed with error", e)
            }
        } else {
            events(InfoEvent("Download to interop has been disabled, skipping this step"))
            0
        }

    override fun handler() = Handler<ScheduledEvent, Event> { _, context ->
        InteropConnectorDownloadStats(downloadFromFederatedServerAndStoreKeys(context))
    }
}

data class InteropConnectorDownloadStats(val processedSubmissions: Int) : Event(EventCategory.Info)

private fun buildInteropClient(
    config: KeyFederationDownloadConfig,
    secretManager: SecretManager,
    events: Events,
    clock: Clock
): InteropClient {

    val authTokenSecretValue = secretManager.getSecret(config.interopAuthTokenSecretName)
        .orElseThrow { RuntimeException("Unable to retrieve authorization token from secrets storage") }

    val signer = StandardSigningFactory(
        clock,
        AwsSsmParameters(),
        AWSKMSClientBuilder.defaultClient()
    ).signContentWithKeyFromParameter(config.signingKeyParameterName)

    return InteropClient(
        config.interopBaseUrl,
        authTokenSecretValue.value,
        JWS(signer),
        events
    )
}
