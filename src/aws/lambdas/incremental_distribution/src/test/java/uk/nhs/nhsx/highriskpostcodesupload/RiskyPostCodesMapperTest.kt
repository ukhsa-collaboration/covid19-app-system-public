package uk.nhs.nhsx.highriskpostcodesupload

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.domain.LocalAuthority
import uk.nhs.nhsx.domain.PostDistrict
import uk.nhs.nhsx.domain.PostDistrictIndicators
import uk.nhs.nhsx.domain.TierIndicator
import uk.nhs.nhsx.domain.RiskIndicator.HIGH
import uk.nhs.nhsx.domain.RiskIndicator.LOW
import uk.nhs.nhsx.domain.RiskIndicator.MEDIUM

class RiskyPostCodesMapperTest {

    private val mapper = RiskyPostCodesMapper(RiskyPostCodeTestData.tierMetadata)

    @Test
    fun `transforms data correctly both versions`() {
        val postDistricts = mapOf(
            PostDistrict.of("CODE1") to PostDistrictIndicators(HIGH, TierIndicator.of("EN.Tier3")),
            PostDistrict.of("CODE2") to PostDistrictIndicators(MEDIUM, TierIndicator.of("WA.Tier2")),
            PostDistrict.of("CODE3") to PostDistrictIndicators(LOW, TierIndicator.of("EN.Tier1"))
        )
        val localAuthorities = mapOf(
            LocalAuthority.of("A1") to LocalAuthorityIndicators(TierIndicator.of("EN.Tier3")),
            LocalAuthority.of("A2") to LocalAuthorityIndicators(TierIndicator.of("WA.Tier2")),
            LocalAuthority.of("A3") to LocalAuthorityIndicators(TierIndicator.of("EN.Tier1"))
        )
        val request = RiskyPostDistrictsRequest(
            postDistricts, localAuthorities
        )
        val result = mapper.mapOrThrow(request)

        assertThat(result).isEqualTo(
            RiskyPostCodesResult(
                RiskyPostCodesV1(
                    mapOf(
                        PostDistrict.of("CODE1") to HIGH,
                        PostDistrict.of("CODE2") to MEDIUM,
                        PostDistrict.of("CODE3") to LOW
                    )
                ),
                RiskyPostCodesV2(
                    mapOf(
                        PostDistrict.of("CODE1") to TierIndicator.of("EN.Tier3"),
                        PostDistrict.of("CODE2") to TierIndicator.of("WA.Tier2"),
                        PostDistrict.of("CODE3") to TierIndicator.of("EN.Tier1")
                    ),
                    mapOf(
                        LocalAuthority.of("A1") to TierIndicator.of("EN.Tier3"),
                        LocalAuthority.of("A2") to TierIndicator.of("WA.Tier2"),
                        LocalAuthority.of("A3") to TierIndicator.of("EN.Tier1")
                    ),
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
            PostDistrict.of("CODE1") to PostDistrictIndicators(HIGH, TierIndicator.of("EN.Tier3")),
            PostDistrict.of("CODE2") to PostDistrictIndicators(MEDIUM, TierIndicator.of("WA.Tier2")),
            PostDistrict.of("CODE3") to PostDistrictIndicators(LOW, TierIndicator.of("EN.Tier1"))
        )
        val request = RiskyPostDistrictsRequest(postDistricts, emptyMap())

        val result = mapper.mapOrThrow(request)

        assertThat(result).isEqualTo(
            RiskyPostCodesResult(
                RiskyPostCodesV1(
                    mapOf(
                        PostDistrict.of("CODE1") to HIGH,
                        PostDistrict.of("CODE2") to MEDIUM,
                        PostDistrict.of("CODE3") to LOW
                    )
                ),
                RiskyPostCodesV2(
                    mapOf(
                        PostDistrict.of("CODE1") to TierIndicator.of("EN.Tier3"),
                        PostDistrict.of("CODE2") to TierIndicator.of("WA.Tier2"),
                        PostDistrict.of("CODE3") to TierIndicator.of("EN.Tier1")
                    ),
                    emptyMap(),
                    RiskyPostCodeTestData.tierMetadata
                )
            )
        )
    }

    @Test
    fun `transforms handling empty post district indicators and local authorities indicators`() {
        val localAuthorities = mapOf(
            LocalAuthority.of("A1") to LocalAuthorityIndicators(TierIndicator.of("EN.Tier3")),
            LocalAuthority.of("A2") to LocalAuthorityIndicators(TierIndicator.of("WA.Tier2")),
            LocalAuthority.of("A3") to LocalAuthorityIndicators(TierIndicator.of("EN.Tier1"))
        )
        val request = RiskyPostDistrictsRequest(emptyMap(), localAuthorities)

        val result = mapper.mapOrThrow(request)

        assertThat(result).isEqualTo(
            RiskyPostCodesResult(
                RiskyPostCodesV1(emptyMap()),
                RiskyPostCodesV2(
                    emptyMap(),
                    mapOf(
                        LocalAuthority.of("A1") to TierIndicator.of("EN.Tier3"),
                        LocalAuthority.of("A2") to TierIndicator.of("WA.Tier2"),
                        LocalAuthority.of("A3") to TierIndicator.of("EN.Tier1")
                    ),
                    RiskyPostCodeTestData.tierMetadata
                )
            )
        )
    }

    @Test
    fun `throws if post districts contains empty key`() {
        assertThatThrownBy { PostDistrict.of(" ") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `throws if post district larger than 20 characters`() {
        assertThatThrownBy { PostDistrict.of("123456789012345678901") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `throws if local authorities contains an empty key`() {
        assertThatThrownBy { LocalAuthority.of(" ") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `throws if local authorities contains an empty tier`() {
        assertThatThrownBy { TierIndicator.of(" ") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `converts v2 post district indicators into raw csv`() {
        val postDistricts = mapOf(
            PostDistrict.of("CODE1") to PostDistrictIndicators(HIGH, TierIndicator.of("EN.Tier3")),
            PostDistrict.of("CODE2") to PostDistrictIndicators(MEDIUM, TierIndicator.of("WA.Tier2")),
            PostDistrict.of("CODE3") to PostDistrictIndicators(LOW, TierIndicator.of("EN.Tier1"))
        )
        val localAuthorities = mapOf(
            LocalAuthority.of("A1") to LocalAuthorityIndicators(TierIndicator.of("EN.Tier3")),
            LocalAuthority.of("A2") to LocalAuthorityIndicators(TierIndicator.of("WA.Tier2")),
            LocalAuthority.of("A3") to LocalAuthorityIndicators(TierIndicator.of("EN.Tier1"))
        )
        val request = RiskyPostDistrictsRequest(
            postDistricts, localAuthorities
        )

        val result = mapper.convertToAnalyticsCsv(request)

        assertThat(result).isEqualTo(
            """
            # postal_district_code, risk_indicator, tier_indicator
            "CODE1", "H", "EN.Tier3"
            "CODE2", "M", "WA.Tier2"
            "CODE3", "L", "EN.Tier1"
        """.trimIndent()
        )
    }

    @Test
    fun `converts emtpy v2 post district indicators into raw csv`() {
        val request = RiskyPostDistrictsRequest(mapOf(), mapOf())

        val result = mapper.convertToAnalyticsCsv(request)

        assertThat(result).isEqualTo("# postal_district_code, risk_indicator, tier_indicator")
    }
}
