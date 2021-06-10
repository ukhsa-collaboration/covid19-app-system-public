package uk.nhs.nhsx.diagnosiskeydist

import batchZipCreation.Exposure
import batchZipCreation.Exposure.TEKSignature
import batchZipCreation.Exposure.TEKSignatureList
import batchZipCreation.Exposure.TemporaryExposureKey
import batchZipCreation.Exposure.TemporaryExposureKeyExport
import com.google.protobuf.ByteString
import uk.nhs.nhsx.diagnosiskeydist.apispec.ZIPSubmissionPeriod
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey
import java.nio.ByteBuffer
import java.time.Duration
import java.util.Base64

class ExposureProtobuf(private val mobileAppBundleId: String) {

    fun buildTEKSignatureList(byteBufferSignatureResult: ByteBuffer?): TEKSignatureList = TEKSignatureList
        .newBuilder()
        .addSignatures(
            TEKSignature
                .newBuilder()
                .setBatchNum(1)
                .setBatchSize(1)
                .setSignatureInfo(buildSignatureInfo())
                .setSignature(ByteString.copyFrom(byteBufferSignatureResult))
                .build()
        )
        .build()

    fun buildTemporaryExposureKeyExport(
        keys: List<StoredTemporaryExposureKey>,
        period: ZIPSubmissionPeriod,
        offset: Duration?
    ): TemporaryExposureKeyExport = TemporaryExposureKeyExport
        .newBuilder()
        .setStartTimestamp(period.startInclusive.plus(offset).epochSecond)
        .setEndTimestamp(period.endExclusive.plus(offset).epochSecond)
        .setBatchNum(1)
        .setBatchSize(1)
        .addSignatureInfos(buildSignatureInfo())
        .addAllKeys(keys.map(::buildTemporaryExposureKey))
        .build()

    private fun buildTemporaryExposureKey(tek: StoredTemporaryExposureKey) = TemporaryExposureKey
        .newBuilder()
        .setKeyData(ByteString.copyFrom(Base64.getDecoder().decode(tek.key)))
        .setRollingStartIntervalNumber(tek.rollingStartNumber)
        .setRollingPeriod(tek.rollingPeriod)
        .setTransmissionRiskLevel(tek.transmissionRisk)
        .setDaysSinceOnsetOfSymptoms(tek.daysSinceOnsetOfSymptoms ?: 0)
        .build()

    private fun buildSignatureInfo() = Exposure.SignatureInfo
        .newBuilder()
        .setAndroidPackage(mobileAppBundleId)
        .setAppBundleId(mobileAppBundleId)
        .setSignatureAlgorithm(SIGNATURE_ALGORITHM)
        .setVerificationKeyId(MOBILE_COUNTRY_CODE)
        .setVerificationKeyVersion(VERIFICATION_KEY_VERSION)
        .build()

    companion object {
        private const val SIGNATURE_ALGORITHM = "1.2.840.10045.4.3.2"
        private const val MOBILE_COUNTRY_CODE = "234"
        private const val VERIFICATION_KEY_VERSION = "v1"
    }
}
