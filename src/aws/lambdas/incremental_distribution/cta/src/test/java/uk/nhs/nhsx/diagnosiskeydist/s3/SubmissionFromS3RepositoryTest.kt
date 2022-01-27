package uk.nhs.nhsx.diagnosiskeydist.s3

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.flatMap
import uk.nhs.nhsx.core.SystemClock
import uk.nhs.nhsx.core.aws.s3.AwsS3
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.diagnosiskeydist.Submission
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey
import uk.nhs.nhsx.testhelper.mocks.FakeS3
import uk.nhs.nhsx.testhelper.mocks.exposureS3Object
import java.time.Duration
import java.time.Instant.ofEpochMilli

class SubmissionFromS3RepositoryTest {

    private val clock = SystemClock.CLOCK
    private val now = clock()
    private val nowMinus5Sec = now.minusSeconds(5)
    private val nowMinus1Sec = now.minusSeconds(1)
    private val bucketName = BucketName.of("SUBMISSION_BUCKET")
    private val fakeS3 = FakeS3()

    @Test
    fun `submissions from s3 accept all filter`() {
        fakeS3.add(exposureS3Object("my-prefix-abc", bucketName), nowMinus5Sec)
        fakeS3.add(exposureS3Object("my-prefix-def", bucketName), now)
        fakeS3.add(exposureS3Object("abcdef", bucketName), nowMinus1Sec)

        val submissions = submissionFromS3Repository(fakeS3).loadAllSubmissions()

        expectThat(submissions)
            .flatMap(::keys)
            .containsExactly("my-prefix-abc", "abcdef", "my-prefix-def")
    }

    @Test
    fun `submissions from s3 has PCR as test type`() {
        fakeS3.add(exposureS3Object("mobile/LAB_RESULT/abc.json", bucketName), nowMinus5Sec)
        fakeS3.add(exposureS3Object("mobile/RAPID_RESULT/def.json", bucketName), now)
        fakeS3.add(exposureS3Object("mobile/RAPID_SELF_REPORTED/ghi.json", bucketName), nowMinus1Sec)

        val submissions = submissionFromS3Repository(fakeS3).loadAllSubmissions()

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

        fakeS3.add(exposureS3Object("veryold", bucketName), ofEpochMilli(nowEpoch - 2 * 60000))
        fakeS3.add(exposureS3Object("old", bucketName), ofEpochMilli(nowEpoch - 60000))
        fakeS3.add(exposureS3Object("now", bucketName), ofEpochMilli(nowEpoch))
        fakeS3.add(exposureS3Object("young", bucketName), ofEpochMilli(nowEpoch + 60000))

        val submissions = submissionFromS3Repository(fakeS3)
            .loadAllSubmissions(nowEpoch, 100, 100)

        expectThat(submissions)
            .flatMap(::keys)
            .containsExactly("young")
    }

    @Test
    fun `submissions from s3 max results`() {
        val nowEpoch = now.toEpochMilli()

        fakeS3.add(exposureS3Object("A", bucketName), ofEpochMilli(nowEpoch + 4 * 60000))
        fakeS3.add(exposureS3Object("B", bucketName), ofEpochMilli(nowEpoch + 3 * 60000))
        fakeS3.add(exposureS3Object("C", bucketName), ofEpochMilli(nowEpoch + 2 * 60000))
        fakeS3.add(exposureS3Object("D", bucketName), ofEpochMilli(nowEpoch + 60000))
        fakeS3.add(exposureS3Object("D", bucketName), ofEpochMilli(nowEpoch - 60000))

        val submissions = submissionFromS3Repository(fakeS3)
            .loadAllSubmissions(nowEpoch, 3, 3)

        expectThat(submissions)
            .flatMap(::keys)
            .containsExactly("D", "C", "B")
    }

    @Test
    fun `filter submissions from s3`() {
        fakeS3.add(exposureS3Object("my-prefix-abc", bucketName), nowMinus5Sec)
        fakeS3.add(exposureS3Object("my-prefix-def", bucketName), now)
        fakeS3.add(exposureS3Object("abcdef", bucketName), nowMinus1Sec)

        val submissions = submissionFromS3Repository(fakeS3) { objectKey ->
            !objectKey.value.startsWith("my-prefix")
        }.loadAllSubmissions()

        expectThat(submissions)
            .flatMap(::keys)
            .containsExactly("abcdef")
    }

    @Test
    fun `filter submissions from s3 by prefix`() {
        fakeS3.add(exposureS3Object("my-prefix-abc", bucketName), nowMinus5Sec)
        fakeS3.add(exposureS3Object("/bla/my-prefix-def", bucketName), now)
        fakeS3.add(exposureS3Object("/mobile/abc", bucketName), nowMinus1Sec)

        val submissions = submissionFromS3Repository(fakeS3) { objectKey ->
            listOf("/nearform/IE", "/nearform/NIR", "/mobile").any { objectKey.value.startsWith(it) }
        }.loadAllSubmissions()

        expectThat(submissions)
            .flatMap(::keys)
            .containsExactly("/mobile/abc")
    }

    @Test
    fun `skips submissions from s3`() {
        fakeS3.add(exposureS3Object("my-prefix-abc", bucketName), nowMinus5Sec)
        fakeS3.add(exposureS3Object("/bla/my-prefix-def", bucketName), now)
        fakeS3.add(exposureS3Object("/mobile/abc", bucketName), nowMinus1Sec)

        val submissions = submissionFromS3Repository(fakeS3) {
            !it.value.startsWith("my-prefix-abc")
        }.loadAllSubmissions()

        expectThat(submissions)
            .flatMap(::keys)
            .containsExactly("/mobile/abc", "/bla/my-prefix-def")
    }

    private fun submissionFromS3Repository(
        fakeS3: AwsS3,
        predicate: (ObjectKey) -> Boolean = { true }
    ) = SubmissionFromS3Repository(
        awsS3 = fakeS3,
        objectKeyFilter = predicate,
        submissionBucketName = bucketName,
        loadSubmissionsTimeout = Duration.ofMinutes(12),
        loadSubmissionsThreadPoolSize = 15,
        events = RecordingEvents()
    ) { now }

    private fun keys(submission: Submission) = submission
        .payload
        .temporaryExposureKeys
        .map(StoredTemporaryExposureKey::key)
}

