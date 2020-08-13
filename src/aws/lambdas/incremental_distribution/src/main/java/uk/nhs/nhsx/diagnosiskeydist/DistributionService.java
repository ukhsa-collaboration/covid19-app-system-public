package uk.nhs.nhsx.diagnosiskeydist;

import batchZipCreation.Exposure;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.nhs.nhsx.core.aws.cloudfront.AwsCloudFront;
import uk.nhs.nhsx.core.aws.s3.AwsS3;
import uk.nhs.nhsx.core.aws.s3.BucketName;
import uk.nhs.nhsx.core.aws.s3.ObjectKey;
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
import java.util.*;

/**
 * Batch job to generate and upload daily and two-hourly Diagnosis Key Distribution ZIPs every two hours during a 15' window
 */
public class DistributionService {
	private static final Logger logger = LoggerFactory.getLogger(DistributionService.class);

	private static final int MAXIMAL_ZIP_SIGN_S3_PUT_TIME_MINUTES = 6;

	private static final String EK_EXPORT_V1_HEADER = "EK Export v1    ";

	private final SubmissionRepository submissionRepository;

	private final ExposureProtobuf exposureProtobuf;

	private final KeyDistributor keyDistributor;

	private final Signer signer;
	private final AwsCloudFront awsCloudFront;
	private final AwsS3 awsS3;
	private final BatchProcessingConfig config;

	private final List<String> uploadedZipFileNames = Collections.synchronizedList(new ArrayList<>());

	DistributionService(SubmissionRepository submissionRepository,
						ExposureProtobuf exposureProtobuf,
						KeyDistributor keyDistributor,
						Signer signer,
						AwsCloudFront awsCloudFront,
						AwsS3 awsS3,
						BatchProcessingConfig config) {
		this.submissionRepository = submissionRepository;
		this.exposureProtobuf = exposureProtobuf;
		this.keyDistributor = keyDistributor;
		this.signer = signer;
		this.awsCloudFront = awsCloudFront;
		this.awsS3 = awsS3;
		this.config = config;
	}

	public void distributeKeys(Date now) throws Exception {
		DistributionServiceWindow window = new DistributionServiceWindow(now);

		logger.info("Batch run triggered: now={}, earliest start={} (inclusive), lastest start={} (exclusive)", now, window.earliestBatchStartDateWithinHourInclusive(), window.latestBatchStartDateWithinHourExclusive());

		if (!window.validBatchStartDate()) {
			logger.error("CloudWatch Event triggered Lambda at wrong time.");

			if (config.shouldAbortOutsideTimeWindow) {
				throw new IllegalStateException("CloudWatch Event triggered Lambda at wrong time.");
			}
		}

		List<Submission> allSubmissions = submissionRepository.loadAllSubmissions();

		for (ZIPSubmissionPeriod lastZipPeriod : Arrays.asList(
				DailyZIPSubmissionPeriod.periodForSubmissionDate(now),
				TwoHourlyZIPSubmissionPeriod.periodForSubmissionDate(now))) {

			try (ConcurrentExecution pool = new ConcurrentExecution("Distribution: " + lastZipPeriod.getClass().getSimpleName(), MAXIMAL_ZIP_SIGN_S3_PUT_TIME_MINUTES)) {
				for (ZIPSubmissionPeriod zipPeriod : lastZipPeriod.allPeriodsToGenerate()) {
					pool.execute(() -> distributeKeys(allSubmissions, window, zipPeriod));
				}
			}
		}

		removeUnmodifiedObjectsFromDistributionBucket(config.zipBucketName);
		invalidateCloudFrontCaches();
	}

	private void removeUnmodifiedObjectsFromDistributionBucket(BucketName bucketName) {
		List<S3ObjectSummary> distributionObjectSummaries = awsS3.getObjectSummaries(bucketName.value);
		for (S3ObjectSummary s3ObjectSummary : distributionObjectSummaries) {
			if (!uploadedZipFileNames.contains(s3ObjectSummary.getKey())) {
				logger.debug("Deleting outdated ZIP: {}", s3ObjectSummary.getKey());
				awsS3.deleteObject(bucketName.value, s3ObjectSummary.getKey());
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
			byte[] binFileContent = generateExportFileContentFrom(temporaryExposureKeys);
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
			binFile.delete();
			sigFile.delete();
		}
	}

	private List<StoredTemporaryExposureKey> validKeysFromSubmissions(List<Submission> submissions, DistributionServiceWindow window, ZIPSubmissionPeriod zipPeriod) {
		List<StoredTemporaryExposureKey> temporaryExposureKeys = new ArrayList<>();
		for (Submission submission : submissions) {
			if (zipPeriod.isCoveringSubmissionDate(submission.submissionDate, DistributionServiceWindow.ZIP_SUBMISSION_PERIOD_OFFSET_MINUTES)) {

				for (StoredTemporaryExposureKey key : submission.payload.temporaryExposureKeys) {
					ENIntervalNumber keyIntervalNumber = new ENIntervalNumber(key.rollingStartNumber);

					if (keyIntervalNumber.validUntil(window.zipExpirationExclusive())) {
						logger.debug("{}: included submission: {}: included: {}", zipPeriod.zipPath(), submission.submissionDate, keyIntervalNumber);

						temporaryExposureKeys.add(key);
					}
				}
			}
		}

		Collections.shuffle(temporaryExposureKeys);

		return temporaryExposureKeys;
	}

	private byte[] generateExportFileContentFrom(List<StoredTemporaryExposureKey> temporaryExposureKeys) throws IOException {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		bout.write(EK_EXPORT_V1_HEADER.getBytes());
		Exposure.TemporaryExposureKeyExport export = exposureProtobuf.buildTemporaryExposureKeyExport(temporaryExposureKeys);
		bout.write(export.toByteArray());
		return bout.toByteArray();
	}

	private byte[] generateSigFileContentFrom(byte[] binFileContent) {
		Signature signature = signer.sign(binFileContent);
		Exposure.TEKSignatureList tekSignatureList = exposureProtobuf.buildTEKSignatureList(signature.asByteBuffer());
		return tekSignatureList.toByteArray();
	}

}
