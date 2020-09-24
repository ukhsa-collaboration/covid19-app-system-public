package uk.nhs.nhsx.activationsubmission.persist;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import uk.nhs.nhsx.activationsubmission.ActivationCode;
import uk.nhs.nhsx.core.SystemClock;
import uk.nhs.nhsx.core.aws.xray.Tracing;
import uk.nhs.nhsx.core.exceptions.ExceptionPrinting;
import uk.nhs.nhsx.core.random.crockford.CrockfordDammRandomStringGenerator;

import java.io.*;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class InsertActivationCodesMain {

    @SuppressWarnings("FieldMayBeFinal")
    public static class CommandLine {
        @Parameter(names = "--batch", description = "Name of batch")
        private String batch;

        @Parameter(names = "--number", description = "Number of codes to add")
        private int number = 0;

        @Parameter(names = "--prefix", description = "Table prefix")
        private String prefix;

        @Parameter(names = "--output", description = "File to output to (default stdout)")
        private String output;

        @Parameter(names = "--input", description = "Load codes from file")
        private String input;

        @Parameter(names = "--region", description = "AWS Region (default eu-west-2")
        private String region = "eu-west-2";

        @Parameter(names = "--generate-only", description = "Only generate the codes, don't load them")
        private boolean generateOnly = false;

        @Parameter(names = "--help", help = true, description = "Show help")
        private boolean help = false;

        public TableNamingStrategy naming() {
            return prefix == null ? TableNamingStrategy.DIRECT : TableNamingStrategy.PREFIX.apply(prefix);
        }
    }

    interface CodeSupplier {
        Stream<ActivationCode> generate();
    }

    public static class StreamCodeSupplier implements CodeSupplier {

        private final BufferedReader reader;
        private final CrockfordDammRandomStringGenerator.DammChecksum checksum;

        public StreamCodeSupplier(InputStream input, CrockfordDammRandomStringGenerator.DammChecksum checksum) {
            reader = new BufferedReader(new InputStreamReader(input));
            this.checksum = checksum;
        }

        @Override
        public Stream<ActivationCode> generate() {
            return reader.lines()
                .filter(checksum::validate)
                .map(ActivationCode::of);
        }
    }

    public static class GeneratedCodeSupplier implements CodeSupplier {

        private final CrockfordDammRandomStringGenerator generator;
        private final int count;

        public GeneratedCodeSupplier(CrockfordDammRandomStringGenerator generator, int count) {
            this.generator = generator;
            this.count = count;
        }

        @Override
        public Stream<ActivationCode> generate() {
            return Stream.iterate(0, i -> i)
                .map(i -> generator.generate())
                .map(ActivationCode::of)
                .distinct()
                .limit(count);
        }
    }

    public static void generate(CodeSupplier codes, PrintStream output) {
        codes.generate().forEach(
            code -> output.println(code.value)
        );
    }

    public static void insert(CodeSupplier codes, ActivationCodeBatchName batchName,
                              ExecutorService executor,
                              DynamoDBActivationCodes persistedCodes,
                              LinkedTransferQueue<ActivationCode> inserted,
                              Consumer<Exception> reporter
    ) {
        codes.generate().forEach(
            code ->
                executor.submit(() -> {
                    try {
                        if (persistedCodes.insert(batchName, code)) {
                            inserted.put(code);
                        }
                    } catch (Exception e) {
                        reporter.accept(e);
                    }
                })
        );
    }

    /**
     * Usage: <br/>
     * GENERATING: --generate-only --number 750000 --output /tmp/codes1.txt<br/>
     * LOADING: --input /tmp/codes1.txt --batch codes-1 --prefix &lt;deployment-prefix&gt; <br/>
     * GENERATE & LOAD (for testing) : --batch monday --number 50 --prefix &lt;deployment-prefix&gt; <br/>
     */

    public static void main(String[] args) throws FileNotFoundException {

        Tracing.disableXRayComplaintsForMainClasses();

        CommandLine commandLine = new CommandLine();
        JCommander commander = JCommander.newBuilder().addObject(commandLine).build();
        commander.parse(args);

        if (commandLine.help) {
            commander.usage();
            System.exit(1);
        }

        AmazonDynamoDB db = AmazonDynamoDBClientBuilder
            .standard()
            .withRegion(commandLine.region)
            .build();

        Supplier<Instant> clock = SystemClock.CLOCK;

        Instant started = clock.get();
        CodeSupplier codeSupplier = commandLine.input == null ?
            new GeneratedCodeSupplier(new CrockfordDammRandomStringGenerator(), commandLine.number) :
            new StreamCodeSupplier(new FileInputStream(commandLine.input), CrockfordDammRandomStringGenerator.checksum());

        PrintStream output = outputFile(commandLine.output);
        try {

            if (commandLine.generateOnly) {
                generate(codeSupplier, output);
            } else {

                ActivationCodeBatchName batchName = ActivationCodeBatchName.of(commandLine.batch);
                DynamoDBActivationCodes dbActivationCodes = new DynamoDBActivationCodes(db, commandLine.naming(), clock);

                ThreadPoolExecutor executor = new ThreadPoolExecutor(75, 75, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());

                LinkedTransferQueue<ActivationCode> inserted = new LinkedTransferQueue<>();

                AtomicInteger errorCount = new AtomicInteger();

                Consumer<Exception> reporter = e -> System.err.printf("%5d %s%n", errorCount.incrementAndGet(), ExceptionPrinting.describeCauseOf(e));

                insert(codeSupplier, batchName, executor, dbActivationCodes, inserted, reporter);

                System.err.println("Waiting for database ....");

                executor.shutdown();
                while (!executor.isTerminated()) {
                    try {
                        Duration estimate = Duration.ofMinutes(12).multipliedBy(executor.getQueue().size()).dividedBy(500_000);
                        System.err.println("Estimate remaining " + estimate);
                        executor.awaitTermination(10, TimeUnit.SECONDS);
                    } catch (InterruptedException ignored) {
                    }
                }

                System.err.println("");
                System.err.println("Completed...");

                int insertedCount = inserted.size();

                inserted.forEach((c) -> output.println(c.value));

                System.err.printf("Inserted %d, and there were %d errors (may be less than you expected, because of duplicates. If zero, then maybe you reloaded a file.\n", insertedCount, errorCount.get());

            }
        } finally {
            System.err.println("Completed in " + Duration.between(started, clock.get()));
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
            return new PrintStream(new FileOutputStream(output));
        }
    }
}
