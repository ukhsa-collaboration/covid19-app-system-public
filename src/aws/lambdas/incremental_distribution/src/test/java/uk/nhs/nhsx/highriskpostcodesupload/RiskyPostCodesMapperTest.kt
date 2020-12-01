package uk.nhs.nhsx.highriskpostcodesupload

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.exceptions.ApiResponseException

class RiskyPostCodesMapperTest {

    private val mapper = RiskyPostCodesMapper(RiskyPostCodeTestData.tierMetadata)

    @Test
    fun `transforms data correctly both versions`() {
        val postDistricts = mapOf(
            "CODE1" to PostDistrictIndicators("H", "EN.Tier3"),
            "CODE2" to PostDistrictIndicators("M", "WA.Tier2"),
            "CODE3" to PostDistrictIndicators("L", "EN.Tier1")
        )
        val localAuthorities = mapOf(
            "A1" to LocalAuthorityIndicators("EN.Tier3"),
            "A2" to LocalAuthorityIndicators("WA.Tier2"),
            "A3" to LocalAuthorityIndicators("EN.Tier1")
        )
        val request = RiskyPostDistrictsRequest(
            postDistricts, localAuthorities
        )
        val result = mapper.mapOrThrow(request)

        assertThat(result).isEqualTo(
            RiskyPostCodesResult(
                RiskyPostCodesV1(mapOf("CODE1" to "H", "CODE2" to "M", "CODE3" to "L")),
                RiskyPostCodesV2(
                    mapOf("CODE1" to "EN.Tier3", "CODE2" to "WA.Tier2", "CODE3" to "EN.Tier1"),
                    mapOf("A1" to "EN.Tier3", "A2" to "WA.Tier2", "A3" to "EN.Tier1"),
                    RiskyPostCodeTestData.tierMetadata
                )
            )
        )
    }

    @Test
    fun `transforms handling empty collections`() {
        val request = RiskyPostDistrictsRequest(emptyMap(), emptyMap())
        val result = mapper.mapOrThrow(request)

        assertThat(result).isEqualTo(
            RiskyPostCodesResult(
                RiskyPostCodesV1(emptyMap()),
                RiskyPostCodesV2(emptyMap(), emptyMap(), RiskyPostCodeTestData.tierMetadata)
            )
        )
    }

    @Test
    fun `transforms handling post district indicators and no local authorities indicators`() {
        val postDistricts = mapOf(
            "CODE1" to PostDistrictIndicators("H", "EN.Tier3"),
            "CODE2" to PostDistrictIndicators("M", "WA.Tier2"),
            "CODE3" to PostDistrictIndicators("L", "EN.Tier1")
        )
        val request = RiskyPostDistrictsRequest(postDistricts, emptyMap())

        val result = mapper.mapOrThrow(request)

        assertThat(result).isEqualTo(
            RiskyPostCodesResult(
                RiskyPostCodesV1(mapOf("CODE1" to "H", "CODE2" to "M", "CODE3" to "L")),
                RiskyPostCodesV2(
                    mapOf("CODE1" to "EN.Tier3", "CODE2" to "WA.Tier2", "CODE3" to "EN.Tier1"),
                    emptyMap(),
                    RiskyPostCodeTestData.tierMetadata
                )
            )
        )
    }

    @Test
    fun `transforms handling empty post district indicators and local authorities indicators`() {
        val localAuthorities = mapOf(
            "A1" to LocalAuthorityIndicators("EN.Tier3"),
            "A2" to LocalAuthorityIndicators("WA.Tier2"),
            "A3" to LocalAuthorityIndicators("EN.Tier1")
        )
        val request = RiskyPostDistrictsRequest(emptyMap(), localAuthorities)

        val result = mapper.mapOrThrow(request)

        assertThat(result).isEqualTo(
            RiskyPostCodesResult(
                RiskyPostCodesV1(emptyMap()),
                RiskyPostCodesV2(
                    emptyMap(),
                    mapOf("A1" to "EN.Tier3", "A2" to "WA.Tier2", "A3" to "EN.Tier1"),
                    RiskyPostCodeTestData.tierMetadata
                )
            )
        )
    }

    @Test
    fun `throws if post districts contains an empty risk`() {
        val postDistricts = mapOf(
            "CODE1" to PostDistrictIndicators("", "EN.Tier3"),
            "CODE2" to PostDistrictIndicators("M", "WA.Tier2"),
            "CODE3" to PostDistrictIndicators("L", "EN.Tier1")
        )
        val localAuthorities = mapOf(
            "A1" to LocalAuthorityIndicators("EN.Tier3"),
            "A2" to LocalAuthorityIndicators("WA.Tier2"),
            "A3" to LocalAuthorityIndicators("EN.Tier1")
        )
        val request = RiskyPostDistrictsRequest(postDistricts, localAuthorities)

        assertThatThrownBy { mapper.mapOrThrow(request) }
            .isInstanceOf(ApiResponseException::class.java)
            .hasMessage("validation error: Invalid risk indicator:  for post district: CODE1")
    }

