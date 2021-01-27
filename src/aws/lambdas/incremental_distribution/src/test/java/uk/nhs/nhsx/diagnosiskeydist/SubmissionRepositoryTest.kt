package uk.nhs.nhsx.diagnosiskeydist

import com.amazonaws.services.s3.model.S3ObjectSummary
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.diagnosiskeydist.s3.SubmissionFromS3Repository
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKeyPayload
import uk.nhs.nhsx.testhelper.data.TestData
import java.io.ByteArrayInputStream
import java.time.Instant
import java.util.*

class SubmissionRepositoryTest {

    private val now = Instant.parse("2020-01-13T13:00:00.000Z")

    @Test
    fun deserializesStoredPayload() {
        val inputStream = ByteArrayInputStream(TestData.STORED_KEYS_PAYLOAD.toByteArray())
        val payload = SubmissionRepository.getTemporaryExposureKeys(inputStream)
        assertThat(payload.temporaryExposureKeys).isNotEmpty
        assertThat(payload.temporaryExposureKeys.stream().allMatch { matchKey(it, TestData.STORED_KEYS_PAYLOAD_DESERIALIZED) }).isTrue
    }

    @Test
    fun deserializesStoredPayloadWithDaysSinceOnset() {
        val inputStream = ByteArrayInputStream(TestData.STORED_KEYS_PAYLOAD_DAYS_SINCE_ONSET.toByteArray())
        val payload = SubmissionRepository.getTemporaryExposureKeys(inputStream)
        assertThat(payload.temporaryExposureKeys).isNotEmpty
        assertThat(payload.temporaryExposureKeys.stream().allMatch { matchKey(it, TestData.STORED_KEYS_PAYLOAD_DESERIALIZED_DAYS_SINCE_ONSET) }).isTrue
    }

    @Test
    fun `limits submissions`() {
        val summaries = listOf(
            summary(2, "e", now),
            summary(2, "f", now),
            summary(2, "d", now),
            summary(3, "a", now),
            summary(3, "b", now),
            summary(3, "c", now),
            summary(1, "h", now),
            summary(1, "g", now),
            summary(1, "j", now),
        )

        val testcases = listOf(
            Triple(1, 5, listOf("a", "b", "c")),
            Triple(2, 5, listOf("a", "b", "c")),
            Triple(3, 5, listOf("a", "b", "c")),
            Triple(4, 5, listOf("a", "b", "c", "e", "f")),
            Triple(5, 5, listOf("a", "b", "c", "e", "f")),
            Triple(6, 5, listOf("a", "b", "c", "e", "f")),
            Triple(7, 100, listOf("a", "b", "c", "e", "f", "d", "h", "g", "j")),
        )

        testcases.forEach { (limit, maxResults, expected) ->
            assertThat(SubmissionFromS3Repository.limit(summaries, limit, maxResults))
                .describedAs("limit: $limit, maxResults: $maxResults")
                .extracting("key")
                .containsExactlyElementsOf(expected)
        }
    }

    @Test
    fun `throws exception if limit is 0`() {
        val summaries = listOf(summary(2, "e", now))

        assertThatThrownBy { SubmissionFromS3Repository.limit(summaries, 0, 10) }
            .hasMessage("limit needs to be greater than 0")
    }

    @Test
    fun `throws exception if limit is negative`() {
        val summaries = listOf(summary(2, "e", now))

        assertThatThrownBy { SubmissionFromS3Repository.limit(summaries, -1, 10) }
            .hasMessage("limit needs to be greater than 0")
    }

    private fun summary(ageSeconds: Long, objectKey: String, now: Instant): S3ObjectSummary =
        S3ObjectSummary().apply {
            key = objectKey
            lastModified = Date.from(now.minusSeconds(ageSeconds))
        }

    private fun matchKey(storedKey: StoredTemporaryExposureKey, deserializedPayload: StoredTemporaryExposureKeyPayload): Boolean =
        deserializedPayload.temporaryExposureKeys.stream()
            .anyMatch { k: StoredTemporaryExposureKey ->
                k.key == storedKey.key
                    && k.rollingPeriod == storedKey.rollingPeriod
                    && k.rollingStartNumber == storedKey.rollingStartNumber
                    && k.transmissionRisk == storedKey.transmissionRisk
                    && (k.daysSinceOnsetOfSymptoms == null && storedKey.daysSinceOnsetOfSymptoms == null || k.daysSinceOnsetOfSymptoms != null && k.daysSinceOnsetOfSymptoms == storedKey.daysSinceOnsetOfSymptoms)
            }
}
