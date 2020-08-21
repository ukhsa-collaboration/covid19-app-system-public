package uk.nhs.nhsx.analyticssubmission;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;

public class PostCodeDeserializerTest {

    Map<String, String> mapping = new HashMap<>(){
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

    @Test
    public void SmallPostDistrictMergedCorrectly(){
        String postDistrict = "AB13";
        String expectedMergedPostcode = "AB13_AB14";
        String deserializedPostcode = PostCodeDeserializer.mergeSmallPostcodes(postDistrict, mapping);
        assertThat(deserializedPostcode, equalTo(expectedMergedPostcode));
    }

    @Test
    public void LargePostDistrictUnchanged(){
        String postDistrict = "AB10";
        String expectedPostcode = "AB10";
        String deserializedPostcode = PostCodeDeserializer.mergeSmallPostcodes(postDistrict, mapping);
        assertThat(deserializedPostcode, equalTo(expectedPostcode));
    }

    @Test
    public void PostDistrictNotFoundInMappingReturnsUnknown(){
        String postDistrict = "F4KEP0STC0DE";
        String expectedPostcode = "UNKNOWN";
        String deserializedPostcode = PostCodeDeserializer.mergeSmallPostcodes(postDistrict, mapping);
        assertThat(deserializedPostcode, equalTo(expectedPostcode));
    }

    @Test
    public void MappingCsvNotFoundInMappingThrowsException(){
        assertThatThrownBy(() ->  PostCodeDeserializer.getPostcodeMapping( "no_file_here.csv"))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Could not find post code mapping csv file at no_file_here.csv");
    }

}
