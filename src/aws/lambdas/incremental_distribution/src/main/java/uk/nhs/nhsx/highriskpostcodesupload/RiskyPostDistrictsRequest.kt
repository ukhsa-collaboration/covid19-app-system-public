package uk.nhs.nhsx.highriskpostcodesupload

import uk.nhs.nhsx.domain.LocalAuthority
import uk.nhs.nhsx.domain.PostDistrict
import uk.nhs.nhsx.domain.PostDistrictIndicators

data class RiskyPostDistrictsRequest(
    val postDistricts: Map<PostDistrict, PostDistrictIndicators>,
    val localAuthorities: Map<LocalAuthority, LocalAuthorityIndicators>
)
