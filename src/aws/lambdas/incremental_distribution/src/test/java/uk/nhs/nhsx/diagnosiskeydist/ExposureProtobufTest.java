package uk.nhs.nhsx.diagnosiskeydist;

import batchZipCreation.Exposure;
import com.google.protobuf.ByteString;
import org.junit.Test;
import uk.nhs.nhsx.diagnosiskeydist.apispec.DailyZIPSubmissionPeriod;
import uk.nhs.nhsx.diagnosiskeydist.apispec.ZIPSubmissionPeriod;
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExposureProtobufTest {

    private static final String BUNDLE_ID = "some-bundle-id";
    private static final String SIG = "some-sig";

    private final ExposureProtobuf exposureProtobuf = new ExposureProtobuf(BUNDLE_ID);
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
            new StoredTemporaryExposureKey("W2zb3BeMWt6Xr2u0ABG32Q==", 12345, 144, 7),
            new StoredTemporaryExposureKey("kzQt9Lf3xjtAlMtm7jkSqw==", 12499, 144, 7, 4)
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
        ZIPSubmissionPeriod period = DailyZIPSubmissionPeriod.periodForSubmissionDate(new Date());
        Exposure.TemporaryExposureKeyExport tekExport = exposureProtobuf.buildTemporaryExposureKeyExport(storedKeys, period, 0);

        assertThat(tekExport.getKeysList()).hasSize(2);
        assertThat(tekExport.getKeysCount()).isEqualTo(2);
    }

    @Test
    public void exportKeysHaveSameData() {
        ZIPSubmissionPeriod period = DailyZIPSubmissionPeriod.periodForSubmissionDate(new Date());
        Exposure.TemporaryExposureKeyExport tekExport = exposureProtobuf.buildTemporaryExposureKeyExport(storedKeys, period, 0);

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
                .setDaysSinceOnsetOfSymptoms(4)
                .build();

        assertThat(exposureKeys).isEqualTo(asList(expectedKey1, expectedKey2));
    }

    @Test
    public void exportHasBatchNumAndSize() {
        ZIPSubmissionPeriod period = DailyZIPSubmissionPeriod.periodForSubmissionDate(new Date());
        Exposure.TemporaryExposureKeyExport tekExport = exposureProtobuf.buildTemporaryExposureKeyExport(storedKeys, period, 0);

        assertThat(tekExport.getBatchNum()).isEqualTo(1);
        assertThat(tekExport.getBatchSize()).isEqualTo(1);
    }

    @Test
    public void exportHasDefaultRegion() {
        ZIPSubmissionPeriod period = DailyZIPSubmissionPeriod.periodForSubmissionDate(new Date());
        Exposure.TemporaryExposureKeyExport tekExport = exposureProtobuf.buildTemporaryExposureKeyExport(storedKeys, period, 0);

        assertThat(tekExport.getRegion()).isEmpty();
    }

    @Test
    public void exportHasRightSignatureInfosCount() {
        ZIPSubmissionPeriod period = DailyZIPSubmissionPeriod.periodForSubmissionDate(new Date());
        Exposure.TemporaryExposureKeyExport tekExport = exposureProtobuf.buildTemporaryExposureKeyExport(storedKeys, period, 0);

        assertThat(tekExport.getSignatureInfosCount()).isEqualTo(1);
        assertThat(tekExport.getSignatureInfosList()).hasSize(1);
    }

    @Test
    public void exportHasRightSignatureInfosContent() {
        ZIPSubmissionPeriod period = DailyZIPSubmissionPeriod.periodForSubmissionDate(new Date());
        Exposure.TemporaryExposureKeyExport tekExport = exposureProtobuf.buildTemporaryExposureKeyExport(storedKeys, period, 0);

        assertThat(tekExport.getSignatureInfos(0)).isEqualTo(expectedSignatureInfo);
    }

    @Test
    public void exportTimeStamps() {
        ZIPSubmissionPeriod period = DailyZIPSubmissionPeriod.periodForSubmissionDate(new Date());
        Exposure.TemporaryExposureKeyExport tekExport = exposureProtobuf.buildTemporaryExposureKeyExport(storedKeys, period, 0);

        assertThat(tekExport.getStartTimestamp()).isEqualTo(period.getStartInclusive().getTime() / 1000);
        assertThat(tekExport.getEndTimestamp()).isEqualTo(period.getEndExclusive().getTime() / 1000);
    }

    @Test
    public void exportDoesNotContainDaysSinceOnsetOfSymptomsForV1() {
        ZIPSubmissionPeriod period = DailyZIPSubmissionPeriod.periodForSubmissionDate(new Date());
        Exposure.TemporaryExposureKeyExport tekExport = exposureProtobuf.buildTemporaryExposureKeyExport(storedKeys, period, 0);
        List<Exposure.TemporaryExposureKey> exposureKeys = tekExport.getKeysList();
        assertFalse(exposureKeys.get(0).hasDaysSinceOnsetOfSymptoms());
    }

    @Test
    public void exportContainsDaysSinceOnsetOfSymptomsForV2() {
        ZIPSubmissionPeriod period = DailyZIPSubmissionPeriod.periodForSubmissionDate(new Date());
        Exposure.TemporaryExposureKeyExport tekExport = exposureProtobuf.buildTemporaryExposureKeyExport(storedKeys, period, 0);
        List<Exposure.TemporaryExposureKey> exposureKeys = tekExport.getKeysList();
        assertTrue(exposureKeys.get(1).hasDaysSinceOnsetOfSymptoms());
    }
}