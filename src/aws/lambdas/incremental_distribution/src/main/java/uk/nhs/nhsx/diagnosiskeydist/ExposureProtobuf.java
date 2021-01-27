package uk.nhs.nhsx.diagnosiskeydist;

import batchZipCreation.Exposure;
import batchZipCreation.Exposure.TemporaryExposureKey;
import com.google.protobuf.ByteString;
import uk.nhs.nhsx.diagnosiskeydist.apispec.ZIPSubmissionPeriod;
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ExposureProtobuf {

    private static final String SIGNATURE_ALGORITHM = "1.2.840.10045.4.3.2";
    private static final String MOBILE_COUNTRY_CODE = "234";
    private static final String VERIFICATION_KEY_VERSION = "v1";

    private final String mobileAppBundleId;

    public ExposureProtobuf(String mobileAppBundleId) {
        this.mobileAppBundleId = mobileAppBundleId;
    }

    public Exposure.TEKSignatureList buildTEKSignatureList(ByteBuffer byteBufferSignatureResult) {
        Exposure.TEKSignature tekSignature = Exposure.TEKSignature
            .newBuilder()
            .setBatchNum(1)
            .setBatchSize(1)
            .setSignatureInfo(buildSignatureInfo())
            .setSignature(ByteString.copyFrom(byteBufferSignatureResult))
            .build();

        return Exposure.TEKSignatureList
            .newBuilder()
            .addSignatures(tekSignature)
            .build();
    }

    public Exposure.TemporaryExposureKeyExport buildTemporaryExposureKeyExport(List<StoredTemporaryExposureKey> keys, ZIPSubmissionPeriod period, long periodOffsetMinutes) {
        return Exposure.TemporaryExposureKeyExport
            .newBuilder()
            .setStartTimestamp((Date.from(period.getStartInclusive()).getTime() / 1000) + periodOffsetMinutes * 60L)
            .setEndTimestamp((Date.from(period.getEndExclusive()).getTime() / 1000) + periodOffsetMinutes * 60L)
            .setBatchNum(1)
            .setBatchSize(1)
            .addSignatureInfos(buildSignatureInfo())
            .addAllKeys(buildTekList(keys))
            .build();
    }

    private List<TemporaryExposureKey> buildTekList(List<StoredTemporaryExposureKey> keys) {
        return keys.stream()
            .map(buildTemporaryExposureKey())
            .collect(Collectors.toList());
    }

    private Function<StoredTemporaryExposureKey, TemporaryExposureKey> buildTemporaryExposureKey() {
        return this::buildTemporaryExposureKey;
    }

    private TemporaryExposureKey buildTemporaryExposureKey(StoredTemporaryExposureKey tek) {
        return Exposure.TemporaryExposureKey
            .newBuilder()
            .setKeyData(ByteString.copyFrom(Base64.getDecoder().decode(tek.key)))
            .setRollingStartIntervalNumber(tek.rollingStartNumber)
            .setRollingPeriod(tek.rollingPeriod)
            .setTransmissionRiskLevel(tek.transmissionRisk)
            .setDaysSinceOnsetOfSymptoms(tek.daysSinceOnsetOfSymptoms != null ? tek.daysSinceOnsetOfSymptoms : 0)
            .build();
    }

    private Exposure.SignatureInfo buildSignatureInfo() {
        return Exposure.SignatureInfo
            .newBuilder()
            .setAndroidPackage(mobileAppBundleId)
            .setAppBundleId(mobileAppBundleId)
            .setSignatureAlgorithm(SIGNATURE_ALGORITHM)
            .setVerificationKeyId(MOBILE_COUNTRY_CODE)
            .setVerificationKeyVersion(VERIFICATION_KEY_VERSION)
            .build();
    }
}
