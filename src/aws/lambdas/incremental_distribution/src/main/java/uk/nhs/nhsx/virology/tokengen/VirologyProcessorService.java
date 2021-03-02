package uk.nhs.nhsx.virology.tokengen;

import uk.nhs.nhsx.core.events.Events;
import uk.nhs.nhsx.core.events.ExceptionThrown;
import uk.nhs.nhsx.virology.CtaToken;
import uk.nhs.nhsx.virology.TestKit;
import uk.nhs.nhsx.virology.VirologyService;
import uk.nhs.nhsx.virology.result.VirologyTokenGenRequest;
import uk.nhs.nhsx.virology.result.VirologyTokenGenResponse;

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
    private final int maxRetryCount;
    private final Events events;

    private final ExecutorService executor = Executors.newFixedThreadPool(20);

    public VirologyProcessorService(VirologyService virologyService,
                                    VirologyProcessorStore virologyProcessorStore,
                                    Supplier<Instant> clock,
                                    int maxRetryCount,
                                    Events events) {
        this.virologyService = virologyService;
        this.virologyProcessorStore = virologyProcessorStore;
        this.clock = clock;
        this.maxRetryCount = maxRetryCount;
        this.events = events;
    }

    public CtaProcessorResult generateAndStoreTokens(CtaProcessorRequest event) {
        var start = clock.get();
        var tokens = generateTokens(event);

        if (tokens.isEmpty()) {
            return new CtaProcessorResult.Error("No tokens generated/stored (empty list)");
        }

        var filename = start.toString();
        var ctaTokensZip = storeTokens(event, tokens, filename);

        var message = String.format(
            "Generated %d tokens in %d seconds",
            tokens.size(), Duration.between(start, Instant.now()).toSeconds()
        );

        return new CtaProcessorResult.Success(ctaTokensZip.filename, message);
    }

    private CtaTokensZip storeTokens(CtaProcessorRequest event, List<CtaToken> tokens, String filename) {
        var csvContent = VirologyProcessorExports.csvFrom(tokens, event.testResult, event.testEndDate);
        var ctaTokensCsv = new CtaTokensCsv(filename, csvContent);
        virologyProcessorStore.storeCsv(ctaTokensCsv);

        var zipFile = VirologyProcessorExports.zipFrom(filename, ctaTokensCsv);
        var ctaTokensZip = new CtaTokensZip(filename, zipFile);
        virologyProcessorStore.storeZip(ctaTokensZip);
        return ctaTokensZip;
    }

    private List<CtaToken> generateTokens(CtaProcessorRequest event) {
        var request = new VirologyTokenGenRequest(event.testResult, event.testEndDate, TestKit.valueOf(event.testKit));
        var futures = createTokenGenFutures(event, request);
        return collectResultsFrom(futures);
    }

    private List<CompletableFuture<VirologyTokenGenResponse>> createTokenGenFutures(CtaProcessorRequest event,
                                                                                    VirologyTokenGenRequest request) {
        return range(0, event.numberOfTokens)
            .mapToObj(it ->
                supplyAsync(() -> generateTokenRetryingOnFailure(request), executor)
                    .exceptionally(ex -> {
                        events.emit(getClass(), new ExceptionThrown<>(ex, "Exception while generating CTA token"));
                        return null;
                    })
            )
            .collect(Collectors.toList());
    }

    private List<CtaToken> collectResultsFrom(List<CompletableFuture<VirologyTokenGenResponse>> futures) {
        return allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(
                future -> futures
                    .stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .map(it -> CtaToken.of(it.ctaToken))
                    .collect(Collectors.toList())
            )
            .join();
    }

    private VirologyTokenGenResponse generateTokenRetryingOnFailure(VirologyTokenGenRequest request) {
        var numberOfTries = 0;
        do {
            try {
                return virologyService.acceptTestResultGeneratingTokens(request);
            } catch (Exception e) {
                numberOfTries++;
            }
        } while (numberOfTries < maxRetryCount);

        throw new RuntimeException("Generate cta token exceeded maximum of " + maxRetryCount + " retries");
    }
}
