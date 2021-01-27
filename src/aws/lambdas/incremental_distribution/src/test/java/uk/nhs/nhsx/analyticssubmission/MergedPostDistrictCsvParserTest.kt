package uk.nhs.nhsx.analyticssubmission

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test

class MergedPostDistrictCsvParserTest {

    @Test
    fun `csv parsed successfully`() {
        val expectedMap = mapOf(
            "AB10" to "AB10",
            "AB11" to "AB11",
            "AB12" to "AB12",
            "AB13" to "AB13_AB14",
            "AB14" to "AB13_AB14",
            "AB15" to "AB15",
            "AB33" to "AB33_AB35_AB36",
            "AB34" to "AB34",
            "AB35" to "AB33_AB35_AB36",
            "AB36" to "AB33_AB35_AB36"
        )
        val csv =
            "\"Postcode_District\",\"Merged_Postcode_District\",\"Estimated_Population\"\n\"AB10\",\"AB10\",22604\n\"AB11\",\"AB11\",21084\n\"AB12\",\"AB12\",27839\n\"AB13\",\"AB13_AB14\",7909\n\"AB14\",\"AB13_AB14\",7909\n\"AB15\",\"AB15\",38064\n\"AB33\",\"AB33_AB35_AB36\",8712\n\"AB34\",\"AB34\",5763\n\"AB35\",\"AB33_AB35_AB36\",8712\n\"AB36\",\"AB33_AB35_AB36\",8712\n"
        val parsedResult = MergedPostDistrictCsvParser.parse(csv)
        assertThat(parsedResult, CoreMatchers.equalTo(expectedMap))
    }

    @Test
    fun `invalid header row throws exception`() {
        val csv =
            "\"Merged_Postcode_District\",\"Estimated_Population\"\n\"AB10\",\"AB10\",22604\n\"AB11\",\"AB11\",21084\n\"AB12\",\"AB12\",27839\n\"AB13\",\"AB13_AB14\",7909\n\"AB14\",\"AB13_AB14\",7909\n\"AB15\",\"AB15\",38064\n\"AB33\",\"AB33_AB35_AB36\",8712\n\"AB34\",\"AB34\",5763\n\"AB35\",\"AB33_AB35_AB36\",8712\n\"AB36\",\"AB33_AB35_AB36\",8712\n"
        assertThatThrownBy { MergedPostDistrictCsvParser.parse(csv) }
            .isInstanceOf(RuntimeException::class.java)
            .hasMessage("Invalid csv header")
    }

    @Test
    fun `invalid row throws exception`() {
        val csv =
            "\"Postcode_District\",\"Merged_Postcode_District\",\"Estimated_Population\"\n\"AB10\",\"AB10\",22604\n\"AB11\",\"AB11\",21084\n\"AB12\",\"AB12\",27839\n\"AB13\",\"AB13_AB14\",7909\n\"AB13_AB14\",7909\n\"AB15\",\"AB15\",38064\n\"AB33\",\"AB33_AB35_AB36\",8712\n\"AB34\",\"AB34\",5763\n\"AB35\",\"AB33_AB35_AB36\",8712\n\"AB36\",\"AB33_AB35_AB36\",8712\n"
        assertThatThrownBy { MergedPostDistrictCsvParser.parse(csv) }
            .isInstanceOf(RuntimeException::class.java)
            .hasMessage("Invalid csv row: \n \"AB13_AB14\",7909\n at line number: 5")
    }

    @Test
    fun `empty csv throws exception`() {
        val csv = "   \n "
        assertThatThrownBy { MergedPostDistrictCsvParser.parse(csv) }
            .isInstanceOf(RuntimeException::class.java)
            .hasMessage("Empty csv")
    }
}