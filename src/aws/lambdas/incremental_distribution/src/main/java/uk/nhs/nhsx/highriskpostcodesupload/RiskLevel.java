package uk.nhs.nhsx.highriskpostcodesupload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class RiskLevel {

    public final String colorScheme;
    public final Map<String, String> name;
    public final Map<String, String> heading;
    public final Map<String, String> content;
    public final Map<String, String> linkTitle;
    public final Map<String, String> linkUrl;

    @JsonCreator
    public RiskLevel(
        @JsonProperty("colorScheme") String colorScheme,
        @JsonProperty("name") Map<String, String> name,
        @JsonProperty("heading") Map<String, String> heading,
        @JsonProperty("content") Map<String, String> content,
        @JsonProperty("linkTitle") Map<String, String> linkTitle,
        @JsonProperty("linkUrl") Map<String, String> linkUrl
    ) {
        this.colorScheme = colorScheme;
        this.name = name;
        this.heading = heading;
        this.content = content;
        this.linkTitle = linkTitle;
        this.linkUrl = linkUrl;
    }

}
