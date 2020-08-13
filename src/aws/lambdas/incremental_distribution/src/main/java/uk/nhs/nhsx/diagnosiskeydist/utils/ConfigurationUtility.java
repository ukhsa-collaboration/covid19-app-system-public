package uk.nhs.nhsx.diagnosiskeydist.utils;

public class ConfigurationUtility {
    public static final String SUBMISSION_JSON_BUCKET_NAME = System.getenv("SUBMISSION_BUCKET_NAME");
    public static final String OUTPUT_STORE = System.getenv("OUTPUT_BUCKET");
    public static final String WORKGROUP = System.getenv("WORKGROUP");
}
