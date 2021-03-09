package uk.nhs.nhsx.virology

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.Environment.EnvironmentKey.Companion.integer
import uk.nhs.nhsx.core.Environment.EnvironmentKey.Companion.string
import uk.nhs.nhsx.core.Handler
import uk.nhs.nhsx.core.SystemClock.CLOCK
import uk.nhs.nhsx.core.aws.s3.AwsS3Client
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.sns.AwsSns
import uk.nhs.nhsx.core.aws.sns.AwsSnsClient
import uk.nhs.nhsx.core.aws.sns.NumericAttribute
import uk.nhs.nhsx.core.events.Event
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.PrintingJsonEvents
import uk.nhs.nhsx.core.random.crockford.CrockfordDammRandomStringGenerator
import uk.nhs.nhsx.core.scheduled.SchedulingHandler
import uk.nhs.nhsx.virology.VirologyConfig.Companion.fromEnvironment
import uk.nhs.nhsx.virology.order.TokensGenerator
import uk.nhs.nhsx.virology.persistence.VirologyPersistenceService
import uk.nhs.nhsx.virology.result.TestEndDate
import uk.nhs.nhsx.virology.result.TestResult
import uk.nhs.nhsx.virology.tokengen.CtaTokenZipFileEntryRequest
import uk.nhs.nhsx.virology.tokengen.VirologyProcessorExports
import uk.nhs.nhsx.virology.tokengen.VirologyProcessorService
import uk.nhs.nhsx.virology.tokengen.VirologyProcessorStore
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit


class ScheduledCtaTokenGenerationHandler @JvmOverloads constructor(
    events: Events = PrintingJsonEvents(CLOCK),
    private val environment: Environment = Environment.fromSystem(),
    private val snsClient: AwsSns = AwsSnsClient(events),
    private val passwordGenerator: CrockfordDammRandomStringGenerator = CrockfordDammRandomStringGenerator(),
    private val virologyProcessorService: VirologyProcessorService = virologyProcessorService(
        environment,
        PrintingJsonEvents(CLOCK)
    )
) : SchedulingHandler(events) {


    override fun handler() = Handler<ScheduledEvent, Event> { _, _ ->
        val today = LocalDate.now()
        val numberOfDays = environment.access.required(NUMBER_OF_DAYS)
        val numberOfTokens = environment.access.required(NUMBER_OF_TOKENS)
        val numberOfBatches = environment.access.required(NUMBER_OF_BATCHES)
        val urlNotificationSnsTopicArn = environment.access.required(URL_NOTIFICATION_SNS_TOPIC_ARN)
        val passwordNotificationSnsTopicArn = environment.access.required(PASSWORD_NOTIFICATION_SNS_TOPIC_ARN)

        val createdTokenFiles = (1 until numberOfBatches + 1).map { batchId ->
            val ctaTokenZipFileEntryRequests = (0 until numberOfDays).map {
                val endDate = today.plus(it.toLong() + 1, ChronoUnit.DAYS)
                CtaTokenZipFileEntryRequest(
                    TestResult.Positive,
                    TestEndDate.of(endDate),
                    TestKit.LAB_RESULT,
                    CSV_FILENAME_FORMATER.format(endDate),
                    numberOfTokens
                )
            }
            val zipFilePassword = passwordGenerator.generate()
            val zipFile = virologyProcessorService.generateStoreTokensAndReturnSignedUrl(
                ctaTokenZipFileEntryRequests,
                zipFilePassword,
                ZonedDateTime.now().plusDays(DOWNLOAD_LINK_EXPIRATION_DAYS.toLong())
            )

            batchId to zipFile
        }
        createdTokenFiles.forEach { ctaTokenFile ->
            snsClient.publishMessage(
                urlNotificationSnsTopicArn,
                String.format(URL_MESSAGE_TEMPLATE, ctaTokenFile.second.url),
                mapOf("batchNumber" to NumericAttribute(ctaTokenFile.first)),
                URL_MESSAGE_SUBJECT
            )
        }

        createdTokenFiles.forEach { ctaTokenFile ->
            snsClient.publishMessage(
                passwordNotificationSnsTopicArn,
                String.format(PASSWORD_MESSAGE_TEMPLATE, ctaTokenFile.second.fileName, ctaTokenFile.second.password),
                mapOf("batchNumber" to NumericAttribute(ctaTokenFile.first))
            )
        }


        CtaTokensAndUrlGenerationCompleted(
            "Scheduled job to generate cta tokens finished.",
            createdTokenFiles.size,
            createdTokenFiles.size
        )
    }

    companion object {
        private val NUMBER_OF_DAYS = integer("number_of_days")
        private val NUMBER_OF_TOKENS = integer("number_of_tokens")
        private val NUMBER_OF_BATCHES = integer("number_of_batches")
        private val URL_NOTIFICATION_SNS_TOPIC_ARN = string("url_notification_sns_topic_arn")
        private val PASSWORD_NOTIFICATION_SNS_TOPIC_ARN = string("password_notification_sns_topic_arn")

        private const val URL_MESSAGE_TEMPLATE =
            "Please download the ZIP with positive cta tokens here: %s (the link is valid for 2 days). The password will be provided via a different channel (SMS)"
        private const val URL_MESSAGE_SUBJECT = "CTA Tokens"

        private const val PASSWORD_MESSAGE_TEMPLATE = "The password of the %s ZIP is: %s"
        private const val DOWNLOAD_LINK_EXPIRATION_DAYS = 2

        val CSV_FILENAME_FORMATER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
            .withZone(ZoneId.systemDefault())

        private const val MAX_RETRY_COUNT = 3

        private fun virologyProcessorService(environment: Environment, events: Events): VirologyProcessorService {
            return VirologyProcessorService(
                VirologyService(
                    VirologyPersistenceService(
                        AmazonDynamoDBClientBuilder.defaultClient(),
                        fromEnvironment(environment),
                        events
                    ),
                    TokensGenerator,
                    CLOCK,
                    VirologyPolicyConfig(),
                    events
                ),
                VirologyProcessorStore(
                    AwsS3Client(events),
                    BucketName.of(System.getenv("virology_tokens_bucket_name"))
                ),
                CLOCK,
                VirologyProcessorExports(),
                MAX_RETRY_COUNT,
                events
            )
        }
    }
}
