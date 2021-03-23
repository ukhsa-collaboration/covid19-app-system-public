package uk.nhs.nhsx.core.aws.ssm

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest
import com.github.benmanes.caffeine.cache.Caffeine
import java.time.Duration
import java.util.concurrent.Executors
import java.util.function.Function

class AwsSsmParameters @JvmOverloads constructor(
    private val ssmClient: AWSSimpleSystemsManagement = AWSSimpleSystemsManagementClientBuilder.defaultClient(),
    private val refreshInterval: Duration = Duration.ofMinutes(2)
) : Parameters {
    override fun <T> parameter(name: ParameterName, convert: Function<String, T>): Parameter<T> {
        val loader = Caffeine.newBuilder()
            .executor(executor)
            .refreshAfterWrite(refreshInterval)
            .build { key: ParameterName -> getParameter(key, convert) }

        return Parameter {
            try {
                loader[name] ?: error("No value loaded")
            } catch (e: Exception) {
                throw RuntimeException(String.format("Unable to load parameter for %s", name), e)
            }
        }
    }

    private fun <T> getParameter(name: ParameterName, convert: Function<String, T>): T {
        val request = GetParameterRequest().withName(name.value)
        val result = ssmClient.getParameter(request)
        return convert.apply(result.parameter.value)
    }

    companion object {
        private val executor = Executors.newCachedThreadPool()
    }
}
