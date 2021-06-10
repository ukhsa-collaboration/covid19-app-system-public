package uk.nhs.nhsx.highriskpostcodesupload

import uk.nhs.nhsx.core.exceptions.ApiResponseException
import uk.nhs.nhsx.core.exceptions.HttpStatusCode.UNPROCESSABLE_ENTITY_422
import uk.nhs.nhsx.domain.LocalAuthority
import uk.nhs.nhsx.domain.PostDistrict
import uk.nhs.nhsx.domain.PostDistrictIndicators
import uk.nhs.nhsx.domain.RiskIndicator
import uk.nhs.nhsx.domain.TierIndicator

class RiskyPostCodesMapper(private val tierMetadata: Map<String, Map<String, Any>>) {
    private val validTierIndicators = tierMetadata.keys.map { TierIndicator.of(it) }

    fun mapOrThrow(request: RiskyPostDistrictsRequest): RiskyPostCodesResult {
        val postCodesToRiskIndicator = mutableMapOf<PostDistrict, RiskIndicator>()
        val postCodesToTierIndicator = mutableMapOf<PostDistrict, TierIndicator>()
        val localAuthoritiesToRiskIndicator = mutableMapOf<LocalAuthority, TierIndicator>()

        request.postDistricts.forEach { (postDistrict, indicators) ->
            validatePostDistrict(postDistrict, indicators)
            postCodesToRiskIndicator[postDistrict] = indicators.riskIndicator
            postCodesToTierIndicator[postDistrict] = indicators.tierIndicator
        }

        request.localAuthorities.forEach { (localAuthority, indicators) ->
            validateLocalAuthority(localAuthority, indicators)
            localAuthoritiesToRiskIndicator[localAuthority] = indicators.tierIndicator
        }

        val v1 = RiskyPostCodesV1(postCodesToRiskIndicator)
        val v2 = RiskyPostCodesV2(postCodesToTierIndicator, localAuthoritiesToRiskIndicator, tierMetadata)

        return RiskyPostCodesResult(v1, v2)
    }

    private fun validatePostDistrict(postDistrict: PostDistrict, indicators: PostDistrictIndicators) {
        if (!validTierIndicators.contains(indicators.tierIndicator)) {
            throw validationErrorWith(
                "Invalid tier indicator: ${indicators.tierIndicator} for post district: $postDistrict"
            )
        }
    }

    private fun validateLocalAuthority(localAuthority: LocalAuthority, indicators: LocalAuthorityIndicators) {
        if (!validTierIndicators.contains(indicators.tierIndicator)) {
            throw validationErrorWith(
                "Invalid tier indicator: ${indicators.tierIndicator} for local authority: $localAuthority"
            )
        }
    }

    fun convertToAnalyticsCsv(request: RiskyPostDistrictsRequest): String =
        (listOf("# postal_district_code, risk_indicator, tier_indicator") + request.postDistricts
            .map(toCsvRow()))
            .joinToString("\n")

    private fun toCsvRow(): (Map.Entry<PostDistrict, PostDistrictIndicators>) -> String = { it ->
        """"${it.key}", "${it.value.riskIndicator}", "${it.value.tierIndicator}""""
    }

    private fun validationErrorWith(reason: String): ApiResponseException =
        ApiResponseException(UNPROCESSABLE_ENTITY_422, "validation error: $reason")
}
