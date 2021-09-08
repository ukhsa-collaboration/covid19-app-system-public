package uk.nhs.nhsx.analyticssubmission.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThan
import strikt.assertions.isNull
import uk.nhs.nhsx.analyticssubmission.analyticsSubmissionIos
import uk.nhs.nhsx.analyticssubmission.analyticsSubmissionIosComplete
import uk.nhs.nhsx.analyticssubmission.analyticsSubmissionAndroid
import uk.nhs.nhsx.core.Json
import kotlin.reflect.full.memberProperties

class ClientAnalyticsSubmissionPayloadTest {

    @Test
    fun `can deserialize first version of android payload`() {
        val json = analyticsSubmissionAndroid()

        val payload = Json.readJsonOrThrow<ClientAnalyticsSubmissionPayload>(json)
        expectThat(payload.metadata) {
            get { operatingSystemVersion }.isEqualTo("29")
            get { latestApplicationVersion }.isEqualTo("3.0")
            get { deviceModel }.isEqualTo("Pixel 4XL")
            get { postalDistrict }.isEqualTo("AB10")
            get { localAuthority }.isNull()
        }

        expectThat(payload.metrics) {
            get { cumulativeDownloadBytes }.isEqualTo(140000000)
            get { cumulativeUploadBytes }.isEqualTo(160000000)
            get { cumulativeCellularDownloadBytes }.isNull()
            get { cumulativeCellularUploadBytes }.isNull()
            get { cumulativeWifiDownloadBytes }.isNull()
            get { cumulativeWifiUploadBytes }.isNull()
        }
    }

    @Test
    fun `can deserialize first version ios payload`() {
        // iOS has extra fields for cellular and wifi bytes
        val json = analyticsSubmissionIos()

        val payload = Json.readJsonOrThrow<ClientAnalyticsSubmissionPayload>(json)
        expectThat(payload.metadata) {
            get { operatingSystemVersion }.isEqualTo("iPhone OS 13.5.1 (17F80)")
            get { latestApplicationVersion }.isEqualTo("3.0")
            get { deviceModel }.isEqualTo("iPhone11,2")
            get { postalDistrict }.isEqualTo("AB10")
            get { localAuthority }.isNull()
        }
    }

    @Test
    fun `can deserialize with local authority field`() {
        val json = analyticsSubmissionIos(localAuthority = "E06000051")

        val payload = Json.readJsonOrThrow<ClientAnalyticsSubmissionPayload>(json)
        expectThat(payload.metadata) {
            get { operatingSystemVersion }.isEqualTo("iPhone OS 13.5.1 (17F80)")
            get { latestApplicationVersion }.isEqualTo("3.0")
            get { deviceModel }.isEqualTo("iPhone11,2")
            get { postalDistrict }.isEqualTo("AB10")
            get { localAuthority }.isEqualTo("E06000051")
        }
    }

    @Test
    fun `can deserialize with all optional metrics in newer versions of payload`() {
        val json = analyticsSubmissionIosComplete(
            localAuthority = "E06000051",
            useCounter = true
        )

        val payload = Json.readJsonOrThrow<ClientAnalyticsSubmissionPayload>(json)
        expectThat(payload.metadata) {
            get { operatingSystemVersion }.isEqualTo("iPhone OS 13.5.1 (17F80)")
            get { latestApplicationVersion }.isEqualTo("3.0")
            get { deviceModel }.isEqualTo("iPhone11,2")
            get { postalDistrict }.isEqualTo("AB10")
            get { localAuthority }.isEqualTo("E06000051")
        }

        payload.metrics::class.memberProperties.forEach {
            val propertyValue = it.getter.call(payload.metrics)
                ?: fail("unexpected field [${it.name}], if this is a new field then add it to the unit test payload with non-null value greater than 0")

            when (propertyValue) {
                is Int -> expectThat(propertyValue).isGreaterThan(0)
                is Long -> expectThat(propertyValue).isGreaterThan(0)
                else -> fail("metrics in this class should be numbers, unexpected type [${it.returnType}] for field [${it.name}]")
            }
        }
    }
}
