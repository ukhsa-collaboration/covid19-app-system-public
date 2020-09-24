package smoke.env

import org.apache.logging.log4j.LogManager
import uk.nhs.nhsx.core.Jackson
import uk.nhs.nhsx.core.aws.xray.Tracing
import java.io.File
import java.io.FileNotFoundException

object SmokeTests {

    private val logger = LogManager.getLogger(SmokeTests::class.java)

    init {
        Tracing.disableXRayComplaintsForMainClasses()
    }

    fun loadConfig(): EnvConfig =
        deserializeOrThrow(loadConfigAsString())

    private fun loadConfigAsString(): String {
        val envConfigFileName = loadConfigFileName()
        return loadConfigFileFor(envConfigFileName)
    }

    private fun loadConfigFileName(): String {
        val envName = "SMOKE_TEST_CONFIG"
        return System.getenv(envName)
            ?: throw IllegalStateException("Env var not set: $envName=<config file>")
    }

    private fun loadConfigFileFor(envConfigFileName: String): String {
        try {
            logger.info("Using config file: $envConfigFileName")
            return loadFile(envConfigFileName).readText()
        } catch (e: FileNotFoundException) {
            throw IllegalStateException(
                "Config file not found: ${envConfigFileName}, " +
                "run following commands to generate it: rake gen:config:<tgt_env>"
            )
        }
    }

    fun loadStaticContent(fileName: String): String =
        loadFile("../../../../src/static/$fileName").readText()

    private fun loadFile(filePath: String): File = File(filePath)

    private inline fun <reified T> deserializeOrThrow(value: String): T {
        return Jackson.deserializeMaybe(value, T::class.java)
            .orElseThrow {
                IllegalStateException(
                    "Unable to deserialize configuration file, " +
                        "check generated config file against ${EnvConfig::class.simpleName} class"
                )
            }
    }
}

