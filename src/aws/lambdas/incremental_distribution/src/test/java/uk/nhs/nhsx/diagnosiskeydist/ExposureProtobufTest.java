package uk.nhs.nhsx.diagnosiskeydist;

import batchZipCreation.Exposure;
import com.google.protobuf.ByteString;
import org.junit.Test;
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.function.Supplier;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class ExposureProtobufTest {

    private static final String BUNDLE_ID = "some-bundle-id";
    private static final String SIG = "some-sig";

    private final Supplier<Instant> systemClock = () -> Instant.ofEpochSecond(0);
    private final ExposureProtobuf exposureProtobuf = new ExposureProtobuf(BUNDLE_ID, systemClock);
    private final ByteBuffer signatureResult = ByteBuffer.wrap(SIG.getBytes());

    private final Exposure.SignatureInfo expectedSignatureInfo = Exposure.SignatureInfo.newBuilder()
        .setAndroidPackage(BUNDLE_ID)
        .setAppBundleId(BUNDLE_ID)
        .setVerificationKeyVersion("v1")
        .setVerificationKeyId("234")
        .setSignatureAlgorithm("1.2.840.10045.4.3.2")
        .build();

    private final List<StoredTemporaryExposureKey> storedKeys =
        asList(
            new StoredTemporaryExposureKey("W2zb3BeMWt6Xr2u0ABG32Q==", 12345, 144),
            new StoredTemporaryExposureKey("kzQt9Lf3xjtAlMtm7jkSqw==", 12499, 144)
        );

    @Test
    public void signatureListContainsOneSignature() {
        Exposure.TEKSignatureList tekSignatureList = exposureProtobuf.buildTEKSignatureList(signatureResult);
        List<Exposure.TEKSignature> signaturesList = tekSignatureList.getSignaturesList();

        assertThat(signaturesList).hasSize(1);
    }

    @Test
    public void signatureHasBatchNumAndSize() {
        Exposure.TEKSignatureList tekSignatureList = exposureProtobuf.buildTEKSignatureList(signatureResult);
        Exposure.TEKSignature tekSignature = tekSignatureList.getSignaturesList().get(0);

        assertThat(tekSignature.getBatchNum()).isEqualTo(1);
        assertThat(tekSignature.getBatchSize()).isEqualTo(1);
    }

    @Test
    public void signatureMatchesTheOneProvided() {
        Exposure.TEKSignatureList tekSignatureList = exposureProtobuf.buildTEKSignatureList(signatureResult);
        Exposure.TEKSignature tekSignature = tekSignatureList.getSignaturesList().get(0);

        assertThat(tekSignature.getSignature()).isEqualTo(ByteString.copyFrom(SIG.getBytes()));
    }

    @Test
    public void signatureInfoCreatedWithCorrectValues() {
        Exposure.TEKSignatureList tekSignatureList = exposureProtobuf.buildTEKSignatureList(signatureResult);
        Exposure.TEKSignature tekSignature = tekSignatureList.getSignaturesList().get(0);

        assertThat(tekSignature.getSignatureInfo())
            .isEqualTo(expectedSignatureInfo);
    }

    @Test
    public void exportContainsCorrectAmountOfKeys() {
        Exposure.TemporaryExposureKeyExport tekExport = exposureProtobuf.buildTemporaryExposureKeyExport(storedKeys);

        assertThat(tekExport.getKeysList()).hasSize(2);
        assertThat(tekExport.getKeysCount()).isEqualTo(2);
    }

    @Test
    public void exportKeysHaveSameData() {
        Exposure.TemporaryExposureKeyExport tekExport = exposureProtobuf.buildTemporaryExposureKeyExport(storedKeys);

        List<Exposure.TemporaryExposureKey> exposureKeys = tekExport.getKeysList();

        Exposure.TemporaryExposureKey expectedKey1 =
            Exposure.TemporaryExposureKey.newBuilder()
                .setKeyData(ByteString.copyFrom(Base64.getDecoder().decode("W2zb3BeMWt6Xr2u0ABG32Q==")))
                .setRollingPeriod(144)
                .setRollingStartIntervalNumber(12345)
                .setTransmissionRiskLevel(7)
                .build();

        Exposure.TemporaryExposureKey expectedKey2 =
            Exposure.TemporaryExposureKey.newBuilder()
                .setKeyData(ByteString.copyFrom(Base64.getDecoder().decode("kzQt9Lf3xjtAlMtm7jkSqw==")))
                .setRollingPeriod(144)
                .setRollingStartIntervalNumber(12499)
                .setTransmissionRiskLevel(7)
                .build();

        assertThat(exposureKeys).isEqualTo(asList(expectedKey1, expectedKey2));
    }

    @Test
    public void exportHasBatchNumAndSize() {
        Exposure.TemporaryExposureKeyExport tekExport = exposureProtobuf.buildTemporaryExposureKeyExport(storedKeys);

        assertThat(tekExport.getBatchNum()).isEqualTo(1);
        assertThat(tekExport.getBatchSize()).isEqualTo(1);
    }

    @Test
    public void exportHasDefaultRegion() {
        Exposure.TemporaryExposureKeyExport tekExport = exposureProtobuf.buildTemporaryExposureKeyExport(storedKeys);

        assertThat(tekExport.getRegion()).isEmpty();
    }

    @Test
    public void exportHasRightSignatureInfosCount() {
        Exposure.TemporaryExposureKeyExport tekExport = exposureProtobuf.buildTemporaryExposureKeyExport(storedKeys);

        assertThat(tekExport.getSignatureInfosCount()).isEqualTo(1);
        assertThat(tekExport.getSignatureInfosList()).hasSize(1);
    }

    @Test
    public void exportHasRightSignatureInfosContent() {
        Exposure.TemporaryExposureKeyExport tekExport = exposureProtobuf.buildTemporaryExposureKeyExport(storedKeys);

        assertThat(tekExport.getSignatureInfos(0)).isEqualTo(expectedSignatureInfo);
    }

    @Test
    public void exportTimeStamps() {
        Exposure.TemporaryExposureKeyExport tekExport = exposureProtobuf.buildTemporaryExposureKeyExport(storedKeys);

        assertThat(tekExport.getStartTimestamp()).isEqualTo(-1209600); // means: now - 14 days in secs
        assertThat(tekExport.getEndTimestamp()).isEqualTo(0); // means: now in secs
    }
}