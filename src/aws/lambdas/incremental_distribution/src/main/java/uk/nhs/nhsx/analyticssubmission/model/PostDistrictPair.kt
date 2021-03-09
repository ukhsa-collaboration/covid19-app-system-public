package uk.nhs.nhsx.analyticssubmission.model

data class PostDistrictPair(
    @JvmField val postDistrict: String?,
    @JvmField val localAuthorityId: String?
) {
    companion object {
        const val UNKNOWN = "UNKNOWN"
    }
}
