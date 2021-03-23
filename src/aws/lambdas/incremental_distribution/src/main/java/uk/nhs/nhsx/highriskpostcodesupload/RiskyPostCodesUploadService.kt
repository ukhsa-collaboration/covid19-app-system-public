package uk.nhs.nhsx.highriskpostcodesupload

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import uk.nhs.nhsx.core.HttpResponses.accepted
import uk.nhs.nhsx.core.HttpResponses.unprocessableEntity
import uk.nhs.nhsx.core.Jackson.readOrNull
import uk.nhs.nhsx.core.Jackson.toJson
import uk.nhs.nhsx.core.aws.cloudfront.AwsCloudFront
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.UnprocessableJson

class RiskyPostCodesUploadService(
    private val persistence: RiskyPostCodesPersistence,
    private val awsCloudFront: AwsCloudFront,
    private val cloudFrontDistributionId: String,
    private val cloudFrontInvalidationPattern: String,
    private val events: Events
) {
    fun upload(rawJson: String?): APIGatewayProxyResponseEvent =
        rawJson
            ?.let { readOrNull<RiskyPostDistrictsRequest>(it) { e -> events(UnprocessableJson(e)) } }
            ?.let {
                val riskLevels = persistence.retrievePostDistrictRiskLevels()
                val mapper = RiskyPostCodesMapper(riskLevels)
                val result = mapper.mapOrThrow(it)
                val analyticsCsv = mapper.convertToAnalyticsCsv(it)
                persistence.uploadToBackup(rawJson)
                persistence.uploadToRaw(analyticsCsv)
                persistence.uploadPostDistrictsVersion1(toJson(result.riskyPostCodesV1))
                persistence.uploadPostDistrictsVersion2(toJson(result.riskyPostCodesV2))
                awsCloudFront.invalidateCache(cloudFrontDistributionId, cloudFrontInvalidationPattern)
                accepted("successfully uploaded")
            }
            ?: unprocessableEntity("validation error: unable to deserialize payload")
}
