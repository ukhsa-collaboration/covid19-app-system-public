package assertions

import org.jose4j.jws.JsonWebSignature
import org.jose4j.jws.JsonWebSignatureAlgorithm
import strikt.api.Assertion
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue

object JwtAssertions {
    val Assertion.Builder<JsonWebSignature>.signaturePayload get() = get(JsonWebSignature::getPayload)

    fun Assertion.Builder<JsonWebSignature>.hasValidSignature() =
        get(JsonWebSignature::verifySignature)
            .describedAs("signature is valid")
            .isTrue()

    fun Assertion.Builder<JsonWebSignature>.algorithmEqualTo(expected: String) =
        get(JsonWebSignature::getAlgorithm)
            .get(JsonWebSignatureAlgorithm::getAlgorithmIdentifier)
            .isEqualTo(expected)
}

