package uk.nhs.nhsx.diagnosiskeydist;

import batchZipCreation.Exposure.*;
import com.google.protobuf.ByteString;
import uk.nhs.nhsx.diagnosiskeydist.apispec.ZIPSubmissionPeriod;
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;

public class ExposureProtobuf {

    private static final String SIGNATURE_ALGORITHM = "1.2.840.10045.4.3.2";
    private static final String MOBILE_COUNTRY_CODE = "234";
    private static final String VERIFICATION_KEY_VERSION = "v1";

    private final String mobileAppBundleId;

    public ExposureProtobuf(String mobileAppBundleId) {
        this.mobileAppBundleId = mobileAppBundleId;
    }

    public TEKSignatureList buildTEKSignatureList(ByteBuffer byteBufferSignatureResult) {
        TEKSignature tekSignature = TEKSignature
            .newBuilder()
            .setBatchNum(1)
            .setBatchSize(1)
            .setSignatureInfo(buildSignatureInfo())
            .setSignature(ByteString.copyFrom(byteBufferSignatureResult))
            .build();

        return TEKSignatureList
            .newBuilder()
            .addSignatures(tekSignature)
            .build();
    }

    public TemporaryExposureKeyExport buildTemporaryExposureKeyExport(List<StoredTemporaryExposureKey> keys,
                                                                      ZIPSubmissionPeriod period,
                                                                      Duration offset) {
        return TemporaryExposureKeyExport
            .newBuilder()
            .setStartTimestamp(period.getStartInclusive().plus(offset).getEpochSecond())
            .setEndTimestamp(period.getEndExclusive().plus(offset).getEpochSecond())
            .setBatchNum(1)
            .setBatchSize(1)
            .addSignatureInfos(buildSignatureInfo())
            .addAllKeys(buildTekList(keys))
            .build();
    }

    private List<TemporaryExposureKey> buildTekList(List<StoredTemporaryExposureKey> keys) {
        return keys.stream()
            .map(buildTemporaryExposureKey())
            .collect(toList());
    }

    private Function<StoredTemporaryExposureKey, TemporaryExposureKey> buildTemporaryExposureKey() {
        return this::buildTemporaryExposureKey;
    }

    private TemporaryExposureKey buildTemporaryExposureKey(StoredTemporaryExposureKey tek) {
        return TemporaryExposureKey
            .newBuilder()
            .setKeyData(ByteString.copyFrom(Base64.getDecoder().decode(tek.key)))
            .setRollingStartIntervalNumber(tek.rollingStartNumber)
            .setRollingPeriod(tek.rollingPeriod)
            .setTransmissionRiskLevel(tek.transmissionRisk)
            .setDaysSinceOnsetOfSymptoms(tek.daysSinceOnsetOfSymptoms != null ? tek.daysSinceOnsetOfSymptoms : 0)
            .build();
    }

    private SignatureInfo buildSignatureInfo() {
        return SignatureInfo
            .newBuilder()
            .setAndroidPackage(mobileAppBundleId)
            .setAppBundleId(mobileAppBundleId)
            .setSignatureAlgorithm(SIGNATURE_ALGORITHM)
            .setVerificationKeyId(MOBILE_COUNTRY_CODE)
            .setVerificationKeyVersion(VERIFICATION_KEY_VERSION)
            .build();
    }
}
