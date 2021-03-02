package uk.nhs.nhsx.diagnosiskeydist.s3;

import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import uk.nhs.nhsx.core.aws.s3.AwsS3;
import uk.nhs.nhsx.core.aws.s3.BucketName;
import uk.nhs.nhsx.core.aws.s3.Locator;
import uk.nhs.nhsx.core.aws.s3.ObjectKey;
import uk.nhs.nhsx.core.events.Events;
import uk.nhs.nhsx.core.events.InfoEvent;
import uk.nhs.nhsx.diagnosiskeydist.ConcurrentExecution;
import uk.nhs.nhsx.diagnosiskeydist.Submission;
import uk.nhs.nhsx.diagnosiskeydist.SubmissionRepository;
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKeyPayload;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static uk.nhs.nhsx.core.Preconditions.checkArgument;

public class SubmissionFromS3Repository implements SubmissionRepository {

    private static final int MAXIMAL_S3_LOAD_PROTOBOF_TIME_MINUTES = 6;
    private static final Duration LOAD_SUBMISSIONS_TIMEOUT = Duration.ofMinutes(MAXIMAL_S3_LOAD_PROTOBOF_TIME_MINUTES);

    private final AwsS3 awsS3;
    private final Predicate<ObjectKey> objectKeyFilter;
    private final BucketName submissionBucketName;
    private final Events events;

    public SubmissionFromS3Repository(AwsS3 awsS3,
                                      Predicate<ObjectKey> objectKeyFilter,
                                      BucketName submissionJsonBucketName,
                                      Events events) {
        this.awsS3 = awsS3;
        this.objectKeyFilter = objectKeyFilter;
        this.submissionBucketName = submissionJsonBucketName;
        this.events = events;
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

        events.emit(getClass(), new InfoEvent(("Submission summaries loaded. Count="+summaries.size()+", Duration="+(System.currentTimeMillis() - start)+"ms")));

        List<Submission> submissions = Collections.synchronizedList(new ArrayList<>());
        try (ConcurrentExecution pool = new ConcurrentExecution("LoadSubmissions", LOAD_SUBMISSIONS_TIMEOUT, events)) {
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
                                events.emit(getClass(), new SubmissionLoaded(submissionBucketName, objectSummary.getKey()));
                            },
                            () ->
                                events.emit(getClass(), new SubmissionMissing(submissionBucketName, objectSummary.getKey()))
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
