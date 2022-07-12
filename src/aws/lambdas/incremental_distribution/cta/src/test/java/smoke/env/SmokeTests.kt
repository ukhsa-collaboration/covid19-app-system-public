package smoke.env

import org.http4k.format.Moshi
import uk.nhs.nhsx.core.aws.xray.Tracing
import java.io.File
import java.io.FileNotFoundException

object SmokeTests {

    init {
        Tracing.disableXRayComplaintsForMainClasses()
    }

    private const val default_config_file = "../../../../../out/gen/config/test_config_branch.json"

    fun loadConfig(path: String = default_config_file): EnvConfig = deserializeOrThrow(loadConfigAsString(path))

    private fun loadConfigAsString(path: String) = loadConfigFileFor(loadConfigFileName(path))

    fun loadStaticContent(fileName: String) = loadFile("../../../../../src/static/$fileName").readText()

    private fun loadConfigFileName(path: String) = System.getenv("SMOKE_TEST_CONFIG") ?: path

    private fun loadConfigFileFor(envConfigFileName: String) = try {
        loadFile(envConfigFileName).readText()
    } catch (e: FileNotFoundException) {
        error("""Config file not found: ${envConfigFileName}, run following commands to generate it: rake gen:config:<tgt_env>""")
    }

    private fun loadFile(filePath: String): File = File(filePath)

    private inline fun <reified T : Any> deserializeOrThrow(value: String): T = try {
        Moshi.asA(value)
    } catch (e: Exception) {
        error(
            "Unable to deserialize configuration file, check generated config file against"
                + " ${EnvConfig::class.simpleName} class\n"
            + "\tCause: ${e}"
        )
    }
}

