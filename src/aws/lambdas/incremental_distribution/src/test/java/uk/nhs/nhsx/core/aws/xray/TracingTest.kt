package uk.nhs.nhsx.core.aws.xray

import com.amazonaws.xray.AWSXRay
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.sameInstance
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.aws.xray.TracingTest.NiceInterface
import java.io.FileNotFoundException
import java.util.concurrent.Callable
import kotlin.jvm.Throws

class TracingTest {

    fun interface NiceInterface {
        @Throws(Exception::class)
        fun method(arg1: Int, arg2: Any): Any
    }

    @Test
    fun invokesDelegateAndReturnsCorrectResult() {
        inDummySegment {
            val returnValue = Any()
            val arg1 = 1
            val arg2 = Any()
            val i = Tracing.tracing(
                "invocations",
                NiceInterface::class.java,
                NiceInterface { a: Int, b: Any ->
                    assertThat(a, `is`(arg1))
                    assertThat(b, sameInstance(arg2))
                    returnValue
                }
            )
            assertThat(i.method(arg1, arg2), sameInstance(returnValue))
            null
        }
    }

    @Test
    fun throwsCorrectException() {
        assertThrows(FileNotFoundException::class.java) {
            inDummySegment {
                val i = Tracing.tracing(
                    "invocations",
                    NiceInterface::class.java,
                    NiceInterface { _: Int, _: Any -> throw FileNotFoundException("should not get converted to invocation/undeclared throwable exception") }
                )
                i.method(1, this)
                null
            }
        }
    }

    private fun inDummySegment(runnable: Callable<Void?>) {
        try {
            AWSXRay.beginDummySegment().use { runnable.call() }
        } finally {
            AWSXRay.endSegment()
        }
    }
}