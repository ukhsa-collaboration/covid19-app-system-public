package uk.nhs.nhsx.diagnosiskeydist;

import batchZipCreation.Exposure;
import uk.nhs.nhsx.core.aws.xray.Tracing;

import java.io.FileInputStream;
import java.util.Base64;
import java.util.Date;
import uk.nhs.nhsx.diagnosiskeydist.agspec.ENIntervalNumber;

/**
 * Run with: mvn -f=src/aws/lambdas/incremental_distribution/pom.xml  exec:java -Dexec.mainClass="uk.nhs.nhsx.diagnosiskeydist.ExportFileExplorer"
 */
public class ExportFileExplorer {
    public static void main(String[] args) throws Exception {
        Tracing.disableXRayComplaintsForMainClasses();

        try (var in = new FileInputStream(args.length == 0 ? "export.bin" : args[0])) {
            in.read(new byte[DistributionService.EK_EXPORT_V1_HEADER.length()]);

            var export = Exposure.TemporaryExposureKeyExport.parseFrom(in);

            System.out.println("{");

            if (export.hasRegion()) {
                System.out.println("  \"region\": \"" + export.getRegion() + "\",");
            }

            if (export.hasBatchNum()) {
                System.out.println("  \"batchNum\": \"" + export.getBatchNum() + "\",");
            }

            if (export.hasBatchSize()) {
                System.out.println("  \"batchSize\": \"" + export.getBatchSize() + "\",");
            }

            if (export.hasStartTimestamp()) {
                System.out.println("  \"startTimestamp\": \"" + export.getStartTimestamp() + "\" /* " + new Date(export.getStartTimestamp() * 1000) + " */,");
            }

            if (export.hasEndTimestamp()) {
                System.out.println("  \"endTimestamp\": \"" + export.getEndTimestamp() + "\" /* " + new Date(export.getEndTimestamp() * 1000) + " */,");
            }

            System.out.println("  \"temporaryExposureKeys\": [");
            for (var i = export.getKeysList().iterator(); i.hasNext(); ) {
                var key = i.next();

                System.out.print("    {");
                System.out.print("\"key\":\"" + Base64.getEncoder().encodeToString(key.getKeyData().toByteArray()) + "\",");
                System.out.print("\"rollingStartNumber\":\"" + key.getRollingStartIntervalNumber() + "\" /* " + new ENIntervalNumber(key.getRollingStartIntervalNumber()).toTimestamp() + " */,");
                System.out.print("\"rollingPeriod\":\"" + key.getRollingPeriod() + "\" /* " + new ENIntervalNumber(key.getRollingStartIntervalNumber() + key.getRollingPeriod()).toTimestamp() + " */,");
                System.out.print("\"transmissionRiskLevel\":\"" + key.getTransmissionRiskLevel() + "\"");

                if (key.hasDaysSinceOnsetOfSymptoms()) {
                    System.out.print(",\"daysSinceOnsetOfSymptoms\":\"" + key.getDaysSinceOnsetOfSymptoms() + "\"");
                }

                if (i.hasNext()) {
                    System.out.println("},");
                }
                else {
                    System.out.println("}");
                }
            }
            System.out.println("  ]");

            System.out.println("}");
        }

        System.exit(0);
    }
}
