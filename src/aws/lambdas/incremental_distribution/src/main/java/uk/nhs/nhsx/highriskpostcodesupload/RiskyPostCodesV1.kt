package uk.nhs.nhsx.highriskpostcodesupload

data class RiskyPostCodesV1(val postDistricts: Map<PostDistrict, RiskIndicator>)
