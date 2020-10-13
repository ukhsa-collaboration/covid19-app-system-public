package uk.nhs.nhsx.virology.tokengen;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.nhs.nhsx.virology.CtaToken;
import uk.nhs.nhsx.virology.VirologyService;
import uk.nhs.nhsx.virology.result.VirologyTokenGenRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.stream.IntStream.range;

public class VirologyProcessorService {

    private final VirologyService virologyService;
    private final VirologyProcessorStore virologyProcessorStore;
    private final Supplier<Instant> clock;

    private final ExecutorService executor = Executors.newFixedThreadPool(20);

    private static final Logger logger = LogManager.getLogger(VirologyProcessorService.class);

    public VirologyProcessorService(VirologyService virologyService,
                                    VirologyProcessorStore virologyProcessorStore,
                                    Supplier<Instant> clock) {
        this.virologyService = virologyService;
        this.virologyProcessorStore = virologyProcessorStore;
        this.clock = clock;
    }

    public void generateAndStoreTokens(CtaProcessorEvent event) {
        logger.info("Generating tokens from event: {}", event);
        var start = clock.get();
        var tokens = generateTokens(event);

        if (tokens.isEmpty()) {
            logger.info("Skipping store tokens (empty)");
            return;
        }

        var filename = start.toString();

        var csvContent = VirologyProcessorExports.csvFrom(tokens, event.testResult, event.testEndDate);
        var ctaTokensCsv = new CtaTokensCsv(filename, csvContent);
        virologyProcessorStore.storeCsv(ctaTokensCsv);

        var zipFile = VirologyProcessorExports.zipFrom(filename, ctaTokensCsv);
        var ctaTokensZip = new CtaTokensZip(filename, zipFile);
        virologyProcessorStore.storeZip(ctaTokensZip);

        logger.info(
            "Generated {} tokens in {} seconds",
            tokens.size(),
            Duration.between(start, Instant.now()).toSeconds()
        );
    }

    private List<CtaToken> generateTokens(CtaProcessorEvent event) {
        var virologyTokenGenRequest = new VirologyTokenGenRequest(event.testResult, event.testEndDate);

        var completableFutures =
            range(0, event.numberOfTokens)
                .mapToObj(it ->
                    supplyAsync(() -> virologyService.acceptTestResultGeneratingTokens(virologyTokenGenRequest), executor)
                        .exceptionally(ex -> {
                            logger.error("Exception while generating CTA token : ", ex);
                            return null;
                        })
                )
                .collect(Collectors.toList());

        return allOf(completableFutures.toArray(new CompletableFuture[0]))
            .thenApply(
                future -> completableFutures
                    .stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .map(it -> CtaToken.of(it.ctaToken))
                    .collect(Collectors.toList())
            )
            .join();
    }
}
