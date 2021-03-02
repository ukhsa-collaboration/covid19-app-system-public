package uk.nhs.nhsx.diagnosiskeydist

import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.Environment.EnvironmentKey
import uk.nhs.nhsx.core.EnvironmentKeys
import uk.nhs.nhsx.core.ObjectKeyFilters
import uk.nhs.nhsx.core.StandardSigning.datedSigner
import uk.nhs.nhsx.core.StandardSigning.signContentWithKeyFromParameter
import uk.nhs.nhsx.core.SystemClock
import uk.nhs.nhsx.core.aws.cloudfront.AwsCloudFront
import uk.nhs.nhsx.core.aws.cloudfront.AwsCloudFrontClient
import uk.nhs.nhsx.core.aws.s3.AwsS3
import uk.nhs.nhsx.core.aws.s3.AwsS3Client
import uk.nhs.nhsx.core.aws.ssm.AwsSsmParameters
import uk.nhs.nhsx.core.aws.ssm.Parameters
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.PrintingJsonEvents
import uk.nhs.nhsx.core.scheduled.Scheduling
import uk.nhs.nhsx.core.scheduled.SchedulingHandler
import uk.nhs.nhsx.diagnosiskeydist.keydistribution.UploadToS3KeyDistributor
import uk.nhs.nhsx.diagnosiskeydist.s3.SubmissionFromS3Repository
import java.time.Instant
import java.util.function.Supplier

/**
 * Scheduling strategy:
 * - cron(47 1,3,5,7,9,11,13,15,17,19,21,23 * * ? *)
 *
 *
 * Dependencies
 * - Submissions bucket: submissions must be deleted >14 days after upload (S3 Lifecycle Policy)
 *
 *
 * Error handling strategy:
 * - Fail fast (e.g. error in one thread -> stop all threads immediately)
 * - Log errors
 * - Error alerting (ERROR entries in log group -> error metric -> error alert)
 */

@Suppress("unused")
class DiagnosisKeyDistributionHandler @JvmOverloads constructor(
    private val environment: Environment = Environment.fromSystem(),
    private val clock: Supplier<Instant> = SystemClock.CLOCK,
    private val parameters: Parameters = AwsSsmParameters(),
    private val awsCloudFrontClient: AwsCloudFront = AwsCloudFrontClient(PrintingJsonEvents(SystemClock.CLOCK)),
    private val awsS3Client: AwsS3 = AwsS3Client(PrintingJsonEvents(SystemClock.CLOCK)),
    events: Events = PrintingJsonEvents(SystemClock.CLOCK)
) : SchedulingHandler(events) {

    private val batchProcessingConfig = BatchProcessingConfig.fromEnvironment(environment)
    private val diagnosisKeySubmissionPrefixes = EnvironmentKey.strings("DIAGNOSIS_KEY_SUBMISSION_PREFIXES")
    private val mobileAppBundleId = EnvironmentKey.string("MOBILE_APP_BUNDLE_ID")

    override fun handler() = Scheduling.Handler { _, _ ->
        try {
            val allowedPrefixes = environment.access.required(diagnosisKeySubmissionPrefixes)
            val objectKeyFilter = ObjectKeyFilters.batched().withPrefixes(allowedPrefixes)
            val submissionBucket = environment.access.required(EnvironmentKeys.SUBMISSION_BUCKET_NAME)
            val submissionRepository =
                SubmissionFromS3Repository(awsS3Client, objectKeyFilter, submissionBucket, events)

            DistributionService(
                submissionRepository,
                ExposureProtobuf(environment.access.required(mobileAppBundleId)),
                UploadToS3KeyDistributor(
                    awsS3Client,
                    datedSigner(
                        clock,
                        parameters,
                        batchProcessingConfig.ssmMetaDataSigningKeyParameterName
                    )
                ),
                signContentWithKeyFromParameter(
                    parameters,
                    batchProcessingConfig.ssmAGSigningKeyParameterName
                ),
                awsCloudFrontClient,
                awsS3Client,
                batchProcessingConfig,
                events
            ).distributeKeys(clock.get())

            KeysDistributed
        } catch (e: Exception) {
            throw RuntimeException("Failed: Key distribution batch", e)
        }
    }
}
