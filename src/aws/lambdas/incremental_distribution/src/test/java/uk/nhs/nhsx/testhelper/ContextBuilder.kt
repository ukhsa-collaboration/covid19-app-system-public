package uk.nhs.nhsx.testhelper

import com.amazonaws.services.lambda.runtime.ClientContext
import com.amazonaws.services.lambda.runtime.CognitoIdentity
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.LambdaLogger
import java.nio.charset.StandardCharsets
import java.util.UUID

class ContextBuilder {

    fun build(): Context = TestContext()

    class TestContext : Context by proxy() {
        override fun getAwsRequestId() = UUID(0,0).toString()

        override fun getLogGroupName(): String = "test-log-group"

        override fun getLogStreamName(): String = "test-log-stream"

        override fun getFunctionName(): String = "function-name"

        override fun getFunctionVersion(): String = "function-version"

        override fun getInvokedFunctionArn(): String = "lambda-function-arn"

        override fun getIdentity(): CognitoIdentity? = null

        override fun getRemainingTimeInMillis(): Int = 1000

        override fun getMemoryLimitInMB(): Int = 100

        override fun getLogger() = object : LambdaLogger {
            override fun log(s: String) {
                println("s = $s")
            }

            override fun log(bytes: ByteArray) {
                println("bytes = " + String(bytes, StandardCharsets.UTF_8))
            }
        }
    }

    companion object {
        fun context(): ContextBuilder = ContextBuilder()

        fun aContext(): Context = ContextBuilder().build()
    }
}
