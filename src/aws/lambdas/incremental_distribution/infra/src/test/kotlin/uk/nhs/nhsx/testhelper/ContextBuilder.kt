package uk.nhs.nhsx.testhelper

import com.amazonaws.services.lambda.runtime.CognitoIdentity
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.LambdaLogger
import java.nio.charset.StandardCharsets
import java.util.UUID

class ContextBuilder {

    fun build(): Context = TestContext()

    class TestContext : Context by proxy() {
        var requestId = UUID(0, 0)

        override fun getAwsRequestId() = requestId.toString()

        override fun getLogGroupName() = "test-log-group"

        override fun getLogStreamName() = "test-log-stream"

        override fun getFunctionName() = "function-name"

        override fun getFunctionVersion() = "function-version"

        override fun getInvokedFunctionArn() = "lambda-function-arn"

        override fun getIdentity(): CognitoIdentity? = null

        override fun getRemainingTimeInMillis() = 1000

        override fun getMemoryLimitInMB() = 100

        override fun getLogger() = object : LambdaLogger {
            override fun log(s: String) {
                println("s = $s")
            }

            override fun log(bytes: ByteArray) {
                println("bytes = ${String(bytes)}")
            }
        }
    }

    companion object {
        fun context() = ContextBuilder()
        fun aContext() = ContextBuilder().build()
    }
}
