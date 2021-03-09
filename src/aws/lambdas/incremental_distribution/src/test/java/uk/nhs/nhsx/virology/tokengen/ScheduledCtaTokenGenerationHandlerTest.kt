package uk.nhs.nhsx.virology.tokengen

import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import com.amazonaws.services.sns.model.PublishResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.nhs.nhsx.core.TestEnvironments
import uk.nhs.nhsx.core.aws.sns.AwsSns
import uk.nhs.nhsx.core.aws.sns.NumericAttribute
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.core.random.crockford.CrockfordDammRandomStringGenerator
import uk.nhs.nhsx.testhelper.proxy
import uk.nhs.nhsx.virology.ScheduledCtaTokenGenerationHandler
import uk.nhs.nhsx.virology.TestKit
import uk.nhs.nhsx.virology.result.TestEndDate
import uk.nhs.nhsx.virology.result.TestResult
import java.lang.NumberFormatException
import java.net.URL
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class ScheduledCtaTokenGenerationHandlerTest {

    private val service = mockk<VirologyProcessorService>()
    private val snsClient = mockk<AwsSns>()
    private val passwordGenerator = mockk<CrockfordDammRandomStringGenerator>()

    private val events = RecordingEvents()
    private val CSV_FILENAME_FORMATER = DateTimeFormatter.ofPattern("yyyyMMdd")
        .withZone(ZoneId.systemDefault())


    @Test
    fun `cta token files created`() {
        val environment = TestEnvironments.TEST.apply(
            mapOf(
                "number_of_days" to "7",
                "number_of_tokens" to "20",
                "number_of_batches" to "2",
                "url_notification_sns_topic_arn" to "URL_TOPIC",
                "password_notification_sns_topic_arn" to "PASSWORD_TOPIC"
            )
        )
        val handler = ScheduledCtaTokenGenerationHandler(events,environment,snsClient,passwordGenerator,service)

        every { service.generateStoreTokensAndReturnSignedUrl(any(),any(),any()) } returns  CtaTokenZipFile(URL("https://example.com"),"file-name","password")
        every { snsClient.publishMessage("URL_TOPIC", any(),any(),any()) } returns PublishResult()
        every { snsClient.publishMessage("PASSWORD_TOPIC", any(),any()) } returns PublishResult()
        every { passwordGenerator.generate() } returns "password"

        handler.handleRequest(ScheduledEvent(), proxy())

        verifySequence {
            // Batch #1
            service.generateStoreTokensAndReturnSignedUrl(createListOfCtaZipFileRequests(7),any(),any())
            // Batch #2
            service.generateStoreTokensAndReturnSignedUrl(createListOfCtaZipFileRequests(7),any(),any())
        }
        verify (exactly = 1) {
            snsClient.publishMessage(
                "URL_TOPIC",
                match { it.contains("Please download the ZIP") && it.contains("https://example.com") },
                match { it.containsKey("batchNumber") && it["batchNumber"]?.value == "1" && it["batchNumber"] is NumericAttribute},
            "CTA Tokens"
            )
        }
        verify (exactly = 1){
            snsClient.publishMessage(
                "PASSWORD_TOPIC",
                match {it .contains("The password of")  && it.contains("password") && it.contains("file-name")},
                match {it.containsKey("batchNumber") && it["batchNumber"]?.value == "1" && it["batchNumber"] is NumericAttribute}
            )
        }

        verify (exactly = 1) {
            snsClient.publishMessage(
                "URL_TOPIC",
                match { it.contains("Please download the ZIP") && it.contains("https://example.com") },
                match { it.containsKey("batchNumber") && it["batchNumber"]?.value == "2" && it["batchNumber"] is NumericAttribute},
                "CTA Tokens"
            )
        }
        verify (exactly = 1){
            snsClient.publishMessage(
                "PASSWORD_TOPIC",
                match {it .contains("The password of")  && it.contains("password") && it.contains("file-name")},
                match {it.containsKey("batchNumber") && it["batchNumber"]?.value == "2" && it["batchNumber"] is NumericAttribute}
            )
        }
    }


    @Test
    fun `wrong number-of-days configuration`() {
        val environment = TestEnvironments.TEST.apply(
            mapOf(
                "number_of_days" to "asd",
                "number_of_tokens" to "20",
                "number_of_batches" to "2",
                )
        )
        val handler = ScheduledCtaTokenGenerationHandler(events,environment,snsClient,passwordGenerator,service)

        every { service.generateStoreTokensAndReturnSignedUrl(any(),any(),any()) } returns  CtaTokenZipFile(URL("https://example.com"),"file-name","password")

        assertThrows<NumberFormatException> {         handler.handleRequest(ScheduledEvent(), proxy()) }

    }

    @Test
    fun `wrong number-of-tokens configuration`() {
        val environment = TestEnvironments.TEST.apply(
            mapOf(
                "number_of_days" to "3",
                "number_of_tokens" to "asd",
                "number_of_batches" to "2",
                )
        )
        val handler = ScheduledCtaTokenGenerationHandler(events,environment,snsClient,passwordGenerator,service)

        every { service.generateStoreTokensAndReturnSignedUrl(any(),any(),any()) } returns  CtaTokenZipFile(URL("https://example.com"),"file-name","password")
        assertThrows<NumberFormatException> {         handler.handleRequest(ScheduledEvent(), proxy()) }

    }

    @Test
    fun `wrong number_of_batches configuration`() {
        val environment = TestEnvironments.TEST.apply(
            mapOf(
                "number_of_days" to "3",
                "number_of_tokens" to "7",
                "number_of_batches" to "asd",
            )
        )
        val handler = ScheduledCtaTokenGenerationHandler(events,environment,snsClient,passwordGenerator,service)

        every { service.generateStoreTokensAndReturnSignedUrl(any(),any(),any()) } returns  CtaTokenZipFile(URL("https://example.com"),"file-name","password")
        assertThrows<NumberFormatException> {         handler.handleRequest(ScheduledEvent(), proxy()) }

    }

    private fun createListOfCtaZipFileRequests(days: Long): List<CtaTokenZipFileEntryRequest> {
        return (1 until days+1).map { createListOfCtaZipFileRequest(it) }
    }

    private fun createListOfCtaZipFileRequest(daysOffset: Long): CtaTokenZipFileEntryRequest {
        val today = LocalDate.now()
        val endDate = today.plus(daysOffset, ChronoUnit.DAYS)
        return CtaTokenZipFileEntryRequest(TestResult.Positive, TestEndDate.of(endDate), TestKit.LAB_RESULT,  CSV_FILENAME_FORMATER.format(endDate),20)

    }
}

