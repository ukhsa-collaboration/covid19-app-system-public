package uk.nhs.nhsx.analyticssubmission

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.http4k.format.Jackson
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.nhs.nhsx.analyticssubmission.model.PostDistrictPair
import uk.nhs.nhsx.testhelper.http4k.assertApproved

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

        val parsedResult = PostDistrictLaReplacerCsvParser.parse(csv)
        assertThat(parsedResult).isEqualTo(expectedMap)
    }

    @Test
    fun `invalid header row throws exception`() {
        val csvWithMissingUnderscore = """
            "PostcodeDistrictLADID","LAD20CD","MergedPostcodeDistrict"
            "AL1_E07000240","E07000240","AL1"
            "AL2_E07000098","E07000098","AL2_AL4_WD7"
            """.trimIndent()

        assertThatThrownBy { PostDistrictLaReplacerCsvParser.parse(csvWithMissingUnderscore) }
            .isInstanceOf(RuntimeException::class.java)
            .hasMessage("Invalid header. Expected [Postcode_District_LAD_ID, LAD20CD, Merged_Postcode_District]")
    }

    @Test
    fun `invalid row throws exception`() {
        val csvWithInvalidPostcodeLADID = """
            "Postcode_District_LAD_ID","LAD20CD","Merged_Postcode_District"
            "AL1E07000240","E07000240","AL1"
            "AL2E07000098","E07000098","AL2_AL4_WD7"
            """.trimIndent()

        assertThatThrownBy { PostDistrictLaReplacerCsvParser.parse(csvWithInvalidPostcodeLADID) }
            .isInstanceOf(RuntimeException::class.java)
            .hasMessage("Invalid data in row 2")
    }

    @Test
    fun `empty csv throws exception`() {
        val csv = "   \n "
        assertThatThrownBy { PostDistrictLaReplacerCsvParser.parse(csv) }
            .isInstanceOf(RuntimeException::class.java)
            .hasMessage("Empty csv")
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
