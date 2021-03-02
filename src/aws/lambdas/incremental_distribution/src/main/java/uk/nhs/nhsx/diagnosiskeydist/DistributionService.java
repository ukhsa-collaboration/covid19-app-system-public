package uk.nhs.nhsx.diagnosiskeydist;

import com.amazonaws.services.s3.model.S3ObjectSummary;
import uk.nhs.nhsx.core.aws.cloudfront.AwsCloudFront;
import uk.nhs.nhsx.core.aws.s3.AwsS3;
import uk.nhs.nhsx.core.aws.s3.BucketName;
import uk.nhs.nhsx.core.aws.s3.Locator;
import uk.nhs.nhsx.core.aws.s3.ObjectKey;
import uk.nhs.nhsx.core.events.Events;
import uk.nhs.nhsx.core.signature.Signature;
import uk.nhs.nhsx.core.signature.Signer;
import uk.nhs.nhsx.diagnosiskeydist.agspec.ENIntervalNumber;
import uk.nhs.nhsx.diagnosiskeydist.apispec.DailyZIPSubmissionPeriod;
import uk.nhs.nhsx.diagnosiskeydist.apispec.TwoHourlyZIPSubmissionPeriod;
import uk.nhs.nhsx.diagnosiskeydist.apispec.ZIPSubmissionPeriod;
import uk.nhs.nhsx.diagnosiskeydist.keydistribution.KeyDistributor;
import uk.nhs.nhsx.diagnosiskeydist.keydistribution.KeyFileUtility;
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static batchZipCreation.Exposure.TEKSignatureList;
import static batchZipCreation.Exposure.TemporaryExposureKeyExport;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.shuffle;
import static uk.nhs.nhsx.diagnosiskeydist.DistributionServiceWindow.ZIP_SUBMISSION_PERIOD_OFFSET;

/**
 * Batch job to generate and upload daily and two-hourly Diagnosis Key Distribution ZIPs every two hours during a 15' window
 */
public class DistributionService {

    private static final int MAXIMAL_ZIP_SIGN_S3_PUT_TIME_MINUTES = 6;

    static final String EK_EXPORT_V1_HEADER = "EK Export v1    ";

    private final SubmissionRepository submissionRepository;

    private final ExposureProtobuf exposureProtobuf;

    private final KeyDistributor keyDistributor;

    private final Signer signer;
    private final AwsCloudFront awsCloudFront;
    private final AwsS3 awsS3;
    private final BatchProcessingConfig config;
    private final Events events;

    private final List<String> uploadedZipFileNames = Collections.synchronizedList(new ArrayList<>());

    DistributionService(SubmissionRepository submissionRepository,
                        ExposureProtobuf exposureProtobuf,
                        KeyDistributor keyDistributor,
                        Signer signer,
                        AwsCloudFront awsCloudFront,
                        AwsS3 awsS3,
                        BatchProcessingConfig config,
                        Events events) {
        this.submissionRepository = submissionRepository;
        this.exposureProtobuf = exposureProtobuf;
        this.keyDistributor = keyDistributor;
        this.signer = signer;
        this.awsCloudFront = awsCloudFront;
        this.awsS3 = awsS3;
        this.config = config;
        this.events = events;
    }

    public void distributeKeys(Instant now) throws Exception {
        DistributionServiceWindow window = new DistributionServiceWindow(now);

        events.emit(getClass(), new DistributionBatchWindow(
            now,
            window.earliestBatchStartDateWithinHourInclusive(),
            window.latestBatchStartDateWithinHourExclusive()
        ));

        if (!window.isValidBatchStartDate()) {
            if (config.shouldAbortOutsideTimeWindow) {
                throw new IllegalStateException("CloudWatch Event triggered Lambda at wrong time.");
            }
        }

        List<Submission> allSubmissions = submissionRepository.loadAllSubmissions();

        DailyZIPSubmissionPeriod daily = DailyZIPSubmissionPeriod.periodForSubmissionDate(now);
        TwoHourlyZIPSubmissionPeriod twoHourly = TwoHourlyZIPSubmissionPeriod.periodForSubmissionDate(now);
        for (ZIPSubmissionPeriod lastZipPeriod : List.of(daily, twoHourly)) {
            try (ConcurrentExecution pool = new ConcurrentExecution("Distribution: " + lastZipPeriod.getClass().getSimpleName(), Duration.ofMinutes(MAXIMAL_ZIP_SIGN_S3_PUT_TIME_MINUTES), events)) {
                for (ZIPSubmissionPeriod zipPeriod : lastZipPeriod.allPeriodsToGenerate()) {
                    pool.execute(() -> distributeKeys(allSubmissions, window, zipPeriod));
                }
            }
        }

        removeUnmodifiedObjectsFromDistributionBucket(config.zipBucketName);
        invalidateCloudFrontCaches();
    }

