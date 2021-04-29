package uk.nhs.nhsx.highriskpostcodesupload

import uk.nhs.nhsx.domain.LocalAuthority
import uk.nhs.nhsx.domain.PostDistrict
import uk.nhs.nhsx.domain.TierIndicator

data class RiskyPostCodesV2(
    val postDistricts: Map<PostDistrict, TierIndicator>,
    val localAuthorities: Map<LocalAuthority, TierIndicator>,
    val riskLevels: Map<String, Map<String, Any>>
)
