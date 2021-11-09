@file:Suppress("SameParameterValue")

package uk.nhs.nhsx.virology.tokengen

import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import com.amazonaws.services.sns.model.PublishResult
import io.mockk.MockKMatcherScope
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.jupiter.api.Test
import strikt.api.expectThrows
import uk.nhs.nhsx.core.TestEnvironments.TEST
import uk.nhs.nhsx.core.aws.sns.AwsSns
import uk.nhs.nhsx.core.aws.sns.MessageAttribute
import uk.nhs.nhsx.core.aws.sns.NumericAttribute
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.core.random.crockford.CrockfordDammRandomStringGenerator
import uk.nhs.nhsx.domain.TestEndDate
import uk.nhs.nhsx.domain.TestKit
import uk.nhs.nhsx.domain.TestResult
import uk.nhs.nhsx.testhelper.ContextBuilder.TestContext
import uk.nhs.nhsx.virology.ScheduledCtaTokenGenerationHandler
import java.net.URL
import java.time.LocalDate
import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit.DAYS

class ScheduledCtaTokenGenerationHandlerTest {

    private val service = mockk<VirologyProcessorService>() {
        every { generateStoreTokensAndReturnSignedUrl(any(), any(), any()) } returns CtaTokenZipFile(
            url = URL("https://example.com"),
            fileName = "file-name",
            password = "password"
        )
    }

    private val snsClient = mockk<AwsSns> {
        every { publishMessage("URL_TOPIC", any(), any(), any()) } returns PublishResult()
        every { publishMessage("PASSWORD_TOPIC", any(), any()) } returns PublishResult()
    }

    private val passwordGenerator = mockk<CrockfordDammRandomStringGenerator> {
        every { generate() } returns "password"
    }

    private val events = RecordingEvents()

    private val csvFilenameFormatter = DateTimeFormatter
        .ofPattern("yyyyMMdd")
        .withZone(UTC)

    @Test
    fun `cta token files created`() {
        val environment = TEST.apply(
            mapOf(
                "number_of_days" to "7",
                "number_of_tokens" to "20",
                "number_of_batches" to "2",
                "url_notification_sns_topic_arn" to "URL_TOPIC",
                "password_notification_sns_topic_arn" to "PASSWORD_TOPIC"
            )
        )

        ScheduledCtaTokenGenerationHandler(
            events = events,
            environment = environment,
            snsClient = snsClient,
            passwordGenerator = passwordGenerator,
            virologyProcessorService = service
        ).handleRequest(ScheduledEvent(), TestContext())

        verifySequence {
            // Batch #1
            service.generateStoreTokensAndReturnSignedUrl(
                requests = createListOfCtaZipFileRequests(days = 7),
                zipFilePassword = any(),
                linkExpirationDate = any()
            )

            // Batch #2
            service.generateStoreTokensAndReturnSignedUrl(
                requests = createListOfCtaZipFileRequests(days = 7),
                zipFilePassword = any(),
                linkExpirationDate = any()
            )
        }

        verify(exactly = 1) {
            snsClient.publishMessage(
                topicArn = "URL_TOPIC",
                message = "Please download the ZIP with positive cta tokens here: https://example.com (the link is valid for 12 hours). The password will be provided via a different channel (SMS)",
                attributes = matchesBatchNumberWithValue(1),
                subject = "CTA Tokens"
            )
        }

        verify(exactly = 1) {
            snsClient.publishMessage(
                topicArn = "PASSWORD_TOPIC",
                message = "The password of the file-name ZIP is: password",
                attributes = matchesBatchNumberWithValue(1),
            )
        }

        verify(exactly = 1) {
            snsClient.publishMessage(
                "URL_TOPIC",
                message = "Please download the ZIP with positive cta tokens here: https://example.com (the link is valid for 12 hours). The password will be provided via a different channel (SMS)",
                matchesBatchNumberWithValue(2),
                "CTA Tokens"
            )
        }

        verify(exactly = 1) {
            snsClient.publishMessage(
                "PASSWORD_TOPIC",
                message = "The password of the file-name ZIP is: password",
                matchesBatchNumberWithValue(2),
            )
        }
    }

    @Test
    fun `wrong number-of-days configuration`() {
        val environment = TEST.apply(
            mapOf(
                "number_of_days" to "asd",
                "number_of_tokens" to "20",
                "number_of_batches" to "2",
            )
        )

        val handler = ScheduledCtaTokenGenerationHandler(
            events = events,
            environment = environment,
            snsClient = snsClient,
            passwordGenerator = passwordGenerator,
            virologyProcessorService = service
        )

        expectThrows<NumberFormatException> { handler.handleRequest(ScheduledEvent(), TestContext()) }
    }

    @Test
    fun `wrong number-of-tokens configuration`() {
        val environment = TEST.apply(
            mapOf(
                "number_of_days" to "3",
                "number_of_tokens" to "asd",
                "number_of_batches" to "2",
            )
        )
        val handler = ScheduledCtaTokenGenerationHandler(
            events = events,
            environment = environment,
            snsClient = snsClient,
            passwordGenerator = passwordGenerator,
            virologyProcessorService = service
        )

        expectThrows<NumberFormatException> { handler.handleRequest(ScheduledEvent(), TestContext()) }
    }

    @Test
    fun `wrong number_of_batches configuration`() {
        val environment = TEST.apply(
            mapOf(
                "number_of_days" to "3",
                "number_of_tokens" to "7",
                "number_of_batches" to "asd",
            )
        )
        val handler = ScheduledCtaTokenGenerationHandler(
            events = events,
            environment = environment,
            snsClient = snsClient,
            passwordGenerator = passwordGenerator,
            virologyProcessorService = service
        )

        expectThrows<NumberFormatException> { handler.handleRequest(ScheduledEvent(), TestContext()) }
    }

    private fun createListOfCtaZipFileRequests(days: Long) =
        (1 until days + 1).map { createListOfCtaZipFileRequest(it) }

    private fun createListOfCtaZipFileRequest(daysOffset: Long) = LocalDate.now().plus(daysOffset, DAYS).let {
        CtaTokenZipFileEntryRequest(
            TestResult.Positive,
            TestEndDate.of(it),
            TestKit.LAB_RESULT,
            csvFilenameFormatter.format(it),
            20
        )
    }

    private fun MockKMatcherScope.matchesBatchNumberWithValue(value: Int): Map<String, MessageAttribute> =
        match { it.containsKey("batchNumber") && it["batchNumber"]?.value == value.toString() && it["batchNumber"] is NumericAttribute }
}

