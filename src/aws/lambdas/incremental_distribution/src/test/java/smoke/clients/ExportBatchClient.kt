package smoke.clients

import batchZipCreation.Exposure
import org.http4k.client.JavaHttpClient
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.slf4j.LoggerFactory
import smoke.env.EnvConfig
import uk.nhs.nhsx.BatchExport
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ExportBatchClient(private val client: JavaHttpClient, private val config: EnvConfig) {

    private val logger = LoggerFactory.getLogger(ExportBatchClient::class.java)

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

    private fun getTwoHourlyTekExport(twoHourlyFilename: String): Exposure.TemporaryExposureKeyExport {
        logger.info("getLatestTwoHourlyTekExport: $twoHourlyFilename")

        val uri = "${config.diagnosisKeysDist2hourlyEndpoint}/$twoHourlyFilename"
        val request = Request(Method.GET, uri)

        val response = client(request)
            .requireStatusCode(Status.OK)
            .requireSignatureHeaders()
            .requireZipContentType()

        return BatchExport.tekExportFrom(response.body.stream)
    }
}