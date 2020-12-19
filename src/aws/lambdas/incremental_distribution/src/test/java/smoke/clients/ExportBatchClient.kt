package smoke.clients

import batchZipCreation.Exposure
import org.apache.logging.log4j.LogManager
import org.http4k.client.JavaHttpClient
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import smoke.env.EnvConfig
import uk.nhs.nhsx.testhelper.BatchExport
import java.lang.Thread.sleep
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ExportBatchClient(private val client: JavaHttpClient, private val config: EnvConfig) {

    private val logger = LogManager.getLogger(ExportBatchClient::class.java)

    fun getLatestTwoHourlyTekExport(): Exposure.TemporaryExposureKeyExport {
        val dateTime = LocalDateTime.ofInstant(Instant.now(), ZoneId.of("UTC"))
        val twoHourlyFilename = currentTwoHourlyWindowFilename(dateTime)
        return getTwoHourlyTekExport(twoHourlyFilename)
    }

    private fun currentTwoHourlyWindowFilename(dateTime: LocalDateTime): String {
        val twoHourlyWindow = if (dateTime.hour % 2 == 0) {
            dateTime.plusHours(2) // use next 2 hour window
        } else {
            dateTime.plusHours(1) // use current 2 hour window
        }

        val twoHourlyWindowStr = twoHourlyWindow
            .format(DateTimeFormatter.ofPattern("YYYYMMddHH"))
            .toString()

        return "$twoHourlyWindowStr.zip"
    }

    fun getTwoHourlyTekExport(twoHourlyFilename: String): Exposure.TemporaryExposureKeyExport {
        logger.info("getLatestTwoHourlyTekExport: $twoHourlyFilename")

        val uri = "${config.diagnosisKeysDist2hourlyEndpoint}/$twoHourlyFilename"
        val request = Request(Method.GET, uri)

        val response =
            getCloudfrontContentRetrying(request)
                .requireStatusCode(Status.OK)
                .requireSignatureHeaders()
                .requireZipContentType()

        return BatchExport.tekExportFrom(response.body.stream)
    }

    fun getDailyTekExport(dailyFilename: String): Exposure.TemporaryExposureKeyExport {
        logger.info("getDailyTekExport: $dailyFilename")

        val uri = "${config.diagnosisKeysDistributionDailyEndpoint}/$dailyFilename"
        val request = Request(Method.GET, uri)

        val response =
            getCloudfrontContentRetrying(request)
                .requireStatusCode(Status.OK)
                .requireSignatureHeaders()
                .requireZipContentType()

        return BatchExport.tekExportFrom(response.body.stream)
    }

    private fun getCloudfrontContentRetrying(request: Request): Response {
        var numberOfTries = 0L
        val maxRetries = 5
        val retryWaitDuration = Duration.ofSeconds(5)

        do {
            val response = client(request)
            if (response.status == Status.OK) {
                return response
            }
            logger.info("Failed to get cloudfront content. Got: ${response.status}")

            numberOfTries++
            val sleepDuration = Duration.ofSeconds(numberOfTries).plus(retryWaitDuration)

            if (numberOfTries <= maxRetries) {
                logger.info("Retrying in $sleepDuration...")
                sleep(sleepDuration.toMillis())
            }
        } while (numberOfTries <= maxRetries)

        throw IllegalStateException("Tried to fetch cloudfront content but failed after $maxRetries attempts")
    }
}