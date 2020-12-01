package uk.nhs.nhsx.diagnosiskeydist.s3

import com.amazonaws.services.s3.model.S3Object
import com.amazonaws.services.s3.model.S3ObjectSummary
import com.google.common.io.ByteSource
import org.apache.http.entity.ContentType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.Jackson
import uk.nhs.nhsx.core.aws.s3.AwsS3
import uk.nhs.nhsx.core.aws.s3.MetaHeader
import uk.nhs.nhsx.core.aws.s3.S3Storage
import uk.nhs.nhsx.diagnosiskeydist.agspec.ENIntervalNumber
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKeyPayload
import java.io.ByteArrayInputStream
import java.security.SecureRandom
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Arrays
import java.util.Base64
import java.util.Date
import java.util.Optional
import java.util.function.Predicate

class SubmissionFromS3RepositoryTest {

    @Test
    fun `submissions from s3 accept all filter`() {
        val fakeS3 = FakeDiagnosisKeysS3(listOf(
            S3ObjectSummary().apply { key = "my-prefix-abc"; lastModified = Date() },
            S3ObjectSummary().apply { key = "my-prefix-def"; lastModified = Date() },
            S3ObjectSummary().apply { key = "abcdef"; lastModified = Date() }
        ))
        val submissionRepository = SubmissionFromS3Repository(fakeS3) { true }
        val submissions = submissionRepository.loadAllSubmissions()

        assertThat(submissions).hasSize(3)
    }

    @Test
    fun `submissions from s3 submission time filter`() {
        val now = System.currentTimeMillis();

        val fakeS3 = FakeDiagnosisKeysS3(listOf(
            S3ObjectSummary().apply { key = "veryold"; lastModified = Date(now - 2 * 60000) },
            S3ObjectSummary().apply { key = "old"; lastModified = Date(now - 60000) },
            S3ObjectSummary().apply { key = "now"; lastModified = Date(now) },
            S3ObjectSummary().apply { key = "young"; lastModified = Date(now + 60000) }
        ))
        val submissionRepository = SubmissionFromS3Repository(fakeS3) { true }
        val submissions = submissionRepository.loadAllSubmissions(now, 100, 100)

        assertThat(submissions).hasSize(1)
    }

    @Test
    fun `submissions from s3 max results`() {
        val now = System.currentTimeMillis();

        val fakeS3 = FakeDiagnosisKeysS3(listOf(
            S3ObjectSummary().apply { key = "A"; lastModified = Date(now + 4 * 60000) },
            S3ObjectSummary().apply { key = "B"; lastModified = Date(now + 3 * 60000) },
            S3ObjectSummary().apply { key = "C"; lastModified = Date(now + 2 * 60000) },
            S3ObjectSummary().apply { key = "D"; lastModified = Date(now + 60000) },
            S3ObjectSummary().apply { key = "D"; lastModified = Date(now - 60000) }
        ))
        val submissionRepository = SubmissionFromS3Repository(fakeS3) { true }
        val submissions = submissionRepository.loadAllSubmissions(now, 3, 3)

        assertThat(submissions).hasSize(3)
    }

    @Test
    fun `filter submissions from s3`() {
        val fakeS3 = FakeDiagnosisKeysS3(listOf(
            S3ObjectSummary().apply { key = "my-prefix-abc"; lastModified = Date() },
            S3ObjectSummary().apply { key = "my-prefix-def"; lastModified = Date() },
            S3ObjectSummary().apply { key = "abcdef"; lastModified = Date() }
        ))
        val submissionRepository = SubmissionFromS3Repository(fakeS3) { objectKey -> !objectKey.startsWith("my-prefix") }
        val submissions = submissionRepository.loadAllSubmissions()

        assertThat(submissions).hasSize(1)
    }

    @Test
    fun `filter submissions from s3 by prefix`() {
        val fakeS3 = FakeDiagnosisKeysS3(listOf(
            S3ObjectSummary().apply { key = "my-prefix-abc"; lastModified = Date() },
            S3ObjectSummary().apply { key = "/bla/my-prefix-def"; lastModified = Date() },
            S3ObjectSummary().apply { key = "/mobile/abc"; lastModified = Date() }
        ))
        val allowedPrefixes = "/nearform/IE,/nearform/NIR,/mobile".split(",".toRegex()).toTypedArray()
        val matchesPrefix = Predicate { objectKey: String -> Arrays.stream(allowedPrefixes).anyMatch { prefix: String? -> objectKey.startsWith(prefix!!) } }
        val submissionRepository = SubmissionFromS3Repository(fakeS3, matchesPrefix)
        val submissions = submissionRepository.loadAllSubmissions()

        assertThat(submissions).hasSize(1)
    }

    @Test
    fun `skip deleted submissions from s3`() {
        val fakeS3 = FakeDiagnosisKeysS3(listOf(
            S3ObjectSummary().apply { key = "my-prefix-abc"; lastModified = Date() },
            S3ObjectSummary().apply { key = "/bla/my-prefix-def"; lastModified = Date() },
            S3ObjectSummary().apply { key = "/mobile/abc"; lastModified = Date() }
        ), listOf("my-prefix-abc"))

        val submissionRepository = SubmissionFromS3Repository(fakeS3) { true }
        val submissions = submissionRepository.loadAllSubmissions()

        assertThat(submissions).hasSize(2)
    }

}

class FakeDiagnosisKeysS3(private val objectSummaries: List<S3ObjectSummary>, private val objectsKeysToSkip:List<String> = listOf()) : AwsS3 {
    override fun upload(locator: S3Storage.Locator?, contentType: ContentType?, bytes: ByteSource?, vararg meta: MetaHeader?) {}

    override fun getObjectSummaries(bucketName: String?) = objectSummaries

    override fun getObject(bucketName: String?, key: String?): Optional<S3Object> {
        if(objectsKeysToSkip.contains(key)){
            return Optional.empty()
        }
        val mostRecentKeyRollingStart = ENIntervalNumber.enIntervalNumberFromTimestamp(
            Date.from(LocalDateTime.now().toInstant(ZoneOffset.UTC))
        ).enIntervalNumber / 144 * 144
        val json = Jackson.toJson(StoredTemporaryExposureKeyPayload(
            listOf(makeKey(mostRecentKeyRollingStart - 144))
        ))
        val s3Object = S3Object()
        s3Object.setObjectContent(ByteArrayInputStream(json.toByteArray()))
        return Optional.of(s3Object)
    }

    override fun deleteObject(bucketName: String?, objectKeyName: String?) {}

    private fun makeKey(keyStartTime: Long): StoredTemporaryExposureKey {
        val key = ByteArray(16)
        SecureRandom().nextBytes(key)
        val base64Key = Base64.getEncoder().encodeToString(key)
        return StoredTemporaryExposureKey(base64Key, Math.toIntExact(keyStartTime), 144, 7)
    }
}