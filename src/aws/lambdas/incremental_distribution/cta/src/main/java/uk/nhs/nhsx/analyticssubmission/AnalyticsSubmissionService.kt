package uk.nhs.nhsx.analyticssubmission

import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose
import com.amazonaws.services.kinesisfirehose.model.PutRecordRequest
import com.amazonaws.services.kinesisfirehose.model.Record
import uk.nhs.nhsx.analyticssubmission.AnalyticsMapFlattener.recFlatten
import uk.nhs.nhsx.analyticssubmission.UploadType.Firehose
import uk.nhs.nhsx.analyticssubmission.model.AnalyticsWindow
import uk.nhs.nhsx.analyticssubmission.model.ClientAnalyticsSubmissionPayload
import uk.nhs.nhsx.core.Clock
import uk.nhs.nhsx.core.Json.toJson
import uk.nhs.nhsx.core.events.Events
import java.nio.ByteBuffer
import java.time.Duration

class AnalyticsSubmissionService(
    private val config: AnalyticsConfig,
    private val kinesisFirehose: AmazonKinesisFirehose,
    private val events: Events,
    private val clock: Clock
) {
    fun accept(payload: ClientAnalyticsSubmissionPayload) {
        when {
            isDateRangeInvalid(payload.analyticsWindow) -> {
                events(
                    AnalyticsSubmissionRejected(
                        startDate = payload.analyticsWindow.startDate.toString(),
                        endDate = payload.analyticsWindow.endDate.toString(),
                        deviceModel = payload.metadata.deviceModel,
                        appVersion = payload.metadata.latestApplicationVersion,
                        osVersion = payload.metadata.operatingSystemVersion
                    )
                )
            }
            config.firehoseIngestEnabled -> {
                val (postalDistrict, localAuthority) = PostDistrictLaReplacer.replacePostDistrictLA(
                    payload.metadata.postalDistrict,
                    payload.metadata.localAuthority,
                    events
                )

                val storeModel = recFlatten(payload)
                    .toMutableMap()
                    .also {
                        it["postalDistrict"] = postalDistrict
                        it["localAuthority"] = localAuthority
                    }

                val json = toJson(storeModel)
                uploadToFirehose(json)
                events(AnalyticsSubmissionUploaded(Firehose, config.firehoseStreamName))
            }
            else -> {
                events(AnalyticsSubmissionDisabled)
            }
        }
    }

    private fun isDateRangeInvalid(analyticsWindow: AnalyticsWindow) =
        analyticsWindow.startDate.isAfter(clock().plus(Duration.ofDays(365))) ||
            analyticsWindow.endDate.isAfter(clock().plus(Duration.ofDays(365)))

    private fun uploadToFirehose(json: String) {
        val record = Record().withData(ByteBuffer.wrap(json.toByteArray()))
        val putRecordRequest = PutRecordRequest()
            .withRecord(record)
            .withDeliveryStreamName(config.firehoseStreamName)
        kinesisFirehose.putRecord(putRecordRequest)
    }
}
