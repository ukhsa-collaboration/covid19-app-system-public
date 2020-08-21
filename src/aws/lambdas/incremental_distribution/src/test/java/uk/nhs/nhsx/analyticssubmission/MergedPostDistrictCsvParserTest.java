package uk.nhs.nhsx.analyticssubmission;

import org.junit.Test;
import java.util.HashMap;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;

public class MergedPostDistrictCsvParserTest {

    @Test
    public void csvParsedSuccessfully(){
        Map<String, String> expectedMap = new HashMap<String, String>(){
            {
                put("AB10", "AB10");
                put("AB11", "AB11");
                put("AB12", "AB12");
                put("AB13", "AB13_AB14");
                put("AB14", "AB13_AB14");
                put("AB15", "AB15");
                put("AB33", "AB33_AB35_AB36");
                put("AB34", "AB34");
                put("AB35", "AB33_AB35_AB36");
                put("AB36", "AB33_AB35_AB36");
            }};
        String csv = "\"Postcode_District\",\"Merged_Postcode_District\",\"Estimated_Population\"\n\"AB10\",\"AB10\",22604\n\"AB11\",\"AB11\",21084\n\"AB12\",\"AB12\",27839\n\"AB13\",\"AB13_AB14\",7909\n\"AB14\",\"AB13_AB14\",7909\n\"AB15\",\"AB15\",38064\n\"AB33\",\"AB33_AB35_AB36\",8712\n\"AB34\",\"AB34\",5763\n\"AB35\",\"AB33_AB35_AB36\",8712\n\"AB36\",\"AB33_AB35_AB36\",8712\n";
        Map<String, String> parsedResult = MergedPostDistrictCsvParser.parse(csv);
        assertThat(parsedResult, equalTo(expectedMap));
    }

    @Test
    public void invalidHeaderRowThrowsException(){
        String csv = "\"Merged_Postcode_District\",\"Estimated_Population\"\n\"AB10\",\"AB10\",22604\n\"AB11\",\"AB11\",21084\n\"AB12\",\"AB12\",27839\n\"AB13\",\"AB13_AB14\",7909\n\"AB14\",\"AB13_AB14\",7909\n\"AB15\",\"AB15\",38064\n\"AB33\",\"AB33_AB35_AB36\",8712\n\"AB34\",\"AB34\",5763\n\"AB35\",\"AB33_AB35_AB36\",8712\n\"AB36\",\"AB33_AB35_AB36\",8712\n";
        assertThatThrownBy(() ->  MergedPostDistrictCsvParser.parse(csv))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Invalid csv header");
    }

    @Test
    public void invalidRowThrowsException(){
        String csv = "\"Postcode_District\",\"Merged_Postcode_District\",\"Estimated_Population\"\n\"AB10\",\"AB10\",22604\n\"AB11\",\"AB11\",21084\n\"AB12\",\"AB12\",27839\n\"AB13\",\"AB13_AB14\",7909\n\"AB13_AB14\",7909\n\"AB15\",\"AB15\",38064\n\"AB33\",\"AB33_AB35_AB36\",8712\n\"AB34\",\"AB34\",5763\n\"AB35\",\"AB33_AB35_AB36\",8712\n\"AB36\",\"AB33_AB35_AB36\",8712\n";
        assertThatThrownBy(() ->  MergedPostDistrictCsvParser.parse(csv))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Invalid csv row: \n \"AB13_AB14\",7909\n at line number: 5");
    }

    @Test
    public void emptyCsvThrowsException(){
        String csv = "   \n ";
        assertThatThrownBy(() ->  MergedPostDistrictCsvParser.parse(csv))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Empty csv");
    }
}