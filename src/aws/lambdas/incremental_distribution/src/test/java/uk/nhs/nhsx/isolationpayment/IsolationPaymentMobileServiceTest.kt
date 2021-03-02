package uk.nhs.nhsx.isolationpayment

import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.isolationpayment.model.*
import java.time.Instant
import java.time.Period
import java.util.*
import java.util.function.Supplier

class IsolationPaymentMobileServiceTest {

    private val now = Instant.ofEpochSecond(0)
    private val clock = Supplier { now }
    private val tokenGenerator = Supplier { "some-token" }
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

        val response = service.handleIsolationPaymentOrder(TokenGenerationRequest("England"))
        assertThat(response.ipcToken).isEqualTo("some-token")
        assertThat(response.isEnabled).isEqualTo(true)

        val slot = slot<IsolationToken>()
        verify(exactly = 1) {
            persistence.insertIsolationToken(capture(slot))
        }

        assertThat(slot.captured.tokenId).isEqualTo("some-token")
        assertThat(slot.captured.tokenStatus).isEqualTo(TokenStateInternal.INT_CREATED.value)
        assertThat(slot.captured.createdTimestamp).isEqualTo(clock.get().epochSecond)
        assertThat(slot.captured.expireAt).isEqualTo(clock.get().plus(Period.ofWeeks(tokenExpiryInWeeks)).epochSecond)
    }

    @Test
    fun `orders isolation payment and throws when token is not saved`() {
        every { persistence.insertIsolationToken(any()) } throws RuntimeException()

        assertThatThrownBy { service.handleIsolationPaymentOrder(TokenGenerationRequest("England")) }
            .isInstanceOf(RuntimeException::class.java)
    }

    @Test
    fun `orders isolation payment and returns empty token when country is not whitelisted`() {
        val response = service.handleIsolationPaymentOrder(TokenGenerationRequest("Germany"))

        assertThat(response.ipcToken).isNull()
        assertThat(response.isEnabled).isEqualTo(false)
    }

    @Test
    fun `updates token successfully for existing token`() {
        val token = IsolationToken(
            "token-id",
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


        val request = TokenUpdateRequest("token-id", "1970-01-01T00:00:00Z", "1970-01-01T00:00:01Z")
        val response = service.handleIsolationPaymentUpdate(request)
        assertThat(response.websiteUrlWithQuery).isEqualTo("https://test/path?ipcToken=token-id")

        val slot = slot<IsolationToken>()
        verifySequence {
            persistence.getIsolationToken("token-id")
            persistence.updateIsolationToken(capture(slot), TokenStateInternal.INT_CREATED)
        }

        assertThat(slot.captured.tokenId).isEqualTo("token-id")
        assertThat(slot.captured.tokenStatus).isEqualTo(TokenStateInternal.INT_UPDATED.value)
        assertThat(slot.captured.createdTimestamp).isEqualTo(1000)
        assertThat(slot.captured.updatedTimestamp).isEqualTo(clock.get().epochSecond)
        assertThat(slot.captured.expireAt).isEqualTo(1)
    }

    @Test
    fun `does not update tokens that are in valid state`() {
        val token = IsolationToken(
            "token-id",
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

        val request = TokenUpdateRequest("token-id", "1970-01-01T00:00:00Z", "1970-01-01T00:00:01Z")
        val response = service.handleIsolationPaymentUpdate(request)
        assertThat(response.websiteUrlWithQuery).isEqualTo("https://test/path?ipcToken=token-id")
    }

    @Test
    fun `update token does not throw exception on conditional check failure`() {
        every {
            persistence.updateIsolationToken(
                any(),
                TokenStateInternal.INT_CREATED
            )
        } throws ConditionalCheckFailedException("")
        every { persistence.getIsolationToken(any()) } returns Optional.of(IsolationToken())

        val request = TokenUpdateRequest("token-id", "1970-01-01T00:00:00Z", "1970-01-01T00:00:01Z")
        val response = service.handleIsolationPaymentUpdate(request)
        assertThat(response.websiteUrlWithQuery).isEqualTo("https://test/path?ipcToken=token-id")
    }
}
