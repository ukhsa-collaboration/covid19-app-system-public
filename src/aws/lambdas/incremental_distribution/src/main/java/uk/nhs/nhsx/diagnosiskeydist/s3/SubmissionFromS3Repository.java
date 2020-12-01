package uk.nhs.nhsx.diagnosiskeydist.s3;

import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.nhs.nhsx.core.aws.s3.AwsS3;
import uk.nhs.nhsx.diagnosiskeydist.ConcurrentExecution;
import uk.nhs.nhsx.diagnosiskeydist.Submission;
import uk.nhs.nhsx.diagnosiskeydist.SubmissionRepository;
import uk.nhs.nhsx.diagnosiskeydist.utils.ConfigurationUtility;
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKeyPayload;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class SubmissionFromS3Repository implements SubmissionRepository {

    private static final Logger logger = LogManager.getLogger(SubmissionFromS3Repository.class);
    private static final int MAXIMAL_S3_LOAD_PROTOBOF_TIME_MINUTES = 6;
    private static final String submissionBucketName = ConfigurationUtility.SUBMISSION_JSON_BUCKET_NAME;

    private final AwsS3 awsS3;
    private final Predicate<String> objectKeyFilter;

    public SubmissionFromS3Repository(AwsS3 awsS3, Predicate<String> objectKeyFilter) {
        this.awsS3 = awsS3;
        this.objectKeyFilter = objectKeyFilter;
    }

    @Override
    public List<Submission> loadAllSubmissions(long minimalSubmissionTimeEpocMillisExclusive, int limit, int maxResults) throws Exception {
        long start = System.currentTimeMillis();

        List<S3ObjectSummary> summaries =
            awsS3.getObjectSummaries(submissionBucketName)
                .stream()
                .filter(it -> objectKeyFilter.test(it.getKey()))
                .filter(it -> it.getLastModified().getTime() > minimalSubmissionTimeEpocMillisExclusive)
                .collect(Collectors.toList());

        summaries = limit(summaries, limit, maxResults);

        logger.info("Submission summaries loaded. Count={}, Duration={}ms", summaries.size(), (System.currentTimeMillis() - start));

        List<Submission> submissions = Collections.synchronizedList(new ArrayList<>());
        try (ConcurrentExecution pool = new ConcurrentExecution("LoadSubmissions", Duration.ofMinutes(MAXIMAL_S3_LOAD_PROTOBOF_TIME_MINUTES))) {
            for (S3ObjectSummary objectSummary : summaries) {
                pool.execute(() ->
                    awsS3.getObject(submissionBucketName, objectSummary.getKey())
                        .ifPresentOrElse(
                            (s3Object) -> {
                                try (S3ObjectInputStream s3inputStream = s3Object.getObjectContent()) {
                                    StoredTemporaryExposureKeyPayload payload = SubmissionRepository.getTemporaryExposureKeys(s3inputStream);
                                    submissions.add(new Submission(objectSummary.getLastModified(), payload));
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                                logger.debug("Submission loaded: {}", objectSummary.getKey());
                            },
                            () -> logger.warn("Bucket: " + submissionBucketName + " does not have key: " + objectSummary.getKey())
                        ));
            }
        }

        return submissions.stream()
                .sorted((e1, e2) -> e1.submissionDate.compareTo(e2.submissionDate))
                .collect(Collectors.toList());
    }

    public static List<S3ObjectSummary> limit(List<S3ObjectSummary> summaries, int limit, int maxResults) {
        List<S3ObjectSummary> limited = new LinkedList<>();
        Iterator<S3ObjectSummary> i = summaries.stream()
            .sorted((e1, e2) -> e1.getLastModified().compareTo(e2.getLastModified()))
            .iterator();
        Date lastModified = null;
        for (int counter = 1; i.hasNext() && counter <= maxResults; counter++) {
            S3ObjectSummary summary = i.next();

            if (counter == limit) {
                lastModified = summary.getLastModified();
            }
            else if (counter > limit) {
                if (!lastModified.equals(summary.getLastModified())) {
                    break;
                }
            }

            limited.add(summary);
            lastModified = summary.getLastModified();
        }
        return limited;
    }
}
