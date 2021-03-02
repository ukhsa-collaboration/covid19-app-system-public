package uk.nhs.nhsx.core.aws.ssm

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterResult
import com.amazonaws.services.simplesystemsmanagement.model.Parameter
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.testhelper.proxy
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

class AwsSsmParametersTest {

    @Test
    fun `caches parameter results`() {
        val interval = Duration.ofSeconds(1)
        val parameters = AwsSsmParameters(FakeSimpleSystemsManagementClient(), interval)
        val name = ParameterName.of("Hello")

        assertThat(getValue(parameters, name), equalTo("world1"))

        await().atMost(interval).until { getValue(parameters, name) == "world2" }
        await().atMost(interval).until { getValue(parameters, name) == "world3" }
    }

    class FakeSimpleSystemsManagementClient : AWSSimpleSystemsManagement by proxy() {
        val count = AtomicInteger(0)
        override fun getParameter(getParameterRequest: GetParameterRequest): GetParameterResult {
            return GetParameterResult().withParameter(
                Parameter().withName("Hello").withValue("WORLD${count.incrementAndGet()}")
            )
        }
    }

    private fun getValue(
        parameters: AwsSsmParameters,
        name: ParameterName
    ) = parameters.parameter(name) { it.toLowerCase() }.value()
}
