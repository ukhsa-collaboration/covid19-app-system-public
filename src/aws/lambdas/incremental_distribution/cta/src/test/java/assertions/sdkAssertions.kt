package assertions

import com.google.protobuf.ByteString
import software.amazon.awssdk.core.SdkBytes
import strikt.api.Assertion
import strikt.assertions.isNullOrEmpty

object AwsSdkAssertions {
    fun Assertion.Builder<SdkBytes>.asString() = get(SdkBytes::asUtf8String)
}

object ProtobufAssertions {
    fun Assertion.Builder<ByteString>.asString() = get(ByteString::toStringUtf8)
    fun Assertion.Builder<ByteString>.isNotNullOrEmpty() = asString().not().isNullOrEmpty()
}

