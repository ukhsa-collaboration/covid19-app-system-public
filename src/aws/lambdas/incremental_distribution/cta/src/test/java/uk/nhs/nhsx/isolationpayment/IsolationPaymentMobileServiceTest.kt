package uk.nhs.nhsx.isolationpayment

import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.domain.Country
import uk.nhs.nhsx.domain.Country.Companion.England
import uk.nhs.nhsx.domain.IpcTokenId
import uk.nhs.nhsx.isolationpayment.model.IsolationToken
import uk.nhs.nhsx.isolationpayment.model.TokenGenerationRequest
import uk.nhs.nhsx.isolationpayment.model.TokenGenerationResponse.Disabled
import uk.nhs.nhsx.isolationpayment.model.TokenGenerationResponse.OK
import uk.nhs.nhsx.isolationpayment.model.TokenStateInternal.INT_CREATED
import uk.nhs.nhsx.isolationpayment.model.TokenStateInternal.INT_UPDATED
import uk.nhs.nhsx.isolationpayment.model.TokenUpdateRequest
import uk.nhs.nhsx.isolationpayment.model.TokenUpdateResponse
import uk.nhs.nhsx.testhelper.assertions.withCaptured
import uk.nhs.nhsx.testhelper.data.asInstant
import java.time.Instant
import java.time.Period
import java.util.*
import java.util.function.Supplier

class IsolationPaymentMobileServiceTest {

    private val now = Instant.ofEpochSecond(0)
    private val clock = { now }
    private val tokenGenerator = { IpcTokenId.of("1".repeat(64)) }
    private val persistence = mockk<IsolationPaymentPersistence>()
    private val websiteOrderUrl = "https://test/path?ipcToken="
    private val tokenExpiryInWeeks = 4
    private val countriesWhitelisted = listOf("England", "Wales")
    private val events = RecordingEvents()

    private val service = IsolationPaymentMobileService(
        clock,
        tokenGenerator,
        persistence,
        websiteOrderUrl,
        tokenExpiryInWeeks,
        countriesWhitelisted,
        "audit_log",
        events
    )

    @Test
    fun `orders isolation payment`() {
        every { persistence.insertIsolationToken(any()) } just runs

        when (val response = service.handleIsolationPaymentOrder(TokenGenerationRequest(England))) {
            is Disabled -> fail { "Country should have been whitelisted" }
            is OK -> expectThat(response) {
                get(OK::ipcToken).isEqualTo(IpcTokenId.of("1".repeat(64)))
                get(OK::isEnabled).isTrue()
            }
        }

        val isolationToken = slot<IsolationToken>()

        verify(exactly = 1) {
            persistence.insertIsolationToken(capture(isolationToken))
        }

        expectThat(isolationToken).withCaptured {
            get(IsolationToken::tokenId).isEqualTo(IpcTokenId.of("1".repeat(64)))
            get(IsolationToken::tokenStatus).isEqualTo(INT_CREATED.value)
            get(IsolationToken::createdTimestamp).isEqualTo(clock().epochSecond)
            get(IsolationToken::expireAt).isEqualTo(clock().plus(Period.ofWeeks(tokenExpiryInWeeks)).epochSecond)
        }
    }

    @Test
    fun `orders isolation payment and throws when token is not saved`() {
        every { persistence.insertIsolationToken(any()) } throws RuntimeException()

        expectThrows<RuntimeException> {
            service.handleIsolationPaymentOrder(TokenGenerationRequest(England))
        }
    }

    @Test
    fun `orders isolation payment and returns empty token when country is not whitelisted`() {
        when (val response = service.handleIsolationPaymentOrder(TokenGenerationRequest(Country.of("Germany")))) {
            is OK -> fail("Country should have been disabled")
            is Disabled -> expectThat(response).get(Disabled::isEnabled).isFalse()
        }
    }

    @Test
    fun `updates token successfully for existing token`() {
        val ipcTokenId = IpcTokenId.of("1".repeat(64))

        val token = IsolationToken(
            ipcTokenId,
            INT_CREATED.value,
            riskyEncounterDate = 0,
            isolationPeriodEndDate = 5000,
            createdTimestamp = 1000,
            updatedTimestamp = 2000,
            validatedTimestamp = 3000,
            consumedTimestamp = 4000,
            expireAt = 0
        )

        every { persistence.getIsolationToken(any()) } returns token
        every { persistence.updateIsolationToken(any(), any()) } just runs

        val request = TokenUpdateRequest(
            ipcTokenId,
            "1970-01-01T00:00:00Z".asInstant(),
            "1970-01-01T00:00:01Z".asInstant()
        )

        val response = service.handleIsolationPaymentUpdate(request)

        expectThat(response)
            .get(TokenUpdateResponse::websiteUrlWithQuery)
            .isEqualTo("https://test/path?ipcToken=${"1".repeat(64)}")

        val isolationToken = slot<IsolationToken>()
        verifySequence {
            persistence.getIsolationToken(ipcTokenId)
            persistence.updateIsolationToken(capture(isolationToken), INT_CREATED)
        }

        expectThat(isolationToken).withCaptured {
            get(IsolationToken::tokenId).isEqualTo(ipcTokenId)
            get(IsolationToken::tokenStatus).isEqualTo(INT_UPDATED.value)
            get(IsolationToken::createdTimestamp).isEqualTo(1000)
            get(IsolationToken::updatedTimestamp).isEqualTo(clock().epochSecond)
            get(IsolationToken::expireAt).isEqualTo(1)
        }
    }

    @Test
    fun `does not update tokens that are in valid state`() {
        val token = IsolationToken(
            IpcTokenId.of("1".repeat(64)),
            INT_UPDATED.value,
            riskyEncounterDate = 0,
            isolationPeriodEndDate = 5000,
            createdTimestamp = 1000,
            updatedTimestamp = 2000,
            validatedTimestamp = 3000,
            consumedTimestamp = 4000,
            expireAt = 0
        )

        every { persistence.getIsolationToken(any()) } returns token

        val request = TokenUpdateRequest(
            IpcTokenId.of("1".repeat(64)),
            "1970-01-01T00:00:00Z".asInstant(),
            "1970-01-01T00:00:01Z".asInstant()
        )

        val response = service.handleIsolationPaymentUpdate(request)

        expectThat(response)
            .get(TokenUpdateResponse::websiteUrlWithQuery)
            .isEqualTo("https://test/path?ipcToken=${"1".repeat(64)}")
    }

    @Test
    fun `update token does not throw exception on conditional check failure`() {
        val ipcToken = IpcTokenId.of("1".repeat(64))
        val epoch = "1970-01-01T00:00:01Z".asInstant()

        every {
            persistence.updateIsolationToken(any(), INT_CREATED)
        } throws ConditionalCheckFailedException("")

        every { persistence.getIsolationToken(any()) } returns IsolationToken(
            ipcToken,
            "",
            createdTimestamp = epoch.epochSecond,
            expireAt = epoch.epochSecond
        )

        val request = TokenUpdateRequest(ipcToken, epoch, epoch)
        val response = service.handleIsolationPaymentUpdate(request)

        expectThat(response)
            .get(TokenUpdateResponse::websiteUrlWithQuery)
            .isEqualTo("https://test/path?ipcToken=${"1".repeat(64)}")
    }
}
