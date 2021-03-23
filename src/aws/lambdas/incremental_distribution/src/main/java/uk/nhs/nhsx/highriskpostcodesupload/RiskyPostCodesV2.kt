package uk.nhs.nhsx.highriskpostcodesupload

data class RiskyPostCodesV2(
    val postDistricts: Map<PostDistrict, TierIndicator>,
    val localAuthorities: Map<LocalAuthority, TierIndicator>,
    val riskLevels: Map<String, Map<String, Any>>
)
