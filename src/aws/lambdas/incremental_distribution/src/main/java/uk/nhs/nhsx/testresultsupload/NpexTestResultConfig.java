package uk.nhs.nhsx.testresultsupload;

public class NpexTestResultConfig {

    public final String submissionTokensTable;
    public final String testResultsTable;
    public final String testOrdersTable;

    public NpexTestResultConfig(String submissionTokensTable, String testResultsTable, String testOrdersTable) {
        this.submissionTokensTable = submissionTokensTable;
        this.testResultsTable = testResultsTable;
        this.testOrdersTable = testOrdersTable;
    }
}
