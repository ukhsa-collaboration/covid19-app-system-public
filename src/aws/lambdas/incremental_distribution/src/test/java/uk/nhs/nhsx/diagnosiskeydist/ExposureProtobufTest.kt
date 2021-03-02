package uk.nhs.nhsx.diagnosiskeydist

import batchZipCreation.Exposure
import batchZipCreation.Exposure.TemporaryExposureKey
import com.google.protobuf.ByteString
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.diagnosiskeydist.apispec.DailyZIPSubmissionPeriod
import uk.nhs.nhsx.diagnosiskeydist.apispec.ZIPSubmissionPeriod
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey
import java.nio.ByteBuffer
import java.time.Duration
import java.time.Instant
import java.util.*

@Suppress("UsePropertyAccessSyntax")
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
    fun signatureListContainsOneSignature() {
        val tekSignatureList = exposureProtobuf.buildTEKSignatureList(signatureResult)
        val signaturesList = tekSignatureList.signaturesList

        assertThat(signaturesList).hasSize(1)
    }

    @Test
    fun signatureHasBatchNumAndSize() {
        val tekSignatureList = exposureProtobuf.buildTEKSignatureList(signatureResult)
        val tekSignature = tekSignatureList.signaturesList[0]

        assertThat(tekSignature.batchNum).isEqualTo(1)
        assertThat(tekSignature.batchSize).isEqualTo(1)
    }

    @Test
    fun signatureMatchesTheOneProvided() {
        val tekSignatureList = exposureProtobuf.buildTEKSignatureList(signatureResult)
        val tekSignature = tekSignatureList.signaturesList[0]

        assertThat(tekSignature.signature).isEqualTo(ByteString.copyFrom(sig.toByteArray()))
    }

    @Test
    fun signatureInfoCreatedWithCorrectValues() {
        val tekSignatureList = exposureProtobuf.buildTEKSignatureList(signatureResult)
        val tekSignature = tekSignatureList.signaturesList[0]

        assertThat(tekSignature.signatureInfo).isEqualTo(expectedSignatureInfo)
    }

    @Test
    fun exportContainsCorrectAmountOfKeys() {
        val period: ZIPSubmissionPeriod = DailyZIPSubmissionPeriod.periodForSubmissionDate(now)
        val tekExport = exposureProtobuf.buildTemporaryExposureKeyExport(storedKeys, period, zero)

        assertThat(tekExport.keysList).hasSize(2)
        assertThat(tekExport.keysCount).isEqualTo(2)
    }

    @Test
    fun exportKeysHaveSameData() {
        val period: ZIPSubmissionPeriod = DailyZIPSubmissionPeriod.periodForSubmissionDate(now)
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
        assertThat(exposureKeys).isEqualTo(listOf(expectedKey1, expectedKey2))
    }

    @Test
    fun exportHasBatchNumAndSize() {
        val period: ZIPSubmissionPeriod = DailyZIPSubmissionPeriod.periodForSubmissionDate(now)
        val tekExport = exposureProtobuf.buildTemporaryExposureKeyExport(storedKeys, period, zero)

        assertThat(tekExport.batchNum).isEqualTo(1)
        assertThat(tekExport.batchSize).isEqualTo(1)
    }

    @Test
    fun exportHasDefaultRegion() {
        val period: ZIPSubmissionPeriod = DailyZIPSubmissionPeriod.periodForSubmissionDate(now)
        val tekExport = exposureProtobuf.buildTemporaryExposureKeyExport(storedKeys, period, zero)

        assertThat(tekExport.region).isEmpty()
    }

    @Test
    fun exportHasRightSignatureInfosCount() {
        val period: ZIPSubmissionPeriod = DailyZIPSubmissionPeriod.periodForSubmissionDate(now)
        val tekExport = exposureProtobuf.buildTemporaryExposureKeyExport(storedKeys, period, zero)

        assertThat(tekExport.signatureInfosCount).isEqualTo(1)
        assertThat(tekExport.signatureInfosList).hasSize(1)
    }

    @Test
    fun exportHasRightSignatureInfosContent() {
        val period: ZIPSubmissionPeriod = DailyZIPSubmissionPeriod.periodForSubmissionDate(now)
        val tekExport = exposureProtobuf.buildTemporaryExposureKeyExport(storedKeys, period, zero)

        assertThat(tekExport.getSignatureInfos(0)).isEqualTo(expectedSignatureInfo)
    }

    @Test
    fun exportTimeStamps() {
        val period: ZIPSubmissionPeriod = DailyZIPSubmissionPeriod.periodForSubmissionDate(now)
        val tekExport = exposureProtobuf.buildTemporaryExposureKeyExport(storedKeys, period, zero)

        assertThat(tekExport.startTimestamp).isEqualTo(1611100800L)
        assertThat(tekExport.endTimestamp).isEqualTo(1611187200L)
    }

    @Test
    fun exportTimeStampsWithOffset() {
        val period: ZIPSubmissionPeriod = DailyZIPSubmissionPeriod.periodForSubmissionDate(now)
        val tekExport = exposureProtobuf.buildTemporaryExposureKeyExport(storedKeys, period, Duration.ofMinutes(-15))

        assertThat(tekExport.startTimestamp).isEqualTo(1611099900L)
        assertThat(tekExport.endTimestamp).isEqualTo(1611186300L)
    }

    @Test
    fun exportContainsDaysSinceOnsetOfSymptomsWhenKeyNotPresent() {
        val period: ZIPSubmissionPeriod = DailyZIPSubmissionPeriod.periodForSubmissionDate(now)
        val tekExport = exposureProtobuf.buildTemporaryExposureKeyExport(storedKeys, period, zero)
        val exposureKeys = tekExport.keysList

        assertThat(exposureKeys[0].hasDaysSinceOnsetOfSymptoms()).isTrue()
        assertThat(exposureKeys[0].daysSinceOnsetOfSymptoms).isEqualTo(0)
    }

    @Test
    fun exportContainsDaysSinceOnsetOfSymptomsWhenKeyPresent() {
        val period: ZIPSubmissionPeriod = DailyZIPSubmissionPeriod.periodForSubmissionDate(now)
        val tekExport = exposureProtobuf.buildTemporaryExposureKeyExport(storedKeys, period, zero)
        val exposureKeys = tekExport.keysList

        assertThat(exposureKeys[1].hasDaysSinceOnsetOfSymptoms()).isTrue()
        assertThat(exposureKeys[1].daysSinceOnsetOfSymptoms).isEqualTo(4)
    }
}
