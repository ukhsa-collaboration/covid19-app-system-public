package uk.nhs.nhsx.diagnosiskeydist;

import batchZipCreation.Exposure;
import com.amazonaws.services.kms.model.SigningAlgorithmSpec;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import uk.nhs.nhsx.testhelper.BatchExport;
import uk.nhs.nhsx.core.aws.cloudfront.AwsCloudFront;
import uk.nhs.nhsx.core.aws.s3.BucketName;
import uk.nhs.nhsx.core.aws.s3.ObjectKey;
import uk.nhs.nhsx.core.aws.ssm.ParameterName;
import uk.nhs.nhsx.core.signature.KeyId;
import uk.nhs.nhsx.core.signature.Signature;
import uk.nhs.nhsx.core.signature.Signer;
import uk.nhs.nhsx.diagnosiskeydist.agspec.ENIntervalNumber;
import uk.nhs.nhsx.diagnosiskeydist.keydistribution.SaveToFileKeyDistributor;
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey;
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKeyPayload;
import uk.nhs.nhsx.testhelper.mocks.FakeS3;

import java.io.File;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.IntStream;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class DistributionServiceTest {

    private static final String mobileAppBundleId = "uk.nhs.covid19.internal";

    private static final Exposure.SignatureInfo expectedSignatureInfo = Exposure.SignatureInfo.newBuilder()
        .setAndroidPackage(mobileAppBundleId)
        .setAppBundleId(mobileAppBundleId)
        .setVerificationKeyVersion("v1")
        .setVerificationKeyId("234")
        .setSignatureAlgorithm("1.2.840.10045.4.3.2")
        .build();

    private final FakeS3 awsS3 = new FakeS3();
    private final AwsCloudFront awsCloudFront = mock(AwsCloudFront.class);
    private final ExposureProtobuf exposureProtobuf = new ExposureProtobuf(mobileAppBundleId);
    private final BatchProcessingConfig batchProcessingConfig = new BatchProcessingConfig(
        true,
        BucketName.of("dist-zip-bucket-name"),
        "dis-id",
        "dist-pattern-daily",
        "dist-pattern-2hourly",
        ParameterName.of("ssmKeyIdParameterName"),
        ParameterName.of("ssmContentKeyIdParameterName"));

    private final Signer signer = (c) -> new Signature(KeyId.of("key-id"), SigningAlgorithmSpec.ECDSA_SHA_256, new byte[]{1, 2, 3});

    private Date utcDate(int year, int month, int day, int hour, int minute, int second, int millis) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month - 1);
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, second);
        cal.set(Calendar.MILLISECOND, millis);

        return cal.getTime();
    }

    @Test
    public void shouldAbortOutsideServiceWindow(@TempDir Path distributionFolder) {
        assertThatThrownBy(() -> {
            Date date = utcDate(2020, 7, 14, 19, 30, 0, 0);
            new DistributionService(
                new MockSubmissionRepository(emptyList()),
                exposureProtobuf,
                new SaveToFileKeyDistributor(distributionFolder.toFile()),
                signer,
                awsCloudFront,
                awsS3,
                batchProcessingConfig
            ).distributeKeys(date);
        })
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("CloudWatch Event triggered Lambda at wrong time.");
    }

    @Test
    public void shouldNotAbortIfFlagIsFalse(@TempDir Path distributionFolder) throws Exception {
        Date date = utcDate(2020, 7, 14, 19, 30, 0, 0);
        new DistributionService(
            new MockSubmissionRepository(emptyList()),
            exposureProtobuf,
            new SaveToFileKeyDistributor(distributionFolder.toFile()),
            signer,
            awsCloudFront,
            awsS3,
            new BatchProcessingConfig(false, BucketName.of("dist-zip-bucket-name"), "", "", "", ParameterName.of(""), ParameterName.of(""))
        ).distributeKeys(date);

        assertDailyExportBatchExists(distributionFolder);
    }

    @Test
    public void distributeKeysFromSubmissionsOnSingleDay(@TempDir Path distributionFolder) throws Exception {
        Date date = utcDate(2020, 7, 16, 7, 46, 0, 0);
        Date dateBefore = utcDate(2020, 7, 15, 7, 46, 0, 0);

        new DistributionService(
            new MockSubmissionRepository(Arrays.asList(date, dateBefore)),
            exposureProtobuf,
            new SaveToFileKeyDistributor(distributionFolder.toFile()),
            signer,
            awsCloudFront,
            awsS3,
            batchProcessingConfig
        ).distributeKeys(date);

        assertDailyExportBatchExists(distributionFolder);
        assertTwoHourlyExportBatchExists(distributionFolder);

        File latestDailyZipFile = new File(distributionFolder.toFile(), "distribution/daily/2020071600.zip");
        List<Exposure.TemporaryExposureKey> keys = BatchExport.INSTANCE.tekListFromZipFile(latestDailyZipFile);
        assertEquals(14, keys.size(), "keys in latest daily zip file");

        File earlierDailyZipFile = new File(distributionFolder.toFile(), "distribution/daily/2020071500.zip");
        keys = BatchExport.INSTANCE.tekListFromZipFile(earlierDailyZipFile);
        assertEquals(0, keys.size(), "keys in earlier daily zip file");
    }

    @Test
    public void distributeKeysFromSubmissionsOnMultipleDays(@TempDir Path distributionFolder) throws Exception {
        Date date = utcDate(2020, 7, 16, 7, 46, 0, 0);
        Date dateBefore = utcDate(2020, 7, 15, 7, 46, 0, 0);
        Date dateBeforeThat = utcDate(2020, 7, 14, 7, 46, 0, 0);
        Date date3DaysBefore = utcDate(2020, 7, 13, 7, 46, 0, 0);

        new DistributionService(
            new MockSubmissionRepository(Arrays.asList(date, dateBefore, dateBeforeThat, date3DaysBefore)),
            exposureProtobuf,
            new SaveToFileKeyDistributor(distributionFolder.toFile()),
            signer,
            awsCloudFront,
            awsS3,
            batchProcessingConfig
        ).distributeKeys(date);

        assertDailyExportBatchExists(distributionFolder);
        assertTwoHourlyExportBatchExists(distributionFolder);

        File latestDailyZipFile = new File(distributionFolder.toFile(), "distribution/daily/2020071600.zip");
        List<Exposure.TemporaryExposureKey> keys = BatchExport.INSTANCE.tekListFromZipFile(latestDailyZipFile);
        assertEquals(14, keys.size(), "keys in latest daily zip file");

        File earlierDailyZipFile = new File(distributionFolder.toFile(), "distribution/daily/2020071500.zip");
        keys = BatchExport.INSTANCE.tekListFromZipFile(earlierDailyZipFile);
        assertEquals(13, keys.size(), "keys in earlier daily zip file");

        earlierDailyZipFile = new File(distributionFolder.toFile(), "distribution/daily/2020071400.zip");
        keys = BatchExport.INSTANCE.tekListFromZipFile(earlierDailyZipFile);
        assertEquals(12, keys.size(), "keys in even earlier daily zip file");
    }

    @Test
    public void tekExportHasCorrectSignatureInfo(@TempDir Path distributionFolder) throws Exception {
        Date date = utcDate(2020, 7, 16, 7, 46, 0, 0);

        new DistributionService(
            new MockSubmissionRepository(singletonList(date)),
            exposureProtobuf,
            new SaveToFileKeyDistributor(distributionFolder.toFile()),
            signer,
            awsCloudFront,
            awsS3,
            batchProcessingConfig
        ).distributeKeys(date);

        File latestDailyZipFile = new File(distributionFolder.toFile(), "distribution/daily/2020071600.zip");
        Exposure.TemporaryExposureKeyExport temporaryExposureKeyExport = BatchExport.INSTANCE.tekExportFromZipFile(latestDailyZipFile);
        assertThat(temporaryExposureKeyExport.getSignatureInfosList()).hasSize(1);
        assertThat(temporaryExposureKeyExport.getSignatureInfosList().get(0)).isEqualTo(expectedSignatureInfo);
    }

    @Test
    public void tekSignatureListHasCorrectSignatureInfo(@TempDir Path distributionFolder) throws Exception {
        Date date = utcDate(2020, 7, 16, 7, 46, 0, 0);

        new DistributionService(
            new MockSubmissionRepository(singletonList(date)),
            exposureProtobuf,
            new SaveToFileKeyDistributor(distributionFolder.toFile()),
            signer,
            awsCloudFront,
            awsS3,
            batchProcessingConfig
        ).distributeKeys(date);

        File latestDailyZipFile = new File(distributionFolder.toFile(), "distribution/daily/2020071600.zip");
        Exposure.TEKSignatureList tekSignatureList = BatchExport.INSTANCE.tekSignatureListFromZipFile(latestDailyZipFile);
        assertThat(tekSignatureList.getSignaturesList()).hasSize(1);
        assertThat(tekSignatureList.getSignaturesList().get(0).getSignatureInfo()).isEqualTo(expectedSignatureInfo);
    }

    @SuppressWarnings("serial")
	@Test
    public void deletesOldObjectsThatDontMatchUploaded(@TempDir Path distributionFolder) throws Exception {
        Date date = utcDate(2020, 7, 16, 7, 46, 0, 0);
        ObjectKey notMatchedObjectKey = ObjectKey.of("obj-key-not-uploaded");

        awsS3.existing.add(
            new S3ObjectSummary() {{
                setBucketName(batchProcessingConfig.zipBucketName.value);
                setKey(notMatchedObjectKey.value);
            }}
        );

        new DistributionService(
            new MockSubmissionRepository(singletonList(date)),
            exposureProtobuf,
            new SaveToFileKeyDistributor(distributionFolder.toFile()),
            signer,
            awsCloudFront,
            awsS3,
            batchProcessingConfig
        ).distributeKeys(date);

        assertThat(awsS3.deleted).contains(new AbstractMap.SimpleEntry<>(batchProcessingConfig.zipBucketName, notMatchedObjectKey));
    }

    @SuppressWarnings("serial")
	@Test
    public void noDeletionIfObjectKeyMatchesUploaded(@TempDir Path distributionFolder) throws Exception {
        Date date = utcDate(2020, 7, 16, 7, 46, 0, 0);
        ObjectKey matchedObjectKey = ObjectKey.of("distribution/daily/2020070300.zip");

        awsS3.existing.add(
            new S3ObjectSummary() {{
                setBucketName(batchProcessingConfig.zipBucketName.value);
                setKey(matchedObjectKey.value);
            }}
        );

        new DistributionService(
            new MockSubmissionRepository(singletonList(date)),
            exposureProtobuf,
            new SaveToFileKeyDistributor(distributionFolder.toFile()),
            signer,
            awsCloudFront,
            awsS3,
            batchProcessingConfig
        ).distributeKeys(date);

        assertThat(awsS3.deleted).hasSize(0);
    }

    @Test
    public void invalidatesCloudFrontCaches(@TempDir Path distributionFolder) throws Exception {
        Date date = utcDate(2020, 7, 16, 7, 46, 0, 0);

        new DistributionService(
            new MockSubmissionRepository(singletonList(date)),
            exposureProtobuf,
            new SaveToFileKeyDistributor(distributionFolder.toFile()),
            signer,
            awsCloudFront,
            awsS3,
            batchProcessingConfig
        ).distributeKeys(date);

        verify(awsCloudFront, times(1))
            .invalidateCache("dis-id", "dist-pattern-daily");
        verify(awsCloudFront, times(1))
            .invalidateCache("dis-id", "dist-pattern-2hourly");
    }
    @Test
    public void checkDailyBatchExistsAtMidnightBoundary(@TempDir Path distributionFolder) throws Exception {
        Date date = utcDate(2020, 9, 16, 23, 47, 0, 0);

        new DistributionService(
            new MockSubmissionRepository(singletonList(date)),
            exposureProtobuf,
            new SaveToFileKeyDistributor(distributionFolder.toFile()),
            signer,
            awsCloudFront,
            awsS3,
            batchProcessingConfig
        ).distributeKeys(date);
        File distributionDailyDir = new File(distributionFolder.toFile(), "distribution/daily");
        assertTrue(new File(distributionDailyDir.getPath().concat("/2020091700.zip")).exists());
        assertDailyExportBatchExists(distributionFolder);
    }
    private void assertDailyExportBatchExists(Path distributionFolder) {
        File distributionDailyDir = new File(distributionFolder.toFile(), "distribution/daily");
        assertTrue(distributionDailyDir.exists());
        assertEquals(15, distributionDailyDir.list().length);
    }

    private void assertTwoHourlyExportBatchExists(Path distributionFolder) {
        File distributionTwoHourlyDir = new File(distributionFolder.toFile(), "distribution/two-hourly");
        assertTrue(distributionTwoHourlyDir.exists());
        assertEquals(168, distributionTwoHourlyDir.list().length);
    }

    static class MockSubmissionRepository implements SubmissionRepository {

        private final List<Submission> submissions = new ArrayList<>();

        MockSubmissionRepository(List<Date> submissionDates) {
            submissionDates.forEach(it -> submissions.add(makeKeySet(it)));
        }

        @Override
        public List<Submission> loadAllSubmissions(long minimalSubmissionTimeEpocMillisExclusive, int maxLimit, int maxResults) {
            return submissions;
        }

        private Submission makeKeySet(Date submissionDate) {
            long mostRecentKeyRollingStart = (ENIntervalNumber.enIntervalNumberFromTimestamp(submissionDate).getEnIntervalNumber() / 144) * 144;
            List<StoredTemporaryExposureKey> keys = IntStream.range(0, 14)
                .mapToObj(i -> makeKey(mostRecentKeyRollingStart - i * 144))
                .collect(toList());
            return new Submission(submissionDate, new StoredTemporaryExposureKeyPayload(keys));
        }

        private static StoredTemporaryExposureKey makeKey(long keyStartTime) {
            return new StoredTemporaryExposureKey("ABC", Math.toIntExact(keyStartTime), 144, 7);
        }
    }

}
