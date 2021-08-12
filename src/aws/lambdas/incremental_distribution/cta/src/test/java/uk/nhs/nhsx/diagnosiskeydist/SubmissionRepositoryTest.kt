package uk.nhs.nhsx.diagnosiskeydist

import com.amazonaws.services.s3.model.S3ObjectSummary
import org.junit.jupiter.api.Test
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isNotEmpty
import strikt.assertions.isTrue
import strikt.assertions.map
import strikt.assertions.message
import uk.nhs.nhsx.diagnosiskeydist.s3.limit
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKeyPayload
import uk.nhs.nhsx.testhelper.data.TestData
import uk.nhs.nhsx.testhelper.data.TestData.STORED_KEYS_PAYLOAD_DESERIALIZED
import uk.nhs.nhsx.testhelper.data.TestData.STORED_KEYS_PAYLOAD_DESERIALIZED_DAYS_SINCE_ONSET
import uk.nhs.nhsx.testhelper.s3.S3ObjectSummary
import java.io.ByteArrayInputStream
import java.time.Instant

class SubmissionRepositoryTest {

    private val now = Instant.parse("2020-01-13T13:00:00.000Z")

    @Test
    fun `deserializes stored payload`() {
        val inputStream = ByteArrayInputStream(TestData.STORED_KEYS_PAYLOAD.toByteArray())
        val payload = SubmissionRepository.getTemporaryExposureKeys(inputStream)

        expectThat(payload.temporaryExposureKeys).isNotEmpty()
        expectThat(payload.temporaryExposureKeys.all { matchKey(it, STORED_KEYS_PAYLOAD_DESERIALIZED) }).isTrue()
    }

    @Test
    fun `deserializes stored payload with days since onset`() {
        val inputStream = ByteArrayInputStream(TestData.STORED_KEYS_PAYLOAD_DAYS_SINCE_ONSET.toByteArray())
        val payload = SubmissionRepository.getTemporaryExposureKeys(inputStream)

        expectThat(payload.temporaryExposureKeys).isNotEmpty()
        expectThat(payload.temporaryExposureKeys.all {
            matchKey(
                it,
                STORED_KEYS_PAYLOAD_DESERIALIZED_DAYS_SINCE_ONSET
            )
        }).isTrue()
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
            expectThat(summaries.limit(limit, maxResults))
                .describedAs("limit: $limit, maxResults: $maxResults")
                .map(S3ObjectSummary::getKey)
                .containsExactly(expected)
        }
    }

    @Test
    fun `throws exception if limit is 0`() {
        val summaries = listOf(summary(2, "e", now))

        expectCatching { summaries.limit(0, 10) }
            .isFailure().message.isEqualTo("limit needs to be greater than 0")
    }

    @Test
    fun `throws exception if limit is negative`() {
        val summaries = listOf(summary(2, "e", now))

        expectCatching { summaries.limit(-1, 10) }
            .isFailure().message.isEqualTo("limit needs to be greater than 0")
    }

    private fun summary(
        ageSeconds: Long,
        objectKey: String,
        now: Instant
    ) = S3ObjectSummary(objectKey, lastModified = now.minusSeconds(ageSeconds))

    private fun matchKey(
        storedKey: StoredTemporaryExposureKey,
        deserializedPayload: StoredTemporaryExposureKeyPayload
    ) = deserializedPayload.temporaryExposureKeys.any {
        it.key == storedKey.key
            && it.rollingPeriod == storedKey.rollingPeriod
            && it.rollingStartNumber == storedKey.rollingStartNumber
            && it.transmissionRisk == storedKey.transmissionRisk
            && (it.daysSinceOnsetOfSymptoms == null && storedKey.daysSinceOnsetOfSymptoms == null || it.daysSinceOnsetOfSymptoms != null && it.daysSinceOnsetOfSymptoms == storedKey.daysSinceOnsetOfSymptoms)
    }
}
