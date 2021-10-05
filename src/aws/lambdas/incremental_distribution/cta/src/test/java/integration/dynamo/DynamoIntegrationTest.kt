package integration.dynamo

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import org.junit.jupiter.api.BeforeAll
import uk.nhs.nhsx.core.aws.xray.Tracing

open class DynamoIntegrationTest {

    companion object {
        val tgtEnv = System.getenv("INTEGRATION_TEST_ENV") ?: loadDefaultWithWarning()

        private fun loadDefaultWithWarning(): String {
            println("WARNING: 'INTEGRATION_TEST_ENV' env var not set, using te-ci to run integration tests")
            return "te-ci"
        }

        internal lateinit var dbClient: AmazonDynamoDB

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            Tracing.disableXRayComplaintsForMainClasses()
            dbClient = AmazonDynamoDBClientBuilder.defaultClient()
        }
    }

}
