package uk.nhs.nhsx.analyticssubmission;

import uk.nhs.nhsx.analyticssubmission.model.PostDistrictLADTuple;
import uk.nhs.nhsx.core.events.Events;
import uk.nhs.nhsx.core.events.InfoEvent;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;

public class PostDistrictLaReplacer {

    private static final String csvMappingFileLocation = "analyticssubmission/PD_LA_to_MergedPD_LA.csv";
    private static final Map<PostDistrictLADTuple, PostDistrictLADTuple> postDistrictLAMapping = getPostDistrictLAMapping(csvMappingFileLocation);
    private static final String UNKNOWN = "UNKNOWN";

    public static PostDistrictLADTuple replacePostDistrictLA(String postDistrict, String localAuthority, Events events) {
        return replacePostDistrictLA(
            postDistrict,
            Optional.ofNullable(localAuthority).orElse(UNKNOWN),
            postDistrictLAMapping,
            events
        );
    }

    public static PostDistrictLADTuple replacePostDistrictLA(String postDistrict,
                                                             String localAuthority,
                                                             Map<PostDistrictLADTuple,
                                                                 PostDistrictLADTuple> mapping,
                                                             Events events) {
        return Optional.ofNullable(mapping.get(new PostDistrictLADTuple(postDistrict, localAuthority)))
            .orElseGet(() -> Optional.ofNullable(mapping.get(new PostDistrictLADTuple(postDistrict, "UNKNOWN")))
                .orElseGet(() -> {
                    events.emit(PostDistrictLaReplacer.class, new InfoEvent("Post district LA tuple not found in mapping. Persisting post district and localAuthority as " + UNKNOWN));
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
