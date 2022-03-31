package uk.nhs.nhsx.analyticsevents

import uk.nhs.nhsx.analyticssubmission.PostDistrictLaReplacer
import uk.nhs.nhsx.core.ContentType.Companion.APPLICATION_JSON
import uk.nhs.nhsx.core.Json.toJson
import uk.nhs.nhsx.core.aws.s3.AwsS3
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.ByteArraySource.Companion.fromUtf8String
import uk.nhs.nhsx.core.aws.s3.Locator
import uk.nhs.nhsx.core.aws.s3.ObjectKeyNameProvider
import uk.nhs.nhsx.core.events.Events
import java.util.*

class AnalyticsEventsSubmissionService(
    private val awsS3: AwsS3,
    private val objectKeyNameProvider: ObjectKeyNameProvider,
    private val bucketName: BucketName,
    private val events: Events
) {
    fun accept(payload: Map<String, Any>) {
        uploadToS3(toJson(transformPayload(payload)))
    }

    private fun transformPayload(payload: Map<String, Any>): Map<String, Any> {
        val metadata = payload["metadata"] as? Map<*, *> ?: error("metadata must be a map")

        val currentPostalDistrict = metadata["postalDistrict"] as String
        val currentLocalAuthority = metadata["localAuthority"] as String?
        val mappedPostDistrict = PostDistrictLaReplacer(
            currentPostalDistrict,
            currentLocalAuthority,
            events
        )

        return payload.toMutableMap().apply {
            this["uuid"] = UUID.randomUUID()
            this["metadata"] = metadata.toMutableMap().apply {
                this["postalDistrict"] = mappedPostDistrict.postDistrict
                this["localAuthority"] = mappedPostDistrict.localAuthorityId
            }

        }
    }

    private fun uploadToS3(json: String) {
        awsS3.upload(
            Locator.of(bucketName, objectKeyNameProvider.generateObjectKeyName().append(".json")),
            APPLICATION_JSON,
            fromUtf8String(json)
        )
    }
}
