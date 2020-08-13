package uk.nhs.nhsx.diagnosiskeydist.s3;

import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.nhs.nhsx.core.aws.s3.AwsS3;
import uk.nhs.nhsx.diagnosiskeydist.ConcurrentExecution;
import uk.nhs.nhsx.diagnosiskeydist.Submission;
import uk.nhs.nhsx.diagnosiskeydist.SubmissionRepository;
import uk.nhs.nhsx.diagnosiskeydist.utils.ConfigurationUtility;
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKeyPayload;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SubmissionFromS3Repository implements SubmissionRepository {

	private static final Logger logger = LoggerFactory.getLogger(SubmissionFromS3Repository.class);
    private static final int MAXIMAL_S3_LOAD_PROTOBOF_TIME_MINUTES = 6;

	private static final String submissionBucketName = ConfigurationUtility.SUBMISSION_JSON_BUCKET_NAME;

	private final AwsS3 awsS3;

	public SubmissionFromS3Repository(AwsS3 awsS3) {
		this.awsS3 = awsS3;
	}

	@Override
    public List<Submission> loadAllSubmissions() throws Exception {
    	long start = System.currentTimeMillis();

        List<S3ObjectSummary> summaries = awsS3.getObjectSummaries(submissionBucketName);

        logger.info("Submission summaries loaded. Count={}, Duration={}ms", summaries.size(), (System.currentTimeMillis() - start));

        List<Submission> submissions = Collections.synchronizedList(new ArrayList<>());
        try (ConcurrentExecution pool = new ConcurrentExecution("LoadSubmissions", MAXIMAL_S3_LOAD_PROTOBOF_TIME_MINUTES)) {
        	for (S3ObjectSummary objectSummary : summaries) {
        		pool.execute(() -> {
        			S3Object s3Object = awsS3.getObject(submissionBucketName, objectSummary.getKey())
						.orElseThrow(() -> new RuntimeException("Bucket: " + submissionBucketName + " does have key: " + objectSummary.getKey()));
					
					try(S3ObjectInputStream s3inputStream = s3Object.getObjectContent() ) {
						StoredTemporaryExposureKeyPayload payload = getTemporaryExposureKeys(s3inputStream);
						submissions.add(new Submission(objectSummary.getLastModified(), payload));
					}

					logger.debug("Submission loaded: {}", objectSummary.getKey());
				});
        	}
        }

        return submissions;
    }

}
