package uk.nhs.nhsx.core.aws.ssm;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterResult;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import static java.lang.String.format;

public class AwsSsmParameters implements Parameters {

    private static final ExecutorService executor = Executors.newCachedThreadPool();

    private final AWSSimpleSystemsManagement ssmClient;
    private final Duration refreshInterval;

    public AwsSsmParameters(AWSSimpleSystemsManagement ssmClient, Duration refreshInterval) {
        this.ssmClient = ssmClient;
        this.refreshInterval = refreshInterval;
    }

    public AwsSsmParameters() {
        this(AWSSimpleSystemsManagementClientBuilder.defaultClient(), Duration.ofMinutes(2));
    }

    @Override
    public <T> Parameter<T> parameter(ParameterName name, Function<String, T> convert) {
        LoadingCache<ParameterName, T> loader = Caffeine.newBuilder()
            .executor(executor)
            .refreshAfterWrite(refreshInterval)
            .build(key -> getParameter(key, convert));

        return () -> {
            try {
                return loader.get(name);
            } catch (Exception e) {
                throw new RuntimeException(format("Unable to load parameter for %s", name), e);
            }
        };
    }

    private <T> T getParameter(ParameterName name, Function<String, T> convert) {
        GetParameterRequest request = new GetParameterRequest().withName(name.getValue());
        GetParameterResult result = ssmClient.getParameter(request);
        return convert.apply(result.getParameter().getValue());
    }
}
