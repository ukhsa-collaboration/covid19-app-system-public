package uk.nhs.nhsx.diagnosiskeydist

import batchZipCreation.Exposure
import batchZipCreation.Exposure.TemporaryExposureKey
import com.google.protobuf.ByteString
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue
import strikt.assertions.withElementAt
import strikt.assertions.withFirst
import uk.nhs.nhsx.diagnosiskeydist.apispec.DailyZIPSubmissionPeriod
import uk.nhs.nhsx.diagnosiskeydist.apispec.ZIPSubmissionPeriod
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey
import java.nio.ByteBuffer
import java.time.Duration
import java.time.Instant
import java.util.*

class ExposureProtobufTest {
    private val bundleId = "some-bundle-id"
    private val sig = "some-sig"

    private val now = Instant.parse("2021-01-20T13:00:00.000Z")
    private val zero = Duration.ZERO

    private val exposureProtobuf = ExposureProtobuf(bundleId)
    private val signatureResult = ByteBuffer.wrap(sig.toByteArray())
    private val expectedSignatureInfo = Exposure.SignatureInfo.newBuilder()
        .setAndroidPackage(bundleId)
        .setAppBundleId(bundleId)
        .setVerificationKeyVersion("v1")
        .setVerificationKeyId("234")
        .setSignatureAlgorithm("1.2.840.10045.4.3.2")
        .build()

    private val storedKeys = listOf(
        StoredTemporaryExposureKey("W2zb3BeMWt6Xr2u0ABG32Q==", 12345, 144, 7),
        StoredTemporaryExposureKey("kzQt9Lf3xjtAlMtm7jkSqw==", 12499, 144, 7, 4)
    )

    @Test
    fun `signature list contains one signature`() {
        val tekSignatureList = exposureProtobuf.buildTEKSignatureList(signatureResult)
        val signaturesList = tekSignatureList.signaturesList

        expectThat(signaturesList).hasSize(1)
    }

    @Test
    fun `signature has batch num and size`() {
        val tekSignatureList = exposureProtobuf.buildTEKSignatureList(signatureResult)
        val tekSignature = tekSignatureList.signaturesList[0]

        expectThat(tekSignature.batchNum).isEqualTo(1)
        expectThat(tekSignature.batchSize).isEqualTo(1)
    }

    @Test
    fun `signature matches the one provided`() {
        val tekSignatureList = exposureProtobuf.buildTEKSignatureList(signatureResult)
        val tekSignature = tekSignatureList.signaturesList[0]

        expectThat(tekSignature.signature).isEqualTo(ByteString.copyFrom(sig.toByteArray()))
    }

    @Test
    fun `signature info created with correct values`() {
        val tekSignatureList = exposureProtobuf.buildTEKSignatureList(signatureResult)
        val tekSignature = tekSignatureList.signaturesList[0]

        expectThat(tekSignature.signatureInfo).isEqualTo(expectedSignatureInfo)
    }

    @Test
    fun `export contains correct amount of keys`() {
        val period = DailyZIPSubmissionPeriod.periodForSubmissionDate(now)
        val tekExport = exposureProtobuf.buildTemporaryExposureKeyExport(storedKeys, period, zero)

        expectThat(tekExport.keysList).hasSize(2)
        expectThat(tekExport.keysCount).isEqualTo(2)
    }

    @Test
    fun `export keys have same data`() {
        val period = DailyZIPSubmissionPeriod.periodForSubmissionDate(now)
        val tekExport = exposureProtobuf.buildTemporaryExposureKeyExport(storedKeys, period, zero)
        val exposureKeys = tekExport.keysList

        val expectedKey1 = TemporaryExposureKey.newBuilder()
            .setKeyData(ByteString.copyFrom(Base64.getDecoder().decode("W2zb3BeMWt6Xr2u0ABG32Q==")))
            .setRollingPeriod(144)
            .setRollingStartIntervalNumber(12345)
            .setTransmissionRiskLevel(7)
            .setDaysSinceOnsetOfSymptoms(0)
            .build()

        val expectedKey2 = TemporaryExposureKey.newBuilder()
            .setKeyData(ByteString.copyFrom(Base64.getDecoder().decode("kzQt9Lf3xjtAlMtm7jkSqw==")))
            .setRollingPeriod(144)
            .setRollingStartIntervalNumber(12499)
            .setTransmissionRiskLevel(7)
            .setDaysSinceOnsetOfSymptoms(4)
            .build()

        expectThat(exposureKeys).isEqualTo(listOf(expectedKey1, expectedKey2))
    }