    @Test
    fun `throws if post districts contains null risk`() {
        val postDistricts = mapOf(
            "CODE1" to PostDistrictIndicators("H", "EN.Tier3"),
            "CODE2" to PostDistrictIndicators("M", "WA.Tier2"),
            "CODE3" to PostDistrictIndicators(null, "EN.Tier1")
        )
        val localAuthorities = mapOf(
            "A1" to LocalAuthorityIndicators("EN.Tier3"),
            "A2" to LocalAuthorityIndicators("WA.Tier2"),
            "A3" to LocalAuthorityIndicators("EN.Tier1")
        )
        val request = RiskyPostDistrictsRequest(postDistricts, localAuthorities)

        assertThatThrownBy { mapper.mapOrThrow(request) }
            .isInstanceOf(ApiResponseException::class.java)
            .hasMessage("validation error: Invalid risk indicator: null for post district: CODE3")
    }

    @Test
    fun `throws if post districts contains an empty tier`() {
        val postDistricts = mapOf(
            "CODE1" to PostDistrictIndicators("H", "EN.Tier3"),
            "CODE2" to PostDistrictIndicators("M", ""),
            "CODE3" to PostDistrictIndicators("L", "EN.Tier1")
        )
        val localAuthorities = mapOf(
            "A1" to LocalAuthorityIndicators("EN.Tier3"),
            "A2" to LocalAuthorityIndicators("WA.Tier2"),
            "A3" to LocalAuthorityIndicators("EN.Tier1")
        )
        val request = RiskyPostDistrictsRequest(postDistricts, localAuthorities)

        assertThatThrownBy { mapper.mapOrThrow(request) }
            .isInstanceOf(ApiResponseException::class.java)
            .hasMessage("validation error: Invalid tier indicator:  for post district: CODE2")
    }

    @Test
    fun `throws if post districts contains a null tier`() {
        val postDistricts = mapOf(
            "CODE1" to PostDistrictIndicators("H", "EN.Tier3"),
            "CODE2" to PostDistrictIndicators("M", "WA.Tier2"),
            "CODE3" to PostDistrictIndicators("L", null)
        )
        val localAuthorities = mapOf(
            "A1" to LocalAuthorityIndicators("EN.Tier3"),
            "A2" to LocalAuthorityIndicators("WA.Tier2"),
            "A3" to LocalAuthorityIndicators("EN.Tier1")
        )
        val request = RiskyPostDistrictsRequest(postDistricts, localAuthorities)

        assertThatThrownBy { mapper.mapOrThrow(request) }
            .isInstanceOf(ApiResponseException::class.java)
            .hasMessage("validation error: Invalid tier indicator: null for post district: CODE3")
    }

    @Test
    fun `throws if post districts contains null key`() {
        val postDistricts = mapOf(
            "CODE1" to PostDistrictIndicators("H", "EN.Tier3"),
            null to PostDistrictIndicators("M", "WA.Tier2"),
            "CODE3" to PostDistrictIndicators("L", "EN.Tier1")
        )
        val localAuthorities = mapOf(
            "A1" to LocalAuthorityIndicators("EN.Tier3"),
            "A2" to LocalAuthorityIndicators("WA.Tier2"),
            "A3" to LocalAuthorityIndicators("EN.Tier1")
        )
        val request = RiskyPostDistrictsRequest(postDistricts, localAuthorities)

        assertThatThrownBy { mapper.mapOrThrow(request) }
            .isInstanceOf(ApiResponseException::class.java)
            .hasMessage("validation error: Invalid post district - no value")
    }

    @Test
    fun `throws if post districts contains empty key`() {
        val postDistricts = mapOf(
            "CODE1" to PostDistrictIndicators("H", "EN.Tier3"),
            " " to PostDistrictIndicators("M", "WA.Tier2"),
            "CODE3" to PostDistrictIndicators("L", "EN.Tier1")
        )
        val localAuthorities = mapOf(
            "A1" to LocalAuthorityIndicators("EN.Tier3"),
            "A2" to LocalAuthorityIndicators("WA.Tier2"),
            "A3" to LocalAuthorityIndicators("EN.Tier1")
        )
        val request = RiskyPostDistrictsRequest(postDistricts, localAuthorities)

        assertThatThrownBy { mapper.mapOrThrow(request) }
            .isInstanceOf(ApiResponseException::class.java)
            .hasMessage("validation error: Invalid post district - no value")
    }

    @Test
    fun `throws if post district larger than 20 characters`() {
        val postDistricts = mapOf(
            "CODE1" to PostDistrictIndicators("H", "EN.Tier3"),
            "123456789012345678901" to PostDistrictIndicators("M", "WA.Tier2"),
            "CODE3" to PostDistrictIndicators("L", "EN.Tier1")
        )
        val localAuthorities = mapOf(
            "A1" to LocalAuthorityIndicators("EN.Tier3"),
            "A2" to LocalAuthorityIndicators("WA.Tier2"),
            "A3" to LocalAuthorityIndicators("EN.Tier1")
        )
        val request = RiskyPostDistrictsRequest(postDistricts, localAuthorities)

        assertThatThrownBy { mapper.mapOrThrow(request) }
            .isInstanceOf(ApiResponseException::class.java)
            .hasMessage("validation error: Invalid post district longer than 20 characters: 123456789012345678901")
    }

