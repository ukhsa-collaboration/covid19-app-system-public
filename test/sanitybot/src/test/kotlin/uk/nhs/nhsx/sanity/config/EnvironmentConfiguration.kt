package uk.nhs.nhsx.sanity.config

import com.fasterxml.jackson.databind.JsonNode
import org.http4k.format.Jackson
import java.io.File

class EnvironmentConfiguration(private val config: JsonNode) {

    fun configFor(lambda: DeployedApiResource, name: String) = lambda.configFrom(config, name)

    companion object {
        fun from(resource: File) = EnvironmentConfiguration(Jackson.asA(resource.reader().readText()))
    }
}
