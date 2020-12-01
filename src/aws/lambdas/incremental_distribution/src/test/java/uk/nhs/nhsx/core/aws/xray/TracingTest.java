package uk.nhs.nhsx.core.aws.xray;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Segment;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.Callable;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.nhs.nhsx.core.aws.xray.Tracing.tracing;

public class TracingTest {

    public interface NiceInterface {
        Object method(int arg1, Object arg2) throws IOException;
    }

    @Test
    public void invokesDelegateAndReturnsCorrectResult() throws Exception {
        inDummySegment(() ->
        {
            Object returnValue = new Object();

            int arg1 = 1;
            Object arg2 = new Object();

            NiceInterface i = tracing("invocations", NiceInterface.class, (a, b) -> {
                assertThat(a, is(arg1));
                assertThat(b, sameInstance(arg2));
                return returnValue;
            });

            assertThat(i.method(arg1, arg2), sameInstance(returnValue));
            return null;
        });
    }

    @Test
    public void throwsCorrectException() {

        assertThrows(FileNotFoundException.class, () ->
                inDummySegment(() -> {
                    NiceInterface i = tracing("invocations", NiceInterface.class, (a, b) -> {
                        throw new FileNotFoundException("should not get converted to invocation/undeclared throwable exception");
                    });
                    i.method(1, this);
                    return null;
                })
        );
    }

    private void inDummySegment(Callable<Void> runnable) throws Exception {
        try (Segment ignored = AWSXRay.beginDummySegment()) {
            runnable.call();
        } finally {
            AWSXRay.endSegment();
        }
    }
}