    @Test
    fun `throws if local authorities contains an empty key`() {
        val postDistricts = mapOf(
            "CODE1" to PostDistrictIndicators("H", "EN.Tier3"),
            "CODE2" to PostDistrictIndicators("M", "WA.Tier2"),
            "CODE3" to PostDistrictIndicators("L", "EN.Tier1")
        )
        val localAuthorities = mapOf(
            "A1" to LocalAuthorityIndicators("EN.Tier3"),
            " " to LocalAuthorityIndicators("WA.Tier2"),
            "A3" to LocalAuthorityIndicators("EN.Tier1")
        )
        val request = RiskyPostDistrictsRequest(postDistricts, localAuthorities)

        assertThatThrownBy { mapper.mapOrThrow(request) }
            .isInstanceOf(ApiResponseException::class.java)
            .hasMessage("validation error: Invalid local authority - no value")
    }

    @Test
    fun `throws if local authorities contains a null key`() {
        val postDistricts = mapOf(
            "CODE1" to PostDistrictIndicators("H", "EN.Tier3"),
            "CODE2" to PostDistrictIndicators("M", "WA.Tier2"),
            "CODE3" to PostDistrictIndicators("L", "EN.Tier1")
        )
        val localAuthorities = mapOf(
            "A1" to LocalAuthorityIndicators("EN.Tier3"),
            null to LocalAuthorityIndicators("WA.Tier2"),
            "A3" to LocalAuthorityIndicators("EN.Tier1")
        )
        val request = RiskyPostDistrictsRequest(postDistricts, localAuthorities)

        assertThatThrownBy { mapper.mapOrThrow(request) }
            .isInstanceOf(ApiResponseException::class.java)
            .hasMessage("validation error: Invalid local authority - no value")
    }

    @Test
    fun `throws if local authorities contains an empty tier`() {
        val postDistricts = mapOf(
            "CODE1" to PostDistrictIndicators("H", "EN.Tier3"),
            "CODE2" to PostDistrictIndicators("M", "WA.Tier2"),
            "CODE3" to PostDistrictIndicators("L", "EN.Tier1")
        )
        val localAuthorities = mapOf(
            "A1" to LocalAuthorityIndicators("EN.Tier3"),
            "A2" to LocalAuthorityIndicators("WA.Tier2"),
            "A3" to LocalAuthorityIndicators(" ")
        )
        val request = RiskyPostDistrictsRequest(postDistricts, localAuthorities)

        assertThatThrownBy { mapper.mapOrThrow(request) }
            .isInstanceOf(ApiResponseException::class.java)
            .hasMessage("validation error: Invalid tier indicator:   for local authority: A3")
    }

    @Test
    fun `throws if local authorities contains a null tier`() {
        val postDistricts = mapOf(
            "CODE1" to PostDistrictIndicators("H", "EN.Tier3"),
            "CODE2" to PostDistrictIndicators("M", "WA.Tier2"),
            "CODE3" to PostDistrictIndicators("L", "EN.Tier1")
        )
        val localAuthorities = mapOf(
            "A1" to LocalAuthorityIndicators("EN.Tier3"),
            "A2" to LocalAuthorityIndicators("WA.Tier2"),
            "A3" to LocalAuthorityIndicators(null)
        )
        val request = RiskyPostDistrictsRequest(postDistricts, localAuthorities)

        assertThatThrownBy { mapper.mapOrThrow(request) }
            .isInstanceOf(ApiResponseException::class.java)
            .hasMessage("validation error: Invalid tier indicator: null for local authority: A3")
    }

    @Test
    fun `converts v2 post district indicators into raw csv`() {
        val postDistricts = mapOf(
            "CODE1" to PostDistrictIndicators("H", "EN.Tier3"),
            "CODE2" to PostDistrictIndicators("M", "WA.Tier2"),
            "CODE3" to PostDistrictIndicators("L", "EN.Tier1")
        )
        val localAuthorities = mapOf(
            "A1" to LocalAuthorityIndicators("EN.Tier3"),
            "A2" to LocalAuthorityIndicators("WA.Tier2"),
            "A3" to LocalAuthorityIndicators("EN.Tier1")
        )
        val request = RiskyPostDistrictsRequest(
            postDistricts, localAuthorities
        )

        val result = mapper.convertToAnalyticsCsv(request)

        assertThat(result).isEqualTo("""
            # postal_district_code, risk_indicator, tier_indicator
            "CODE1", "H", "EN.Tier3"
            "CODE2", "M", "WA.Tier2"
            "CODE3", "L", "EN.Tier1"
        """.trimIndent())
    }

    @Test
    fun `converts emtpy v2 post district indicators into raw csv`() {
        val request = RiskyPostDistrictsRequest(mapOf(), mapOf())

        val result = mapper.convertToAnalyticsCsv(request)

        assertThat(result).isEqualTo("""# postal_district_code, risk_indicator, tier_indicator""")
    }

}