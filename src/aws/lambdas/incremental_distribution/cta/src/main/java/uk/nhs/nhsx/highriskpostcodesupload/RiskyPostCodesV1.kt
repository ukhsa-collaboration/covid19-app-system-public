package uk.nhs.nhsx.highriskpostcodesupload

import uk.nhs.nhsx.domain.PostDistrict
import uk.nhs.nhsx.domain.RiskIndicator

data class RiskyPostCodesV1(val postDistricts: Map<PostDistrict, RiskIndicator>)
