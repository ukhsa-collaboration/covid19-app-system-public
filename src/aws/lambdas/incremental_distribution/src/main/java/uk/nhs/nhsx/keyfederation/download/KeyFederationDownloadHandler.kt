package uk.nhs.nhsx.keyfederation.download

import com.amazonaws.services.lambda.runtime.Context
import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.StandardSigning
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
import uk.nhs.nhsx.core.events.PrintingJsonEvents
import uk.nhs.nhsx.core.scheduled.Scheduling
import uk.nhs.nhsx.core.scheduled.SchedulingHandler
import uk.nhs.nhsx.keyfederation.BatchTagDynamoDBService
import uk.nhs.nhsx.keyfederation.BatchTagService
import uk.nhs.nhsx.keyfederation.FederatedKeyUploader
import uk.nhs.nhsx.keyfederation.InteropClient
import uk.nhs.nhsx.keyfederation.upload.JWS
import java.time.Instant
import java.util.function.Supplier

/**
 * Key Federation download lambda
 *
 *
 * doc/architecture/api-contracts/diagnosis-key-federation.md
 */
class KeyFederationDownloadHandler  @JvmOverloads constructor (
    private val clock: Supplier<Instant> = CLOCK,
    events: Events = PrintingJsonEvents(clock),
    private val config: KeyFederationDownloadConfig = KeyFederationDownloadConfig.fromEnvironment(Environment.fromSystem()),
    private val batchTagService: BatchTagService = BatchTagDynamoDBService(config.stateTableName),
    private val secretManager: SecretManager = AwsSecretManager(),
    private val interopClient: InteropClient = buildInteropClient(config, secretManager, events),
    private val awsS3Client: S3Storage = AwsS3Client(events)
) : SchedulingHandler(events) {

    private fun downloadFromFederatedServerAndStoreKeys(context: Context) =
        if (config.downloadFeatureFlag.isEnabled) {
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
            events(javaClass, InfoEvent("Download to interop has been disabled, skipping this step"))
            0
        }

    override fun handler() = Scheduling.Handler { _, context ->
        InteropConnectorDownloadStats(downloadFromFederatedServerAndStoreKeys(context))
    }
}

data class InteropConnectorDownloadStats(val processedSubmissions: Int) : Event(EventCategory.Info)

private fun buildInteropClient(
    config: KeyFederationDownloadConfig,
    secretManager: SecretManager,
    events: Events
): InteropClient {

    val authTokenSecretValue = secretManager.getSecret(config.interopAuthTokenSecretName)
        .orElseThrow { RuntimeException("Unable to retrieve authorization token from secrets storage") }

    val signer = StandardSigning.signContentWithKeyFromParameter(AwsSsmParameters(), config.signingKeyParameterName)

    return InteropClient(
        config.interopBaseUrl,
        authTokenSecretValue.value,
        JWS(signer),
        events
    )
}