    private void removeUnmodifiedObjectsFromDistributionBucket(BucketName bucketName) {
        List<S3ObjectSummary> distributionObjectSummaries = awsS3.getObjectSummaries(bucketName);
        for (S3ObjectSummary s3ObjectSummary : distributionObjectSummaries) {
            if (!uploadedZipFileNames.contains(s3ObjectSummary.getKey())) {
                awsS3.deleteObject(Locator.of(bucketName, ObjectKey.of(s3ObjectSummary.getKey())));
            }
        }
    }

    private void invalidateCloudFrontCaches() {
        awsCloudFront.invalidateCache(
            config.cloudFrontDistributionId, config.distributionPatternDaily
        );
        awsCloudFront.invalidateCache(
            config.cloudFrontDistributionId, config.distributionPattern2Hourly
        );
    }

    private void distributeKeys(List<Submission> submissions,
                                DistributionServiceWindow window,
                                ZIPSubmissionPeriod zipPeriod) throws IOException, NoSuchAlgorithmException {

        List<StoredTemporaryExposureKey> temporaryExposureKeys = validKeysFromSubmissions(submissions, window, zipPeriod);

        File binFile = File.createTempFile("export", ".bin");
        File sigFile = File.createTempFile("export", ".sig");

        try {
            byte[] binFileContent = generateExportFileContentFrom(temporaryExposureKeys, zipPeriod, ZIP_SUBMISSION_PERIOD_OFFSET);
            KeyFileUtility.writeToFile(binFile, binFileContent);

            byte[] sigFileContent = generateSigFileContentFrom(binFileContent);
            KeyFileUtility.writeToFile(sigFile, sigFileContent);

            String objectName = zipPeriod.zipPath();
            keyDistributor.distribute(
                config.zipBucketName,
                ObjectKey.of(objectName),
                binFile,
                sigFile
            );
            uploadedZipFileNames.add(objectName);
        } finally {
            List.of(binFile, sigFile).forEach(File::delete);
        }
    }

    private List<StoredTemporaryExposureKey> validKeysFromSubmissions(List<Submission> submissions, DistributionServiceWindow window, ZIPSubmissionPeriod zipPeriod) {
        List<StoredTemporaryExposureKey> temporaryExposureKeys = new ArrayList<>();
        for (Submission submission : submissions) {
            if (zipPeriod.isCoveringSubmissionDate(submission.submissionDate.toInstant(), ZIP_SUBMISSION_PERIOD_OFFSET)) {
                for (StoredTemporaryExposureKey key : submission.payload.temporaryExposureKeys) {
                    ENIntervalNumber keyIntervalNumber = new ENIntervalNumber(key.rollingStartNumber);

                    if (keyIntervalNumber.validUntil(window.zipExpirationExclusive())) {
                        temporaryExposureKeys.add(key);
                    }
                }
            }
        }

        // Important: the keys must not be distributed in submission order for privacy reasons
        shuffle(temporaryExposureKeys);

        return temporaryExposureKeys;
    }

    @SuppressWarnings("SameParameterValue")
    private byte[] generateExportFileContentFrom(List<StoredTemporaryExposureKey> temporaryExposureKeys,
                                                 ZIPSubmissionPeriod period,
                                                 Duration periodOffset) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        bout.write(EK_EXPORT_V1_HEADER.getBytes(UTF_8));
        TemporaryExposureKeyExport export = exposureProtobuf.buildTemporaryExposureKeyExport(temporaryExposureKeys, period, periodOffset);
        bout.write(export.toByteArray());
        return bout.toByteArray();
    }

    private byte[] generateSigFileContentFrom(byte[] binFileContent) {
        Signature signature = signer.sign(binFileContent);
        TEKSignatureList tekSignatureList = exposureProtobuf.buildTEKSignatureList(signature.asByteBuffer());
        return tekSignatureList.toByteArray();
    }
}
