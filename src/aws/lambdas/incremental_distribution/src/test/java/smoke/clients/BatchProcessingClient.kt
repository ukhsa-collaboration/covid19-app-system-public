package smoke.clients

import org.apache.logging.log4j.LogManager
import org.http4k.core.Status
import smoke.env.EnvConfig

class BatchProcessingClient(private val config: EnvConfig) {

    companion object {
        private val logger = LogManager.getLogger(BatchProcessingClient::class.java)
    }

    fun invokeBatchProcessing() {
        logger.info("invokeBatchProcessing")

        enableBatchProcessingOutsideTimeWindow()

        AwsLambda.invokeFunction(config.diagnosisKeysProcessingFunction)
            .requireStatusCode(Status.OK)
            .requireBodyText("\"success\"")

        disableBatchProcessingOutsideTimeWindow()
    }

    private fun enableBatchProcessingOutsideTimeWindow() {
        setAbortOutsideTimeWindow(false)
    }

    private fun disableBatchProcessingOutsideTimeWindow() {
        setAbortOutsideTimeWindow(true)
    }

    private fun setAbortOutsideTimeWindow(value: Boolean) {
        val envVarName = "ABORT_OUTSIDE_TIME_WINDOW"
        val result = AwsLambda.updateLambdaEnvVar(
            config.diagnosisKeysProcessingFunction,
            envVarName to "$value"
        )
        val updatedEnvVar = result.environment.variables[envVarName]
        if (updatedEnvVar != "$value")
            throw IllegalStateException("Expected env var: $envVarName to be updated but it was not.")
    }
}