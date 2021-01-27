package smoke.env

import org.apache.logging.log4j.LogManager
import uk.nhs.nhsx.core.Jackson
import uk.nhs.nhsx.core.aws.xray.Tracing
import java.io.File
import java.io.FileNotFoundException

object SmokeTests {

    private val logger = LogManager.getLogger(SmokeTests::class.java)

    val sleeper = Sleeper.Companion.Real

    init {
        Tracing.disableXRayComplaintsForMainClasses()
    }

    fun loadConfig(): EnvConfig = deserializeOrThrow(loadConfigAsString())

    private fun loadConfigAsString() = loadConfigFileFor(loadConfigFileName())

    private fun loadConfigFileName() =
        System.getenv("SMOKE_TEST_CONFIG") ?: "../../../../out/gen/config/test_config_branch.json"

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

    fun loadStaticContent(fileName: String): String = loadFile("../../../../src/static/$fileName").readText()

    private fun loadFile(filePath: String): File = File(filePath)

    private inline fun <reified T> deserializeOrThrow(value: String) = Jackson.deserializeMaybe(value, T::class.java)
        .orElseThrow {
            IllegalStateException(
                "Unable to deserialize configuration file, " +
                    "check generated config file against ${EnvConfig::class.simpleName} class"
            )
        }
}

