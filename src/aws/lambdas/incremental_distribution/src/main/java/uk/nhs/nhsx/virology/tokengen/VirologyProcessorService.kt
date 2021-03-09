package uk.nhs.nhsx.virology.tokengen

import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.ExceptionThrown
import uk.nhs.nhsx.virology.CtaToken
import uk.nhs.nhsx.virology.CtaTokensAndUrlGenerationFailed
import uk.nhs.nhsx.virology.TestKit
import uk.nhs.nhsx.virology.VirologyService
import uk.nhs.nhsx.virology.result.TestEndDate
import uk.nhs.nhsx.virology.result.TestResult
import uk.nhs.nhsx.virology.result.VirologyTokenGenRequestV2
import uk.nhs.nhsx.virology.result.VirologyTokenGenResponse
import uk.nhs.nhsx.virology.tokengen.CtaProcessorResult.Error
import uk.nhs.nhsx.virology.tokengen.CtaProcessorResult.Success
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.function.Supplier
import java.util.stream.Collectors
import java.util.stream.IntStream

class VirologyProcessorService(
    private val virologyService: VirologyService,
    private val virologyProcessorStore: VirologyProcessorStore,
    private val clock: Supplier<Instant>,
    private val virologyProcessorExports: VirologyProcessorExports,
    private val maxRetryCount: Int,
    private val events: Events
) {
    private val executor = Executors.newFixedThreadPool(20)

    fun generateAndStoreTokens(event: CtaProcessorRequest): CtaProcessorResult {
        val start = clock.get()
        val tokens = generateTokens(event)
        if (tokens.isEmpty()) {
            return Error("No tokens generated/stored (empty list)")
        }
        val filename = start.toString().replace(":","")
        val ctaTokensZip = storeTokens(event, tokens, filename)
        val message = String.format("Generated %d tokens in %d seconds", tokens.size, Duration.between(start, Instant.now()).toSeconds())
        return Success(ctaTokensZip.filename, message)
    }

    fun generateStoreTokensAndReturnSignedUrl(requests: List<CtaTokenZipFileEntryRequest>, zipFilePassword: String,linkExpirationDate: ZonedDateTime): CtaTokenZipFile {
        val tokenFile = generateAndStoreTokensAsSingleZip(requests, zipFilePassword)
        if (tokenFile !is Success) {
            events.emit(javaClass, CtaTokensAndUrlGenerationFailed(requests, "Token file generation failed"))
            throw RuntimeException("Token file generation failed")
        }
        val filename = tokenFile.zipFilename
        val url = virologyProcessorStore.generateSignedURL(filename, Date.from(linkExpirationDate.toInstant()))

        if(url == null) {
            events.emit(javaClass,CtaTokensAndUrlGenerationFailed(requests, "Unable to retrieve signed URL from S3"))
            throw RuntimeException("Unable to retrieve signed URL from S3")
        }

        return CtaTokenZipFile(url, filename, zipFilePassword)
    }

    private fun generateAndStoreTokensAsSingleZip(requests: List<CtaTokenZipFileEntryRequest>, zipFilePassword: String): CtaProcessorResult {
        val start = clock.get()
        val zipFileEntries = requests.stream().map { (testResult, testEndDate, testKit, filename, numberOfTokens) ->
            CtaTokenZipFileEntry(
                testResult,
                testEndDate,
                filename,
                generateTokens(
                    CtaProcessorRequest(
                        testResult,
                        testEndDate,
                        testKit,
                        numberOfTokens
                    )
                )
            )
        }.collect(Collectors.toList())
        val filename = start.toString().replace(":", "")
        val ctaTokensZip = storeTokensAsSingleZip(zipFileEntries, filename, zipFilePassword)
        val message = String.format(
            "Generated %d token entries in %d seconds",
            zipFileEntries.size, Duration.between(start, Instant.now()).toSeconds()
        )
        return Success(ctaTokensZip.filename, message)
    }

    private fun storeTokensAsSingleZip(zipFileEntries: List<CtaTokenZipFileEntry>, zipFileName: String, zipFilePassword: String): CtaTokensZip {
        val ctaTokenCsvs = zipFileEntries
            .map { (testResult, endDate, filename, ctaTokens) ->
                val csvContent =
                    virologyProcessorExports.csvFrom(ctaTokens, testResult, endDate)
                CtaTokensCsv(filename, csvContent)
            }
        val zipFile = virologyProcessorExports.zipFrom(zipFileName, ctaTokenCsvs, zipFilePassword)
        val ctaTokensZip = CtaTokensZip(zipFileName, zipFile)
        virologyProcessorStore.storeZip(ctaTokensZip)
        return ctaTokensZip
    }

    private fun storeTokens(event: CtaProcessorRequest, tokens: List<CtaToken>, filename: String): CtaTokensZip {
        val csvContent = virologyProcessorExports.csvFrom(tokens, event.testResult, event.testEndDate)
        val ctaTokensCsv = CtaTokensCsv(filename, csvContent)
        virologyProcessorStore.storeCsv(ctaTokensCsv)
        val zipFile = virologyProcessorExports.zipFrom(filename, listOf(ctaTokensCsv))
        val ctaTokensZip = CtaTokensZip(filename, zipFile)
        virologyProcessorStore.storeZip(ctaTokensZip)
        return ctaTokensZip
    }

    private fun generateTokens(event: CtaProcessorRequest) = collectResultsFrom(
        createTokenGenFutures(
            event,
            VirologyTokenGenRequestV2(
                event.testEndDate,
                event.testResult,
                event.testKit
            )
        )
    )

    private fun createTokenGenFutures(
        event: CtaProcessorRequest,
        request: VirologyTokenGenRequestV2
    ) = IntStream.range(0, event.numberOfTokens)
        .mapToObj {
            CompletableFuture.supplyAsync({ generateTokenRetryingOnFailure(request) }, executor)
                .exceptionally { ex: Throwable ->
                    events.emit(javaClass, ExceptionThrown(ex, "Exception while generating CTA token"))
                    null
                }
        }
        .collect(Collectors.toList())

    private fun collectResultsFrom(futures: List<CompletableFuture<VirologyTokenGenResponse>>) =
        CompletableFuture.allOf(*futures.toTypedArray<CompletableFuture<*>>())
            .thenApply {
                futures
                    .stream()
                    .map { obj: CompletableFuture<VirologyTokenGenResponse> -> obj.join() }
                    .filter { obj: VirologyTokenGenResponse? -> Objects.nonNull(obj) }
                    .map(VirologyTokenGenResponse::ctaToken)
                    .collect(Collectors.toList())
            }
            .join()

    private fun generateTokenRetryingOnFailure(request: VirologyTokenGenRequestV2): VirologyTokenGenResponse {
        var numberOfTries = 0
        do {
            try {
                return virologyService.acceptTestResultGeneratingTokens(request)
            } catch (e: Exception) {
                numberOfTries++
            }
        } while (numberOfTries < maxRetryCount)
        throw RuntimeException("Generate cta token exceeded maximum of $maxRetryCount retries")
    }
}

data class CtaTokenZipFileEntry(val testResult: TestResult, val endDate: TestEndDate, val filename: String, val ctaTokens: List<CtaToken>)
data class CtaTokenZipFileEntryRequest (val testResult: TestResult, val testEndDate:TestEndDate, val testKit:TestKit, val filename:String, val numberOfTokens:Int)

