package smoke.actors

import org.http4k.core.ContentType
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.core.Status.Companion.ACCEPTED
import smoke.env.EnvConfig

class RiskParties(private val http: HttpHandler, private val envConfig: EnvConfig) {

    fun uploadsRiskyPostcodes(csv: String) {
        http(Request(POST, envConfig.riskyPostDistrictsUploadEndpoint)
            .header("Authorization", envConfig.authHeaders.highRiskPostCodeUpload)
            .header("Content-Type", ContentType("text/csv").value)
            .body(csv))
            .requireStatusCode(ACCEPTED)
            .requireBodyText("successfully uploaded")
    }

    fun uploadsRiskyVenues(csv: String) {
        http(Request(POST, envConfig.riskyVenuesUploadEndpoint)
            .header("Authorization", envConfig.authHeaders.highRiskVenuesCodeUpload)
            .header("Content-Type", ContentType("text/csv").value)
            .body(csv))
            .requireStatusCode(Status.ACCEPTED)
            .requireBodyText("successfully uploaded")
    }
}