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

        AwsLambda.invokeFunction(config.diagnosisKeysProcessingFunction)
            .requireStatusCode(Status.OK)
            .requireBodyText("\"success\"")
    }
}