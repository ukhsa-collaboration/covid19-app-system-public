package uk.nhs.nhsx.core.signature;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import uk.nhs.nhsx.core.StandardSigning;
import uk.nhs.nhsx.core.SystemClock;
import uk.nhs.nhsx.core.SystemObjectMapper;
import uk.nhs.nhsx.core.aws.s3.ByteArraySource;
import uk.nhs.nhsx.core.aws.ssm.AwsSsmParameters;
import uk.nhs.nhsx.core.aws.ssm.ParameterName;
import uk.nhs.nhsx.core.aws.ssm.Parameters;
import uk.nhs.nhsx.core.aws.xray.Tracing;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * This class is used during the deployment to generate signatures for the S3 static files
 */
public class DistributionSignatureMain {

    public static class CommandLine {
        @Parameter(names = "--input", required = true, description = "Path To Input Fie")
        private String input;

        @Parameter(names = "--output", description = "File to output to (default stdout)")
        private String output;

        @Parameter(names = "--ssm-key-id", required = true, description = "Name to look up in SSM to get Key Id")
        private String ssmKeyId;

        @Parameter(names = "--help", help = true, description = "Show help")
        private boolean help = false;
    }

    public static void main(String[] args) throws IOException {

        Tracing.disableXRayComplaintsForMainClasses();

        CommandLine commandLine = new DistributionSignatureMain.CommandLine();
        JCommander commander = JCommander.newBuilder().addObject(commandLine).build();
        commander.parse(args);

        if (commandLine.help) {
            commander.usage();
            System.exit(1);
        }

        Parameters parameters = new AwsSsmParameters();

        RFC2616DatedSigner signer = StandardSigning.datedSigner(SystemClock.CLOCK, parameters, ParameterName.of(commandLine.ssmKeyId));

        DatedSignature signature = signer.sign(new DistributionSignature(ByteArraySource.fromFile(new File(commandLine.input))));

        Map<String, String> map = new HashMap<>();

        SigningHeaders.fromDatedSignature(signature).forEach(h -> map.put(h.asS3MetaName(), h.getValue()));

        PrintStream output = outputFile(commandLine.output);
        try {
            SystemObjectMapper.MAPPER.writeValue(output, map);
        } finally {
            if (output != System.out) {
                output.close();
            }
        }
        System.exit(0);
    }

    private static PrintStream outputFile(String output) throws FileNotFoundException {
        if (output == null) {
            return System.out;
        } else {
            return new PrintStream(new FileOutputStream(output), false, UTF_8);
        }
    }
}
