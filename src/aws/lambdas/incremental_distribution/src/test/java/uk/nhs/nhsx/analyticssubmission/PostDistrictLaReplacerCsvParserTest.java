package uk.nhs.nhsx.analyticssubmission;

import org.junit.jupiter.api.Test;
import uk.nhs.nhsx.analyticssubmission.model.PostDistrictLADTuple;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.nhs.nhsx.analyticssubmission.PostDistrictLaReplacerCsvParser.*;

public class PostDistrictLaReplacerCsvParserTest {


    @Test
    public void csvParsedSuccessfully(){
        Map<PostDistrictLADTuple,PostDistrictLADTuple> expectedMap = new HashMap<>() {
            {
                put(new PostDistrictLADTuple("AL1", "E07000240"), new PostDistrictLADTuple("AL1", "E07000240"));
                put(new PostDistrictLADTuple("AL2", "E07000098"), new PostDistrictLADTuple("AL2_AL4_WD7", "E07000098"));
            }
        };

        String csv = "\"Postcode_District_LAD_ID\",\"LAD20CD\",\"Merged_Postcode_District\"\n" +
            "\"AL1_E07000240\",\"E07000240\",\"AL1\"\n" +
            "\"AL2_E07000098\",\"E07000098\",\"AL2_AL4_WD7\"";
        Map<PostDistrictLADTuple, PostDistrictLADTuple> parsedResult = parse(csv);
        assertThat(parsedResult, equalTo(expectedMap));
    }

    @Test
    public void invalidHeaderRowThrowsException(){
        String csvWithMissingUnderscore = "\"PostcodeDistrictLADID\",\"LAD20CD\",\"MergedPostcodeDistrict\"\n" +
            "\"AL1_E07000240\",\"E07000240\",\"AL1\"\n" +
            "\"AL2_E07000098\",\"E07000098\",\"AL2_AL4_WD7\"";
        assertThatThrownBy(() ->  parse(csvWithMissingUnderscore))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Invalid csv header: unexpected column headers");
    }

    @Test
    public void invalidRowThrowsException(){
        String csvWithInvalidPostcodeLADID = "\"Postcode_District_LAD_ID\",\"LAD20CD\",\"Merged_Postcode_District\"\n" +
            "\"AL1E07000240\",\"E07000240\",\"AL1\"\n" +
            "\"AL2E07000098\",\"E07000098\",\"AL2_AL4_WD7\"";
        assertThatThrownBy(() ->  parse(csvWithInvalidPostcodeLADID))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Invalid csv row: \n \"AL1E07000240\",\"E07000240\",\"AL1\"\n at line number: 1");
    }

    @Test
    public void emptyCsvThrowsException(){
        String csv = "   \n ";
        assertThatThrownBy(() ->  parse(csv))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Empty csv");
    }
}