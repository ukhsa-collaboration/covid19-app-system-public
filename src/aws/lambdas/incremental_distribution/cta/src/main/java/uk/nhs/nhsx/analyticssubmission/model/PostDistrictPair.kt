package uk.nhs.nhsx.analyticssubmission.model

data class PostDistrictPair(
    val postDistrict: String,
    val localAuthorityId: String?
) {
    companion object {
        const val UNKNOWN = "UNKNOWN"
    }
}
