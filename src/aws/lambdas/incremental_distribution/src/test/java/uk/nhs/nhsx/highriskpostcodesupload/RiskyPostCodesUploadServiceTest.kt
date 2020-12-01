package uk.nhs.nhsx.highriskpostcodesupload

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verifySequence
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.SystemObjectMapper
import uk.nhs.nhsx.core.aws.cloudfront.AwsCloudFront

class RiskyPostCodesUploadServiceTest {

    private val postDistrictsAndLocalAuthoritiesJson = """{
      "postDistricts": {
        "CODE1": {
          "riskIndicator": "H",
          "tierIndicator": "EN.Tier3"
        },
        "CODE2": {
          "riskIndicator": "M",
          "tierIndicator": "EN.Tier2"
        },
        "CODE3": {
          "riskIndicator": "L",
          "tierIndicator": "EN.Tier1"
        }
      },
      "localAuthorities": {
        "A1": {
          "tierIndicator": "EN.Tier3"
        },
        "A2": {
          "tierIndicator": "EN.Tier1"
        },
        "A3": {
          "tierIndicator": "EN.Tier2"
        }
      }
    }""".trimIndent()

    private val postDistrictsOnlyJson = """{
      "postDistricts": {
        "CODE1": {
          "riskIndicator": "H",
          "tierIndicator": "EN.Tier3"
        },
        "CODE2": {
          "riskIndicator": "M",
          "tierIndicator": "EN.Tier2"
        },
        "CODE3": {
          "riskIndicator": "L",
          "tierIndicator": "EN.Tier1"
        }
      },
      "localAuthorities": {}
    }""".trimIndent()

    private val localAuthoritiesOnlyJson = """{
      "postDistricts": {},
      "localAuthorities": {
        "A1": {
          "tierIndicator": "EN.Tier3"
        },
        "A2": {
          "tierIndicator": "EN.Tier1"
        },
        "A3": {
          "tierIndicator": "EN.Tier2"
        }
      }
    }""".trimIndent()

    private val rawCsv = """
        # postal_district_code, risk_indicator, tier_indicator
        "CODE1", "H", "EN.Tier3"
        "CODE2", "M", "EN.Tier2"
        "CODE3", "L", "EN.Tier1"
    """.trimIndent()

    private val emptyRawCsv = """# postal_district_code, risk_indicator, tier_indicator"""

    private val awsCloudFront = mockk<AwsCloudFront> {
        every { invalidateCache(any(), any()) } just Runs
    }

    private val postDistrictsV1Slot = slot<String>()
    private val postDistrictsV2Slot = slot<String>()

    private val riskLevelsMetadata = mapOf(
        "EN.Tier1" to mapOf(
            "colorScheme" to "yellow",
            "name" to mapOf("en" to "name"),
            "heading" to mapOf("en" to "heading"),
            "content" to mapOf("en" to "content"),
            "linkTitle" to mapOf("en" to "link-title"),
            "linkUrl" to mapOf("en" to "link-url")
        ),
        "EN.Tier2" to mapOf(
            "colorScheme" to "neutral",
            "name" to mapOf("en" to "name"),
            "heading" to mapOf("en" to "heading"),
            "content" to mapOf("en" to "content"),
            "linkTitle" to mapOf("en" to "link-title"),
            "linkUrl" to mapOf("en" to "link-url")
        ),
        "EN.Tier3" to mapOf(
            "colorScheme" to "red",
            "name" to mapOf("en" to "name"),
            "heading" to mapOf("en" to "heading"),
            "content" to mapOf("en" to "content"),
            "linkTitle" to mapOf("en" to "link-title"),
            "linkUrl" to mapOf("en" to "link-url")
        )
    )

    private val persistence = mockk<RiskyPostCodesPersistence> {
        every { retrievePostDistrictRiskLevels() } returns riskLevelsMetadata
        every { uploadToBackup(any()) } just Runs
        every { uploadToRaw(any()) } just Runs
        every { uploadPostDistrictsVersion1(capture(postDistrictsV1Slot)) } just Runs
        every { uploadPostDistrictsVersion2(capture(postDistrictsV2Slot)) } just Runs
    }

    private val service = RiskyPostCodesUploadService(persistence, awsCloudFront, "cloudfront-dist-id", "cloudfront-invalidation-pattern")

