package uk.nhs.nhsx.diagnosiskeydist.s3

import com.amazonaws.services.s3.model.S3ObjectSummary
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.SystemClock
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.diagnosiskeydist.Submission
import uk.nhs.nhsx.diagnosiskeydist.loadAllSubmissions
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey
import uk.nhs.nhsx.testhelper.mocks.FakeDiagnosisKeysS3
import java.util.Arrays
import java.util.Date
import java.util.function.Predicate

class SubmissionFromS3RepositoryTest {

    private val clock = SystemClock.CLOCK
    private val now = clock()

    private val nowMinus5Sec = now.minusSeconds(5)

    private val nowMinus1Sec = now.minusSeconds(1)

    @Test
    fun `submissions from s3 accept all filter`() {

        val fakeS3 = FakeDiagnosisKeysS3(listOf(
            S3ObjectSummary().apply { key = "my-prefix-abc"; lastModified = Date.from(nowMinus5Sec) },
            S3ObjectSummary().apply { key = "my-prefix-def"; lastModified = Date.from(now) },
            S3ObjectSummary().apply { key = "abcdef"; lastModified = Date.from(nowMinus1Sec) }
        ))
        val submissionRepository = SubmissionFromS3Repository(
            fakeS3,
            { true },
            BucketName.of("SUBMISSION_BUCKET"),
            RecordingEvents(),
            { now }
        )
        val submissions = submissionRepository.loadAllSubmissions()

        assertContainsKeys(submissions, listOf("my-prefix-abc", "abcdef", "my-prefix-def"))
    }

    @Test
    fun `submissions from s3 submission time filter`() {
        val nowEpoch = now.toEpochMilli()

        val fakeS3 = FakeDiagnosisKeysS3(listOf(
            S3ObjectSummary().apply { key = "veryold"; lastModified = Date(nowEpoch - 2 * 60000) },
            S3ObjectSummary().apply { key = "old"; lastModified = Date(nowEpoch - 60000) },
            S3ObjectSummary().apply { key = "now"; lastModified = Date(nowEpoch) },
            S3ObjectSummary().apply { key = "young"; lastModified = Date(nowEpoch + 60000) }
        ))
        val submissionRepository = SubmissionFromS3Repository(
            fakeS3,
            { true },
            BucketName.of("SUBMISSION_BUCKET"),
            RecordingEvents(),
            { now }
        )
        val submissions = submissionRepository.loadAllSubmissions(nowEpoch, 100, 100)

        assertContainsKeys(submissions, listOf("young"))
    }

    @Test
    fun `submissions from s3 max results`() {
        val nowEpoch = now.toEpochMilli()
        val fakeS3 = FakeDiagnosisKeysS3(listOf(
            S3ObjectSummary().apply { key = "A"; lastModified = Date(nowEpoch + 4 * 60000) },
            S3ObjectSummary().apply { key = "B"; lastModified = Date(nowEpoch + 3 * 60000) },
            S3ObjectSummary().apply { key = "C"; lastModified = Date(nowEpoch + 2 * 60000) },
            S3ObjectSummary().apply { key = "D"; lastModified = Date(nowEpoch + 60000) },
            S3ObjectSummary().apply { key = "D"; lastModified = Date(nowEpoch - 60000) }
        ))
        val submissionRepository = SubmissionFromS3Repository(
            fakeS3,
            { true },
            BucketName.of("SUBMISSION_BUCKET"),
            RecordingEvents(),
            { now }
        )
        val submissions = submissionRepository.loadAllSubmissions(nowEpoch, 3, 3)

        assertContainsKeys(submissions, listOf("D", "C", "B"))
    }

    @Test
    fun `filter submissions from s3`() {
        val fakeS3 = FakeDiagnosisKeysS3(listOf(
            S3ObjectSummary().apply { key = "my-prefix-abc"; lastModified = Date.from(nowMinus5Sec) },
            S3ObjectSummary().apply { key = "my-prefix-def"; lastModified = Date.from(now) },
            S3ObjectSummary().apply { key = "abcdef"; lastModified = Date.from(nowMinus1Sec) }
        ))
        val submissionRepository = SubmissionFromS3Repository(
            fakeS3,
            { objectKey -> !objectKey.value.startsWith("my-prefix") },
            BucketName.of("SUBMISSION_BUCKET"),
            RecordingEvents(),
            { now }
        )
        val submissions = submissionRepository.loadAllSubmissions()

        assertContainsKeys(submissions, listOf("abcdef"))
    }

    @Test
    fun `filter submissions from s3 by prefix`() {
        val fakeS3 = FakeDiagnosisKeysS3(listOf(
            S3ObjectSummary().apply { key = "my-prefix-abc"; lastModified = Date.from(nowMinus5Sec) },
            S3ObjectSummary().apply { key = "/bla/my-prefix-def"; lastModified = Date.from(now) },
            S3ObjectSummary().apply { key = "/mobile/abc"; lastModified = Date.from(nowMinus1Sec) }
        ))
        val allowedPrefixes = "/nearform/IE,/nearform/NIR,/mobile".split(",".toRegex()).toTypedArray()
        val matchesPrefix = Predicate { objectKey: ObjectKey ->
            Arrays.stream(allowedPrefixes).anyMatch { prefix: String? -> objectKey.value.startsWith(prefix!!) }
        }
        val submissionRepository = SubmissionFromS3Repository(
            fakeS3,
            matchesPrefix,
            BucketName.of("SUBMISSION_BUCKET"),
            RecordingEvents(),
            { now }
        )
        val submissions = submissionRepository.loadAllSubmissions()

        assertContainsKeys(submissions, listOf("/mobile/abc"))
    }

    @Test
    fun `skip deleted submissions from s3`() {
        val fakeS3 = FakeDiagnosisKeysS3(listOf(
            S3ObjectSummary().apply { key = "my-prefix-abc"; lastModified = Date.from(nowMinus5Sec) },
            S3ObjectSummary().apply { key = "/bla/my-prefix-def"; lastModified = Date.from(now) },
            S3ObjectSummary().apply { key = "/mobile/abc"; lastModified = Date.from(nowMinus1Sec) }
        ), listOf("my-prefix-abc"))

        val submissionRepository = SubmissionFromS3Repository(
            fakeS3,
            { true },
            BucketName.of("SUBMISSION_BUCKET"),
            RecordingEvents(),
            { now }
        )
        val submissions = submissionRepository.loadAllSubmissions()

        assertContainsKeys(submissions, listOf("/mobile/abc", "/bla/my-prefix-def"))
    }

    private fun assertContainsKeys(
        submissions: List<Submission>,
        expected: List<String>
    ) {
        assertThat(submissions).hasSize(expected.size)
            .flatExtracting({ it.payload.temporaryExposureKeys.map(StoredTemporaryExposureKey::key).first() })
            .isEqualTo(expected)
    }
}

