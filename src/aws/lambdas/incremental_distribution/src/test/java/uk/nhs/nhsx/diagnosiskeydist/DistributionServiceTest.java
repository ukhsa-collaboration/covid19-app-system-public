package uk.nhs.nhsx.diagnosiskeydist;

import batchZipCreation.Exposure;
import com.amazonaws.services.kms.model.SigningAlgorithmSpec;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import uk.nhs.nhsx.BatchExport;
import uk.nhs.nhsx.core.SystemClock;
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

import java.io.File;
import java.io.FilenameFilter;
import java.util.*;
import java.util.stream.IntStream;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

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

    @Rule
    public TemporaryFolder distributionFolder = new TemporaryFolder();

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
    public void shouldAbortOutsideServiceWindow() {
        assertThatThrownBy(() -> {
            Date date = utcDate(2020, 7, 14, 19, 30, 0, 0);
            new DistributionService(
                new MockSubmissionRepository(emptyList()),
                exposureProtobuf,
                new SaveToFileKeyDistributor(distributionFolder.getRoot()),
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
    public void shouldNotAbortIfFlagIsFalse() throws Exception {
        Date date = utcDate(2020, 7, 14, 19, 30, 0, 0);
        new DistributionService(
            new MockSubmissionRepository(emptyList()),
            exposureProtobuf,
            new SaveToFileKeyDistributor(distributionFolder.getRoot()),
            signer,
            awsCloudFront,
            awsS3,
            new BatchProcessingConfig(false, BucketName.of("dist-zip-bucket-name"), "", "", "", ParameterName.of(""), ParameterName.of(""))
        ).distributeKeys(date);

        assertDailyExportBatchExists();
    }

    @Test
    public void distributeKeysFromSubmissionsOnSingleDay() throws Exception {
        Date date = utcDate(2020, 7, 16, 7, 46, 0, 0);
        Date dateBefore = utcDate(2020, 7, 15, 7, 46, 0, 0);

        new DistributionService(
            new MockSubmissionRepository(Arrays.asList(date, dateBefore)),
            exposureProtobuf,
            new SaveToFileKeyDistributor(distributionFolder.getRoot()),
            signer,
            awsCloudFront,
            awsS3,
            batchProcessingConfig
        ).distributeKeys(date);

        assertDailyExportBatchExists();
        assertTwoHourlyExportBatchExists();

        File latestDailyZipFile = new File(distributionFolder.getRoot(), "distribution/daily/2020071600.zip");
        List<Exposure.TemporaryExposureKey> keys = BatchExport.INSTANCE.tekListFromZipFile(latestDailyZipFile);
        assertEquals("keys in latest daily zip file", 14, keys.size());

        File earlierDailyZipFile = new File(distributionFolder.getRoot(), "distribution/daily/2020071500.zip");
        keys = BatchExport.INSTANCE.tekListFromZipFile(earlierDailyZipFile);
        assertEquals("keys in earlier daily zip file", 0, keys.size());
    }

    @Test
    public void distributeKeysFromSubmissionsOnMultipleDays() throws Exception {
        Date date = utcDate(2020, 7, 16, 7, 46, 0, 0);
        Date dateBefore = utcDate(2020, 7, 15, 7, 46, 0, 0);
        Date dateBeforeThat = utcDate(2020, 7, 14, 7, 46, 0, 0);
        Date date3DaysBefore = utcDate(2020, 7, 13, 7, 46, 0, 0);

        new DistributionService(
            new MockSubmissionRepository(Arrays.asList(date, dateBefore, dateBeforeThat, date3DaysBefore)),
            exposureProtobuf,
            new SaveToFileKeyDistributor(distributionFolder.getRoot()),
            signer,
            awsCloudFront,
            awsS3,
            batchProcessingConfig
        ).distributeKeys(date);

        assertDailyExportBatchExists();
        assertTwoHourlyExportBatchExists();

        File latestDailyZipFile = new File(distributionFolder.getRoot(), "distribution/daily/2020071600.zip");
        List<Exposure.TemporaryExposureKey> keys = BatchExport.INSTANCE.tekListFromZipFile(latestDailyZipFile);
        assertEquals("keys in latest daily zip file", 14, keys.size());

        File earlierDailyZipFile = new File(distributionFolder.getRoot(), "distribution/daily/2020071500.zip");
        keys = BatchExport.INSTANCE.tekListFromZipFile(earlierDailyZipFile);
        assertEquals("keys in earlier daily zip file", 13, keys.size());

        earlierDailyZipFile = new File(distributionFolder.getRoot(), "distribution/daily/2020071400.zip");
        keys = BatchExport.INSTANCE.tekListFromZipFile(earlierDailyZipFile);
        assertEquals("keys in even earlier daily zip file", 12, keys.size());
    }

    @Test
    public void tekExportHasCorrectSignatureInfo() throws Exception {
        Date date = utcDate(2020, 7, 16, 7, 46, 0, 0);

        new DistributionService(
            new MockSubmissionRepository(singletonList(date)),
            exposureProtobuf,
            new SaveToFileKeyDistributor(distributionFolder.getRoot()),
            signer,
            awsCloudFront,
            awsS3,
            batchProcessingConfig
        ).distributeKeys(date);

        File latestDailyZipFile = new File(distributionFolder.getRoot(), "distribution/daily/2020071600.zip");
        Exposure.TemporaryExposureKeyExport temporaryExposureKeyExport = BatchExport.INSTANCE.tekExportFromZipFile(latestDailyZipFile);
        assertThat(temporaryExposureKeyExport.getSignatureInfosList()).hasSize(1);
        assertThat(temporaryExposureKeyExport.getSignatureInfosList().get(0)).isEqualTo(expectedSignatureInfo);
    }

    @Test
    public void tekSignatureListHasCorrectSignatureInfo() throws Exception {
        Date date = utcDate(2020, 7, 16, 7, 46, 0, 0);

        new DistributionService(
            new MockSubmissionRepository(singletonList(date)),
            exposureProtobuf,
            new SaveToFileKeyDistributor(distributionFolder.getRoot()),
            signer,
            awsCloudFront,
            awsS3,
            batchProcessingConfig
        ).distributeKeys(date);

        File latestDailyZipFile = new File(distributionFolder.getRoot(), "distribution/daily/2020071600.zip");
        Exposure.TEKSignatureList tekSignatureList = BatchExport.INSTANCE.tekSignatureListFromZipFile(latestDailyZipFile);
        assertThat(tekSignatureList.getSignaturesList()).hasSize(1);
        assertThat(tekSignatureList.getSignaturesList().get(0).getSignatureInfo()).isEqualTo(expectedSignatureInfo);
    }

    @SuppressWarnings("serial")
	@Test
    public void deletesOldObjectsThatDontMatchUploaded() throws Exception {
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
            new SaveToFileKeyDistributor(distributionFolder.getRoot()),
            signer,
            awsCloudFront,
            awsS3,
            batchProcessingConfig
        ).distributeKeys(date);

        assertThat(awsS3.deleted).contains(new AbstractMap.SimpleEntry<>(batchProcessingConfig.zipBucketName, notMatchedObjectKey));
    }

    @SuppressWarnings("serial")
	@Test
    public void noDeletionIfObjectKeyMatchesUploaded() throws Exception {
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
            new SaveToFileKeyDistributor(distributionFolder.getRoot()),
            signer,
            awsCloudFront,
            awsS3,
            batchProcessingConfig
        ).distributeKeys(date);

        assertThat(awsS3.deleted).hasSize(0);
    }

    @Test
    public void invalidatesCloudFrontCaches() throws Exception {
        Date date = utcDate(2020, 7, 16, 7, 46, 0, 0);

        new DistributionService(
            new MockSubmissionRepository(singletonList(date)),
            exposureProtobuf,
            new SaveToFileKeyDistributor(distributionFolder.getRoot()),
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
    public void checkDailyBatchExistsAtMidnightBoundary() throws Exception {
        Date date = utcDate(2020, 9, 16, 23, 47, 0, 0);

        new DistributionService(
            new MockSubmissionRepository(singletonList(date)),
            exposureProtobuf,
            new SaveToFileKeyDistributor(distributionFolder.getRoot()),
            signer,
            awsCloudFront,
            awsS3,
            batchProcessingConfig
        ).distributeKeys(date);
        File distributionDailyDir = new File(distributionFolder.getRoot(), "distribution/daily");
        assertTrue(new File(distributionDailyDir.getPath().concat("/2020091700.zip")).exists());
        assertDailyExportBatchExists();
    }
    private void assertDailyExportBatchExists() {
        File distributionDailyDir = new File(distributionFolder.getRoot(), "distribution/daily");
        assertTrue(distributionDailyDir.exists());
        assertEquals(15, distributionDailyDir.list().length);
    }

    private void assertTwoHourlyExportBatchExists() {
        File distributionTwoHourlyDir = new File(distributionFolder.getRoot(), "distribution/two-hourly");
        assertTrue(distributionTwoHourlyDir.exists());
        assertEquals(168, distributionTwoHourlyDir.list().length);
    }

    static class MockSubmissionRepository implements SubmissionRepository {

        private final List<Submission> submissions = new ArrayList<>();

        MockSubmissionRepository(List<Date> submissionDates) {
            submissionDates.forEach(it -> submissions.add(makeKeySet(it)));
        }

        @Override
        public List<Submission> loadAllSubmissions() {
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