    @Test
    fun `uploads json objects`() {
        service.upload(postDistrictsAndLocalAuthoritiesJson)

        verifyMocksInvoked(postDistrictsAndLocalAuthoritiesJson, rawCsv)

        val v1Captured = toMap(postDistrictsV1Slot.captured)
        assertThat(v1Captured).isEqualTo(
            mapOf(
                "postDistricts" to mapOf(
                    "CODE1" to "H",
                    "CODE2" to "M",
                    "CODE3" to "L"
                )
            )
        )

        val v2Captured = toMap(postDistrictsV2Slot.captured)
        assertThat(v2Captured).isEqualTo(
            mapOf(
                "postDistricts" to mapOf(
                    "CODE1" to "EN.Tier3",
                    "CODE2" to "EN.Tier2",
                    "CODE3" to "EN.Tier1",
                ),
                "localAuthorities" to mapOf(
                    "A1" to "EN.Tier3",
                    "A2" to "EN.Tier1",
                    "A3" to "EN.Tier2",
                ),
                "riskLevels" to riskLevels()
            )
        )
    }

    @Test
    fun `uploads handling no post districts`() {
        service.upload(localAuthoritiesOnlyJson)

        verifyMocksInvoked(localAuthoritiesOnlyJson, emptyRawCsv)

        val v1Captured = toMap(postDistrictsV1Slot.captured)
        assertThat(v1Captured).isEqualTo(
            mapOf(
                "postDistricts" to emptyMap<String, Any>()
            )
        )

        val v2Captured = toMap(postDistrictsV2Slot.captured)
        assertThat(v2Captured).isEqualTo(
            mapOf(
                "postDistricts" to mapOf(),
                "localAuthorities" to mapOf(
                    "A1" to "EN.Tier3",
                    "A2" to "EN.Tier1",
                    "A3" to "EN.Tier2",
                ),
                "riskLevels" to riskLevels()
            )
        )
    }

    @Test
    fun `uploads handling no local authorities`() {
        service.upload(postDistrictsOnlyJson)

        verifyMocksInvoked(postDistrictsOnlyJson, rawCsv)

        val v1Captured = toMap(postDistrictsV1Slot.captured)
        assertThat(v1Captured).isEqualTo(
            mapOf(
                "postDistricts" to mapOf(
                    "CODE1" to "H",
                    "CODE2" to "M",
                    "CODE3" to "L"
                )
            )
        )

        val v2Captured = toMap(postDistrictsV2Slot.captured)
        assertThat(v2Captured).isEqualTo(
            mapOf(
                "postDistricts" to mapOf(
                    "CODE1" to "EN.Tier3",
                    "CODE2" to "EN.Tier2",
                    "CODE3" to "EN.Tier1",
                ),
                "localAuthorities" to mapOf(),
                "riskLevels" to riskLevels()
            )
        )
    }

    private fun verifyMocksInvoked(json: String, csv: String) {
        verifySequence {
            persistence.retrievePostDistrictRiskLevels()
            persistence.uploadToBackup(json)
            persistence.uploadToRaw(csv)
            persistence.uploadPostDistrictsVersion1(any())
            persistence.uploadPostDistrictsVersion2(any())
            awsCloudFront.invalidateCache("cloudfront-dist-id", "cloudfront-invalidation-pattern")
        }
    }

    private fun riskLevels(): Map<String, Map<String, Any>> = mapOf(
        "EN.Tier1" to mapOf(
            "colorScheme" to "yellow",
            "name" to mapOf("en" to "name"),
            "content" to mapOf("en" to "content"),
            "heading" to mapOf("en" to "heading"),
            "linkTitle" to mapOf("en" to "link-title"),
            "linkUrl" to mapOf("en" to "link-url")
        ),
        "EN.Tier2" to mapOf(
            "colorScheme" to "neutral",
            "name" to mapOf("en" to "name"),
            "content" to mapOf("en" to "content"),
            "heading" to mapOf("en" to "heading"),
            "linkTitle" to mapOf("en" to "link-title"),
            "linkUrl" to mapOf("en" to "link-url")
        ),
        "EN.Tier3" to mapOf(
            "colorScheme" to "red",
            "name" to mapOf("en" to "name"),
            "content" to mapOf("en" to "content"),
            "heading" to mapOf("en" to "heading"),
            "linkTitle" to mapOf("en" to "link-title"),
            "linkUrl" to mapOf("en" to "link-url")
        )
    )

    private fun toMap(value: String): Map<*, *> = SystemObjectMapper.MAPPER.readValue(value, Map::class.java)
}