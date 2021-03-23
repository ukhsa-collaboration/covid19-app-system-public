package uk.nhs.nhsx.diagnosiskeydist.s3

import com.amazonaws.services.s3.model.S3ObjectSummary
import uk.nhs.nhsx.core.Clock
import uk.nhs.nhsx.core.aws.s3.AwsS3
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.Locator.Companion.of
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.InfoEvent
import uk.nhs.nhsx.diagnosiskeydist.ConcurrentExecution
import uk.nhs.nhsx.diagnosiskeydist.ConcurrentExecution.Companion.SYSTEM_EXIT_ERROR_HANDLER
import uk.nhs.nhsx.diagnosiskeydist.Submission
import uk.nhs.nhsx.diagnosiskeydist.SubmissionRepository
import uk.nhs.nhsx.diagnosiskeydist.SubmissionRepository.Companion.getTemporaryExposureKeys
import java.time.Duration
import java.util.*
import java.util.Collections.synchronizedList
import java.util.function.Predicate

class SubmissionFromS3Repository(
    private val awsS3: AwsS3,
    private val objectKeyFilter: Predicate<ObjectKey>,
    private val submissionBucketName: BucketName,
    private val events: Events,
    private val clock: Clock
) : SubmissionRepository {

    companion object {
        private val LOAD_SUBMISSIONS_TIMEOUT = Duration.ofMinutes(6)
    }

    override fun loadAllSubmissions(
        minimalSubmissionTimeEpochMillisExclusive: Long,
        limit: Int,
        maxResults: Int
    ): List<Submission> {

        val start = clock()

        var summaries = awsS3.getObjectSummaries(submissionBucketName)
            .filter { objectKeyFilter.test(ObjectKey.of(it.key)) }
            .filter { it.lastModified.time > minimalSubmissionTimeEpochMillisExclusive }

        summaries = summaries.limit(limit, maxResults)

        events(
            InfoEvent(
                "Submission summaries loaded. Count=${summaries.size}, Duration=${
                    Duration.between(start, clock()).toMillis()
                }ms"
            )
        )

        val submissions = synchronizedList(ArrayList<Submission>())

        ConcurrentExecution(
            "LoadSubmissions",
            LOAD_SUBMISSIONS_TIMEOUT,
            events,
            clock,
            SYSTEM_EXIT_ERROR_HANDLER
        ).use { pool ->
            for (objectSummary in summaries) {
                pool.execute {
                    awsS3.getObject(of(submissionBucketName, ObjectKey.of(objectSummary.key)))
                        .ifPresentOrElse(
                            { o ->
                                o.objectContent.use {
                                    submissions.add(
                                        Submission(
                                            objectSummary.lastModified.toInstant(),
                                            getTemporaryExposureKeys(it)
                                        )
                                    )
                                }
                                events(SubmissionLoaded(submissionBucketName, objectSummary.key))
                            }
                        ) { events(SubmissionMissing(submissionBucketName, objectSummary.key)) }
                }
            }
        }

        return submissions.sortedBy { it.submissionDate }
    }
}

fun List<S3ObjectSummary>.limit(limit: Int, maxResults: Int): List<S3ObjectSummary> {
    require(limit > 0) { "limit needs to be greater than 0" }

    val limited = mutableListOf<S3ObjectSummary>()

    val i = sortedBy { it.lastModified }.iterator()

    var lastModified: Date? = null
    var counter = 1

    while (i.hasNext() && counter <= maxResults) {
        val summary = i.next()
        if (counter > limit && lastModified != summary.lastModified) break
        limited.add(summary)
        lastModified = summary.lastModified
        counter++
    }

    return limited
}
