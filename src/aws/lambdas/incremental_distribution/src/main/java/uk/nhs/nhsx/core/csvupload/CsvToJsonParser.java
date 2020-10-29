package uk.nhs.nhsx.core.csvupload;

import uk.nhs.nhsx.highriskpostcodesupload.RiskLevel;

import java.util.Map;

public interface CsvToJsonParser {
    String toJson(String csv, Map<String, RiskLevel> riskLevels);

}
