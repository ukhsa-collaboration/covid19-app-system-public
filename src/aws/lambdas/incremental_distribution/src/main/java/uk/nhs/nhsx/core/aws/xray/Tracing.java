package uk.nhs.nhsx.core.aws.xray;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Subsegment;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

public class Tracing {

    public static <T> T tracing(String name, Class<T> clazz, T delegate) {
        return clazz.cast(
            Proxy.newProxyInstance(
                clazz.getClassLoader(),
                new Class<?>[]{clazz},
                (proxy, method, args) -> {
                    Subsegment subsegment = AWSXRay.beginSubsegment(name);
                    try {
                        return method.invoke(delegate, args);
                    } catch (IllegalAccessException | IllegalArgumentException e) {
                        subsegment.addException(e);
                        throw new RuntimeException("Programmer error invoking " + method + " on " + clazz);
                    } catch (InvocationTargetException e) {
                        Throwable cause = e.getCause();
                        subsegment.addException(cause);
                        throw cause;
                    } finally {
                        subsegment.close();
                    }
                }
            )
        );
    }
}
