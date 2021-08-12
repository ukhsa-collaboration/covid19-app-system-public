package uk.nhs.nhsx.diagnosiskeydist.s3

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.flatMap
import uk.nhs.nhsx.core.SystemClock
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.diagnosiskeydist.Submission
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey
import uk.nhs.nhsx.testhelper.mocks.FakeDiagnosisKeysS3
import uk.nhs.nhsx.testhelper.s3.S3ObjectSummary
import java.time.Instant

class SubmissionFromS3RepositoryTest {

    private val clock = SystemClock.CLOCK
    private val now = clock()
    private val nowMinus5Sec = now.minusSeconds(5)
    private val nowMinus1Sec = now.minusSeconds(1)

    @Test
    fun `submissions from s3 accept all filter`() {
        val fakeS3 = FakeDiagnosisKeysS3(
            S3ObjectSummary("my-prefix-abc", lastModified = nowMinus5Sec),
            S3ObjectSummary("my-prefix-def", lastModified = now),
            S3ObjectSummary("abcdef", lastModified = nowMinus1Sec)
        )

        val submissions = SubmissionFromS3Repository(fakeS3).loadAllSubmissions()

        expectThat(submissions)
            .flatMap(::keys)
            .containsExactly("my-prefix-abc", "abcdef", "my-prefix-def")
    }

    @Test
    fun `submissions from s3 has PCR as test type`() {
        val fakeS3 = FakeDiagnosisKeysS3(
            S3ObjectSummary("mobile/LAB_RESULT/abc.json", lastModified = nowMinus5Sec),
            S3ObjectSummary("mobile/RAPID_RESULT/def.json", lastModified = now),
            S3ObjectSummary("mobile/RAPID_SELF_REPORTED/ghi.json", lastModified = nowMinus1Sec)
        )

        val submissions = SubmissionFromS3Repository(fakeS3).loadAllSubmissions()

        expectThat(submissions)
            .flatMap(::keys)
            .containsExactly(
                "mobile/LAB_RESULT/abc.json",
                "mobile/RAPID_SELF_REPORTED/ghi.json",
                "mobile/RAPID_RESULT/def.json"
            )
    }

    @Test
    fun `submissions from s3 submission time filter`() {
        val nowEpoch = now.toEpochMilli()

        val fakeS3 = FakeDiagnosisKeysS3(
            S3ObjectSummary("veryold", lastModified = Instant.ofEpochMilli(nowEpoch - 2 * 60000)),
            S3ObjectSummary("old", lastModified = Instant.ofEpochMilli(nowEpoch - 60000)),
            S3ObjectSummary("now", lastModified = Instant.ofEpochMilli(nowEpoch)),
            S3ObjectSummary("young", lastModified = Instant.ofEpochMilli(nowEpoch + 60000))
        )

        val submissions = SubmissionFromS3Repository(fakeS3)
            .loadAllSubmissions(nowEpoch, 100, 100)

        expectThat(submissions)
            .flatMap(::keys)
            .containsExactly("young")
    }

    @Test
    fun `submissions from s3 max results`() {
        val nowEpoch = now.toEpochMilli()

        val fakeS3 = FakeDiagnosisKeysS3(
            S3ObjectSummary("A", lastModified = Instant.ofEpochMilli(nowEpoch + 4 * 60000)),
            S3ObjectSummary("B", lastModified = Instant.ofEpochMilli(nowEpoch + 3 * 60000)),
            S3ObjectSummary("C", lastModified = Instant.ofEpochMilli(nowEpoch + 2 * 60000)),
            S3ObjectSummary("D", lastModified = Instant.ofEpochMilli(nowEpoch + 60000)),
            S3ObjectSummary("D", lastModified = Instant.ofEpochMilli(nowEpoch - 60000))
        )

        val submissions = SubmissionFromS3Repository(fakeS3)
            .loadAllSubmissions(nowEpoch, 3, 3)

        expectThat(submissions)
            .flatMap(::keys)
            .containsExactly("D", "C", "B")
    }

    @Test
    fun `filter submissions from s3`() {
        val fakeS3 = FakeDiagnosisKeysS3(
            listOf(
                S3ObjectSummary("my-prefix-abc", lastModified = nowMinus5Sec),
                S3ObjectSummary("my-prefix-def", lastModified = now),
                S3ObjectSummary("abcdef", lastModified = nowMinus1Sec)
            )
        )

        val submissions = SubmissionFromS3Repository(fakeS3) { objectKey ->
            !objectKey.value.startsWith("my-prefix")
        }.loadAllSubmissions()

        expectThat(submissions)
            .flatMap(::keys)
            .containsExactly("abcdef")
    }

    @Test
    fun `filter submissions from s3 by prefix`() {
        val fakeS3 = FakeDiagnosisKeysS3(
            S3ObjectSummary("my-prefix-abc", lastModified = nowMinus5Sec),
            S3ObjectSummary("/bla/my-prefix-def", lastModified = now),
            S3ObjectSummary("/mobile/abc", lastModified = nowMinus1Sec)
        )

        val submissions = SubmissionFromS3Repository(fakeS3) { objectKey ->
            listOf("/nearform/IE", "/nearform/NIR", "/mobile").any { objectKey.value.startsWith(it) }
        }.loadAllSubmissions()

        expectThat(submissions)
            .flatMap(::keys)
            .containsExactly("/mobile/abc")
    }

    @Test
    fun `skip deleted submissions from s3`() {
        val fakeS3 = FakeDiagnosisKeysS3(
            listOf(
                S3ObjectSummary("my-prefix-abc", lastModified = nowMinus5Sec),
                S3ObjectSummary("/bla/my-prefix-def", lastModified = now),
                S3ObjectSummary("/mobile/abc", lastModified = nowMinus1Sec)
            ), listOf("my-prefix-abc")
        )

        val submissions = SubmissionFromS3Repository(fakeS3).loadAllSubmissions()

        expectThat(submissions)
            .flatMap(::keys)
            .containsExactly("/mobile/abc", "/bla/my-prefix-def")
    }

    @Suppress("TestFunctionName")
    private fun SubmissionFromS3Repository(
        fakeS3: FakeDiagnosisKeysS3,
        predicate: (ObjectKey) -> Boolean = { true }
    ) = SubmissionFromS3Repository(
        fakeS3,
        predicate,
        BucketName.of("SUBMISSION_BUCKET"),
        RecordingEvents()
    ) { now }

    private fun keys(submission: Submission) = submission
        .payload
        .temporaryExposureKeys
        .map(StoredTemporaryExposureKey::key)
}

