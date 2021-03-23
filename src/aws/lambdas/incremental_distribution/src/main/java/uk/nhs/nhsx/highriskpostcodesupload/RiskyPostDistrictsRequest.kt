package uk.nhs.nhsx.highriskpostcodesupload

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.length

class PostDistrict private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<PostDistrict>(::PostDistrict, (1..20).length.let { v -> { v(it.trim()) } })
}

class LocalAuthority private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<LocalAuthority>(::LocalAuthority)
}

data class RiskyPostDistrictsRequest(
    val postDistricts: Map<PostDistrict, PostDistrictIndicators>,
    val localAuthorities: Map<LocalAuthority, LocalAuthorityIndicators>
)