    @Test
    fun `export has batch num and size`() {
        val period = DailyZIPSubmissionPeriod.periodForSubmissionDate(now)
        val tekExport = exposureProtobuf.buildTemporaryExposureKeyExport(storedKeys, period, zero)

        expectThat(tekExport.batchNum).isEqualTo(1)
        expectThat(tekExport.batchSize).isEqualTo(1)
    }

    @Test
    fun `export has default region`() {
        val period = DailyZIPSubmissionPeriod.periodForSubmissionDate(now)
        val tekExport = exposureProtobuf.buildTemporaryExposureKeyExport(storedKeys, period, zero)

        expectThat(tekExport.region).isEmpty()
    }

    @Test
    fun `export has right signature infos count`() {
        val period = DailyZIPSubmissionPeriod.periodForSubmissionDate(now)
        val tekExport = exposureProtobuf.buildTemporaryExposureKeyExport(storedKeys, period, zero)

        expectThat(tekExport.signatureInfosCount).isEqualTo(1)
        expectThat(tekExport.signatureInfosList).hasSize(1)
    }

    @Test
    fun `export has right signature infos content`() {
        val period = DailyZIPSubmissionPeriod.periodForSubmissionDate(now)
        val tekExport = exposureProtobuf.buildTemporaryExposureKeyExport(storedKeys, period, zero)

        expectThat(tekExport.getSignatureInfos(0)).isEqualTo(expectedSignatureInfo)
    }

    @Test
    fun `export time stamps`() {
        val period: ZIPSubmissionPeriod = DailyZIPSubmissionPeriod.periodForSubmissionDate(now)
        val tekExport = exposureProtobuf.buildTemporaryExposureKeyExport(storedKeys, period, zero)

        expectThat(tekExport.startTimestamp).isEqualTo(1611100800L)
        expectThat(tekExport.endTimestamp).isEqualTo(1611187200L)
    }

    @Test
    fun `export time stamps with offset`() {
        val period = DailyZIPSubmissionPeriod.periodForSubmissionDate(now)
        val tekExport = exposureProtobuf.buildTemporaryExposureKeyExport(storedKeys, period, Duration.ofMinutes(-15))

        expectThat(tekExport.startTimestamp).isEqualTo(1611099900L)
        expectThat(tekExport.endTimestamp).isEqualTo(1611186300L)
    }

    @Test
    fun `export contains days since onset of symptoms when key not present`() {
        val period = DailyZIPSubmissionPeriod.periodForSubmissionDate(now)
        val tekExport = exposureProtobuf.buildTemporaryExposureKeyExport(storedKeys, period, zero)
        val exposureKeys = tekExport.keysList

        expectThat(exposureKeys.toList()).withFirst {
            get(TemporaryExposureKey::hasDaysSinceOnsetOfSymptoms).isTrue()
            get(TemporaryExposureKey::getDaysSinceOnsetOfSymptoms).isEqualTo(0)
        }
    }

    @Test
    fun `export contains days since onset of symptoms when key present`() {
        val period = DailyZIPSubmissionPeriod.periodForSubmissionDate(now)
        val tekExport = exposureProtobuf.buildTemporaryExposureKeyExport(storedKeys, period, zero)
        val exposureKeys = tekExport.keysList

        expectThat(exposureKeys.toList()).withElementAt(1) {
            get(TemporaryExposureKey::hasDaysSinceOnsetOfSymptoms).isTrue()
            get(TemporaryExposureKey::getDaysSinceOnsetOfSymptoms).isEqualTo(4)
        }
    }
}
