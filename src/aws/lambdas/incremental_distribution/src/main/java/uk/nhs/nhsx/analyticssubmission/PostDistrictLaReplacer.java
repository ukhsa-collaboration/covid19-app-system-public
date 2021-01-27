package uk.nhs.nhsx.analyticssubmission;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.nhs.nhsx.analyticssubmission.model.PostDistrictLADTuple;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;

public class PostDistrictLaReplacer {

    private static final Logger logger = LogManager.getLogger(PostDistrictLaReplacer.class);
    private static final String csvMappingFileLocation = "analyticssubmission/PD_LA_to_MergedPD_LA.csv";
    private static final Map<PostDistrictLADTuple, PostDistrictLADTuple> postDistrictLAMapping = getPostDistrictLAMapping(csvMappingFileLocation);
    private static final String UNKNOWN = "UNKNOWN";

    public static PostDistrictLADTuple replacePostDistrictLA(String postDistrict, String localAuthority) {
        return replacePostDistrictLA(
            postDistrict,
            Optional.ofNullable(localAuthority).orElse(UNKNOWN),
            postDistrictLAMapping
        );
    }

    public static PostDistrictLADTuple replacePostDistrictLA(String postDistrict,
                                                             String localAuthority,
                                                             Map<PostDistrictLADTuple, PostDistrictLADTuple> mapping) {
        return Optional.ofNullable(mapping.get(new PostDistrictLADTuple(postDistrict, localAuthority)))
            .orElseGet(() -> Optional.ofNullable(mapping.get(new PostDistrictLADTuple(postDistrict, "UNKNOWN")))
                .orElseGet(() -> {
                    logger.info(format("Post district LA tuple not found in mapping. Persisting post district and localAuthority as \"%s\"", UNKNOWN));
                    return new PostDistrictLADTuple(UNKNOWN, UNKNOWN);
                }));
    }

    public static Map<PostDistrictLADTuple, PostDistrictLADTuple> getPostDistrictLAMapping(String resource) {
        try {
            return PostDistrictLaReplacerCsvParser.parse(readFileAsString(resource));
        } catch (Exception e) {
            throw new RuntimeException("Failed to read csv file for postcode/LA tuple replacement due to exception: " + e.toString());
        }
    }

    private static String readFileAsString(String resourceName) throws IOException, URISyntaxException {
        URL resource = PostDistrictLaReplacer.class.getClassLoader().getResource(resourceName);
        if (resource == null) {
            throw new RuntimeException("Could not find post code mapping csv file at " + resourceName);
        }
        return Files.readString(Paths.get(resource.toURI()), UTF_8);
    }
}
