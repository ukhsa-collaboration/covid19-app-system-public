package uk.nhs.nhsx.testhelper

import com.amazonaws.services.lambda.runtime.ClientContext
import com.amazonaws.services.lambda.runtime.CognitoIdentity
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.LambdaLogger
import java.nio.charset.StandardCharsets

class ContextBuilder {

    fun build(): Context {
        return TestContext()
    }

    class TestContext : Context {
        override fun getAwsRequestId(): String {
            throw UnsupportedOperationException("james didn't write")
        }

        override fun getLogGroupName(): String {
            return "test-log-group"
        }

        override fun getLogStreamName(): String {
            return "test-log-stream"
        }

        override fun getFunctionName(): String {
            return "function-name"
        }

        override fun getFunctionVersion(): String {
            return "function-version"
        }

        override fun getInvokedFunctionArn(): String {
            return "lambda-function-arn"
        }

        override fun getIdentity(): CognitoIdentity? {
            return null
        }

        override fun getClientContext(): ClientContext {
            throw UnsupportedOperationException("james didn't write")
        }

        override fun getRemainingTimeInMillis(): Int {
            return 1000
        }

        override fun getMemoryLimitInMB(): Int {
            return 100
        }

        override fun getLogger(): LambdaLogger {
            return object : LambdaLogger {
                override fun log(s: String) {
                    println("s = $s")
                }

                override fun log(bytes: ByteArray) {
                    println("bytes = " + String(bytes, StandardCharsets.UTF_8))
                }
            }
        }
    }

    companion object {
        fun context(): ContextBuilder {
            return ContextBuilder()
        }

        fun aContext(): Context {
            return ContextBuilder().build()
        }
    }
}