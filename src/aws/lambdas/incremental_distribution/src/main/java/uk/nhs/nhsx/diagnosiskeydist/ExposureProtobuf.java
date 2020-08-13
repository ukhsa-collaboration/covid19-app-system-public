package uk.nhs.nhsx.diagnosiskeydist;

import batchZipCreation.Exposure;
import batchZipCreation.Exposure.TemporaryExposureKey;
import com.google.protobuf.ByteString;
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.Period;
import java.util.Base64;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ExposureProtobuf {

    private static final String SIGNATURE_ALGORITHM = "1.2.840.10045.4.3.2";
    private static final String MOBILE_COUNTRY_CODE = "234";
    private static final String VERIFICATION_KEY_VERSION = "v1";

    private final String mobileAppBundleId;
    private final Supplier<Instant> clock;

    public ExposureProtobuf(String mobileAppBundleId, Supplier<Instant> clock) {
        this.mobileAppBundleId = mobileAppBundleId;
        this.clock = clock;
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

    public Exposure.TemporaryExposureKeyExport buildTemporaryExposureKeyExport(List<StoredTemporaryExposureKey> keys) {
        Instant now = clock.get();
        return Exposure.TemporaryExposureKeyExport
            .newBuilder()
            .setStartTimestamp(now.minus(Period.ofDays(14)).getEpochSecond())
            .setEndTimestamp(now.getEpochSecond())
            .setBatchNum(1)
            .setBatchSize(1)
            .addSignatureInfos(buildSignatureInfo())
            .addAllKeys(buildTekList(keys))
            .build();
    }

    private List<TemporaryExposureKey> buildTekList(List<StoredTemporaryExposureKey> keys) {
        return keys.stream()
            .map(k -> buildTemporaryExposureKey(k.key, k.rollingStartNumber, k.rollingPeriod, k.transmissionRisk))
            .collect(Collectors.toList());
    }

    private TemporaryExposureKey buildTemporaryExposureKey(String key,
                                                          Integer rollingStartNumber,
                                                          Integer rollingPeriod,
                                                          Integer transmissionRisk) {
        return Exposure.TemporaryExposureKey
            .newBuilder()
            .setKeyData(ByteString.copyFrom(Base64.getDecoder().decode(key)))
            .setRollingStartIntervalNumber(rollingStartNumber)
            .setRollingPeriod(rollingPeriod)
            .setTransmissionRiskLevel(transmissionRisk)
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
