package uk.nhs.nhsx.analyticssubmission

import org.http4k.format.Jackson
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isEqualTo
import strikt.assertions.message
import uk.nhs.nhsx.analyticssubmission.model.PostDistrictPair

@ExtendWith(ApprovalTest::class)
class PostDistrictLaReplacerCsvParserTest {

    @Test
    fun `csv parsed successfully`() {
        val expectedMap = mapOf(
            PostDistrictPair("AL1", "E07000240") to PostDistrictPair("AL1", "E07000240"),
            PostDistrictPair("AL2", "E07000098") to PostDistrictPair("AL2_AL4_WD7", "E07000098")
        )

        val csv = """
            "Postcode_District_LAD_ID","LAD20CD","Merged_Postcode_District"
            "AL1_E07000240","E07000240","AL1"
            "AL2_E07000098","E07000098","AL2_AL4_WD7"
            """.trimIndent()

        expectThat(PostDistrictLaReplacerCsvParser.parse(csv)).isEqualTo(expectedMap)
    }

    @Test
    fun `invalid header row throws exception`() {
        val csvWithMissingUnderscore = """
            "PostcodeDistrictLADID","LAD20CD","MergedPostcodeDistrict"
            "AL1_E07000240","E07000240","AL1"
            "AL2_E07000098","E07000098","AL2_AL4_WD7"
            """.trimIndent()

        expectThrows<RuntimeException> { PostDistrictLaReplacerCsvParser.parse(csvWithMissingUnderscore) }
            .message.isEqualTo("Invalid header. Expected [Postcode_District_LAD_ID, LAD20CD, Merged_Postcode_District]")
    }

    @Test
    fun `invalid row throws exception`() {
        val csvWithInvalidPostcodeLADID = """
            "Postcode_District_LAD_ID","LAD20CD","Merged_Postcode_District"
            "AL1E07000240","E07000240","AL1"
            "AL2E07000098","E07000098","AL2_AL4_WD7"
            """.trimIndent()

        expectThrows<RuntimeException> { PostDistrictLaReplacerCsvParser.parse(csvWithInvalidPostcodeLADID) }
            .message.isEqualTo("Invalid data in row 2")
    }

    @Test
    fun `empty csv throws exception`() {
        val csv = "   \n "

        expectThrows<RuntimeException> { PostDistrictLaReplacerCsvParser.parse(csv) }
            .message.isEqualTo("Empty csv")
    }

    @Test
    fun `can parse CSV in resources`(approver: Approver) {
        val map = (javaClass.classLoader.getResource("analyticssubmission/PD_LA_to_MergedPD_LA.csv")
            ?.readText()
            ?.let { PostDistrictLaReplacerCsvParser.parse(it) }
            ?: error("Failed to read csv file for postcode/LA tuple replacement"))

        approver.assertApproved(Jackson.prettify(Jackson.asFormatString(map)))
    }
}
