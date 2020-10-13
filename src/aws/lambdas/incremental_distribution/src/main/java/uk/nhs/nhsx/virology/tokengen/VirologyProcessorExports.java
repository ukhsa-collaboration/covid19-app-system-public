package uk.nhs.nhsx.virology.tokengen;

import org.apache.commons.codec.Charsets;
import uk.nhs.nhsx.virology.CtaToken;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipOutputStream;

import static java.util.stream.Collectors.joining;
import static uk.nhs.nhsx.core.UncheckedException.uncheckedGet;
import static uk.nhs.nhsx.core.UncheckedRun.uncheckedRun;
import static uk.nhs.nhsx.diagnosiskeydist.keydistribution.KeyFileUtility.addFileToZip;
import static uk.nhs.nhsx.diagnosiskeydist.keydistribution.KeyFileUtility.writeToFile;

public class VirologyProcessorExports {

    public static String csvFrom(List<CtaToken> tokens, String testResult, String testEndDate) {
        return tokens.stream()
            .map(it -> it.value + ", " + testResult + ", " + testEndDate)
            .collect(joining("\n", "cta token, test result, test end date\n", ""));
    }

    public static File zipFrom(String filename, CtaTokensCsv ctaTokensCsv) {
        var csvFile = uncheckedGet(() -> File.createTempFile(filename, ".csv"));
        uncheckedRun(() -> writeToFile(csvFile, ctaTokensCsv.content.getBytes(Charsets.UTF_8)));
        return uncheckedGet(() -> zipFile(filename, filename + ".csv", csvFile));
    }

    private static File zipFile(String zipFileName, String zipEntryFileName, File zipEntryFile) throws IOException {
        var zipFile = uncheckedGet(() -> File.createTempFile(zipFileName, ".zip"));
        try (var outputStream = new FileOutputStream(zipFile)) {
            try (var zipOut = new ZipOutputStream(outputStream)) {
                addFileToZip(zipOut, zipEntryFileName, zipEntryFile);
            }
        }
        return zipFile;
    }
}
