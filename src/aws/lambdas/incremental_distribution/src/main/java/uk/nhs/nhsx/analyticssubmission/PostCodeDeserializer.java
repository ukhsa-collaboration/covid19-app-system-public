package uk.nhs.nhsx.analyticssubmission;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.nhs.nhsx.diagnosiskeydist.ConcurrentExecution;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

public class PostCodeDeserializer {

    private static final Logger logger = LogManager.getLogger(ConcurrentExecution.class);
    private final static String csvMappingFileLocation = "analyticssubmission/merged_postcode_district_list.csv";
    private final static Map<String, String> postcodeMapping = getPostcodeMapping(csvMappingFileLocation);

    public static String mergeSmallPostcodes(String postcode){
        return mergeSmallPostcodes(postcode, postcodeMapping);
    }

    public static String mergeSmallPostcodes(String postcode, Map<String, String> mapping) {
        return Optional.ofNullable(mapping.get(postcode)).orElseGet(() -> {
            logger.info("Post district not found in mapping. Persisting post district as \"UNKNOWN\".");
            return "UNKNOWN";
        });
    }

    public static Map<String, String> getPostcodeMapping(String resource){
        String csv;
        try {
            csv = readFileAsString(resource);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException("Failed to read csv file for postcode mapping due to exception: " + e.toString());
        }
        return MergedPostDistrictCsvParser.parse(csv);
    }


    private static String readFileAsString(String resourceName) throws IOException, URISyntaxException {
        URL resource = PostCodeDeserializer.class.getClassLoader().getResource(resourceName);
        if (resource == null) {
            throw new RuntimeException("Could not find post code mapping csv file at " + resourceName);
        }
        return new String(Files.readAllBytes(Paths.get(resource.toURI())));
    }
}
