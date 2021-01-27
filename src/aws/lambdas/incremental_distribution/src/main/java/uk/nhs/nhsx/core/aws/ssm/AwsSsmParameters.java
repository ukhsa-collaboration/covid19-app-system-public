package uk.nhs.nhsx.core.aws.ssm;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterResult;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class AwsSsmParameters implements Parameters {

    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final Logger logger = LogManager.getLogger(AwsSsmParameters.class);

    private final AWSSimpleSystemsManagement ssmClient;

    public AwsSsmParameters() {
        ssmClient = AWSSimpleSystemsManagementClientBuilder.defaultClient();
    }

    @Override
    public <T> Parameter<T> parameter(ParameterName name, Function<String, T> convert) {
        LoadingCache<ParameterName, T> loader = CacheBuilder.newBuilder()
            .refreshAfterWrite(2, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                @Override
                public T load(ParameterName parameterName) {
                    return AwsSsmParameters.this.load(parameterName, convert);
                }

                @Override
                public ListenableFuture<T> reload(ParameterName key, T oldValue) {
                    ListenableFutureTask<T> task = ListenableFutureTask.create(() -> load(key));
                    executor.execute(task);
                    return task;
                }
            });

        return () -> {
            try {
                return loader.get(name);
            } catch (ExecutionException e) {
                throw new RuntimeException("Unable to load parameter for " + name, e);
            }
        };
    }

    private <T> T load(ParameterName name, Function<String, T> convert) {
        logger.info("Reloading value of parameter {}", name);
        GetParameterRequest request = new GetParameterRequest().withName(name.value);
        GetParameterResult result = ssmClient.getParameter(request);
        return convert.apply(result.getParameter().getValue());
    }
}
