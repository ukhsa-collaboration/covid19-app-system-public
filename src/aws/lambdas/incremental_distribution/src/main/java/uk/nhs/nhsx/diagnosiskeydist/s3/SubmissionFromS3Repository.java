package uk.nhs.nhsx.diagnosiskeydist.s3;

import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.nhs.nhsx.core.aws.s3.*;
import uk.nhs.nhsx.diagnosiskeydist.ConcurrentExecution;
import uk.nhs.nhsx.diagnosiskeydist.Submission;
import uk.nhs.nhsx.diagnosiskeydist.SubmissionRepository;
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKeyPayload;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

public class SubmissionFromS3Repository implements SubmissionRepository {

    private static final Logger logger = LogManager.getLogger(SubmissionFromS3Repository.class);

    private static final int MAXIMAL_S3_LOAD_PROTOBOF_TIME_MINUTES = 6;
    private static final Duration LOAD_SUBMISSIONS_TIMEOUT = Duration.ofMinutes(MAXIMAL_S3_LOAD_PROTOBOF_TIME_MINUTES);

    private final AwsS3 awsS3;
    private final Predicate<ObjectKey> objectKeyFilter;
    private final BucketName submissionBucketName;

    public SubmissionFromS3Repository(AwsS3 awsS3,
                                      Predicate<ObjectKey> objectKeyFilter,
                                      BucketName submissionJsonBucketName) {
        this.awsS3 = awsS3;
        this.objectKeyFilter = objectKeyFilter;
        submissionBucketName = submissionJsonBucketName;
    }

    @Override
    public List<Submission> loadAllSubmissions(long minimalSubmissionTimeEpocMillisExclusive,
                                               int limit,
                                               int maxResults) throws Exception {
        long start = System.currentTimeMillis();

        List<S3ObjectSummary> summaries =
            awsS3.getObjectSummaries(submissionBucketName)
                .stream()
                .filter(it -> objectKeyFilter.test(ObjectKey.of(it.getKey())))
                .filter(it -> it.getLastModified().getTime() > minimalSubmissionTimeEpocMillisExclusive)
                .collect(toList());

        summaries = limit(summaries, limit, maxResults);

        logger.info("Submission summaries loaded. Count={}, Duration={}ms", summaries.size(), (System.currentTimeMillis() - start));

        List<Submission> submissions = Collections.synchronizedList(new ArrayList<>());
        try (ConcurrentExecution pool = new ConcurrentExecution("LoadSubmissions", LOAD_SUBMISSIONS_TIMEOUT)) {
            for (S3ObjectSummary objectSummary : summaries) {
                pool.execute(() ->
                    awsS3.getObject(Locator.of(submissionBucketName, ObjectKey.of(objectSummary.getKey())))
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
                            () -> logger.warn("Bucket: {} does not have key: {}", submissionBucketName, objectSummary.getKey())
                        ));
            }
        }

        return submissions.stream()
            .sorted(comparing(e -> e.submissionDate))
            .collect(toList());
    }

    public static List<S3ObjectSummary> limit(List<S3ObjectSummary> summaries, int limit, int maxResults) {
        checkArgument(limit > 0, "limit needs to be greater than 0");

        List<S3ObjectSummary> limited = new LinkedList<>();

        Iterator<S3ObjectSummary> i = summaries.stream()
            .sorted(comparing(S3ObjectSummary::getLastModified))
            .iterator();

        Date lastModified = null;

        for (int counter = 1; i.hasNext() && counter <= maxResults; counter++) {
            S3ObjectSummary summary = i.next();

            if (counter > limit) {
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
