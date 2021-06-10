package uk.nhs.nhsx.isolationpayment

import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifySequence
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.isolationpayment.model.IsolationToken
import uk.nhs.nhsx.isolationpayment.model.TokenGenerationRequest
import uk.nhs.nhsx.isolationpayment.model.TokenGenerationResponse.Disabled
import uk.nhs.nhsx.isolationpayment.model.TokenGenerationResponse.OK
import uk.nhs.nhsx.isolationpayment.model.TokenStateInternal
import uk.nhs.nhsx.isolationpayment.model.TokenUpdateRequest
import uk.nhs.nhsx.testhelper.data.asInstant
import uk.nhs.nhsx.domain.Country
import uk.nhs.nhsx.domain.Country.Companion.England
import uk.nhs.nhsx.domain.IpcTokenId
import java.time.Instant
import java.time.Period
import java.util.Optional
import java.util.function.Supplier

class IsolationPaymentMobileServiceTest {

    private val now = Instant.ofEpochSecond(0)
    private val clock = { now }
    private val tokenGenerator = Supplier { IpcTokenId.of("1".repeat(64)) }
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
        every { persistence.insertIsolationToken(any()) } just Runs

        when(val response = service.handleIsolationPaymentOrder(TokenGenerationRequest(England))) {
            is Disabled -> fail { "Country should have been whitelisted" }
            is OK -> {
                assertThat(response.ipcToken).isEqualTo(IpcTokenId.of("1".repeat(64)))
                assertThat(response.isEnabled).isEqualTo(true)
            }
        }

        val slot = slot<IsolationToken>()
        verify(exactly = 1) {
            persistence.insertIsolationToken(capture(slot))
        }

        assertThat(slot.captured.tokenId).isEqualTo(IpcTokenId.of("1".repeat(64)))
        assertThat(slot.captured.tokenStatus).isEqualTo(TokenStateInternal.INT_CREATED.value)
        assertThat(slot.captured.createdTimestamp).isEqualTo(clock().epochSecond)
        assertThat(slot.captured.expireAt).isEqualTo(clock().plus(Period.ofWeeks(tokenExpiryInWeeks)).epochSecond)
    }

    @Test
    fun `orders isolation payment and throws when token is not saved`() {
        every { persistence.insertIsolationToken(any()) } throws RuntimeException()

        assertThatThrownBy { service.handleIsolationPaymentOrder(TokenGenerationRequest(England)) }
            .isInstanceOf(RuntimeException::class.java)
    }

    @Test
    fun `orders isolation payment and returns empty token when country is not whitelisted`() {
        when(val response = service.handleIsolationPaymentOrder(TokenGenerationRequest(Country.of("Germany")))) {
            is OK -> fail { "Country should have been disabled" }
            is Disabled -> assertThat(response.isEnabled).isEqualTo(false)
        }
    }

    @Test
    fun `updates token successfully for existing token`() {
        val token = IsolationToken(
            IpcTokenId.of("1".repeat(64)),
            TokenStateInternal.INT_CREATED.value,
            0,
            5000,
            1000,
            2000,
            3000,
            4000,
            0
        )
        every { persistence.getIsolationToken(any()) } returns Optional.of(token)
        every { persistence.updateIsolationToken(any(), any()) } just Runs

        val request = TokenUpdateRequest(IpcTokenId.of("1".repeat(64)), "1970-01-01T00:00:00Z".asInstant(), "1970-01-01T00:00:01Z".asInstant())
        val response = service.handleIsolationPaymentUpdate(request)
        assertThat(response.websiteUrlWithQuery).isEqualTo("https://test/path?ipcToken=${"1".repeat(64)}")

        val slot = slot<IsolationToken>()
        verifySequence {
            persistence.getIsolationToken(IpcTokenId.of("1".repeat(64)))
            persistence.updateIsolationToken(capture(slot), TokenStateInternal.INT_CREATED)
        }

        assertThat(slot.captured.tokenId).isEqualTo(IpcTokenId.of("1".repeat(64)))
        assertThat(slot.captured.tokenStatus).isEqualTo(TokenStateInternal.INT_UPDATED.value)
        assertThat(slot.captured.createdTimestamp).isEqualTo(1000)
        assertThat(slot.captured.updatedTimestamp).isEqualTo(clock().epochSecond)
        assertThat(slot.captured.expireAt).isEqualTo(1)
    }

    @Test
    fun `does not update tokens that are in valid state`() {
        val token = IsolationToken(
            IpcTokenId.of("1".repeat(64)),
            TokenStateInternal.INT_UPDATED.value,
            0,
            5000,
            1000,
            2000,
            3000,
            4000,
            0
        )
        every { persistence.getIsolationToken(any()) } returns Optional.of(token)

        val request = TokenUpdateRequest(IpcTokenId.of("1".repeat(64)), "1970-01-01T00:00:00Z".asInstant(), "1970-01-01T00:00:01Z".asInstant())
        val response = service.handleIsolationPaymentUpdate(request)
        assertThat(response.websiteUrlWithQuery).isEqualTo("https://test/path?ipcToken=${"1".repeat(64)}")
    }

    @Test
    fun `update token does not throw exception on conditional check failure`() {

        val ipcToken = IpcTokenId.of("1".repeat(64))
        val epoch = "1970-01-01T00:00:01Z".asInstant()

        every {
            persistence.updateIsolationToken(
                any(),
                TokenStateInternal.INT_CREATED
            )
        } throws ConditionalCheckFailedException("")
        every { persistence.getIsolationToken(any()) } returns Optional.of(IsolationToken(ipcToken, "", createdTimestamp = epoch.epochSecond, expireAt = epoch.epochSecond))

        val request = TokenUpdateRequest(ipcToken, epoch, epoch)
        val response = service.handleIsolationPaymentUpdate(request)
        assertThat(response.websiteUrlWithQuery).isEqualTo("https://test/path?ipcToken=${"1".repeat(64)}")
    }
}
