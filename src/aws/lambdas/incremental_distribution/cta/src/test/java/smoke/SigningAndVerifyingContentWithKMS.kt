package smoke

import assertions.JwtAssertions.algorithmEqualTo
import assertions.JwtAssertions.hasValidSignature
import assertions.JwtAssertions.signaturePayload
import com.amazonaws.services.kms.AWSKMSClientBuilder
import com.amazonaws.services.kms.model.GetPublicKeyRequest
import org.jose4j.jws.JsonWebSignature
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import uk.nhs.nhsx.core.aws.kms.KmsSigner
import uk.nhs.nhsx.core.aws.ssm.AwsSsmParameters
import uk.nhs.nhsx.core.aws.ssm.ParameterKeyLookup
import uk.nhs.nhsx.core.aws.ssm.ParameterName
import uk.nhs.nhsx.core.aws.xray.Tracing
import uk.nhs.nhsx.keyfederation.upload.JWS
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec

class SigningAndVerifyingContentWithKMS {

    @Test
    fun `signing and verifying some content with kms`() {
        Tracing.disableXRayComplaintsForMainClasses()
        // generate key using KmsSignature
        val parameterKeyLookup = ParameterKeyLookup(AwsSsmParameters(), ParameterName.of("/app/kms/SigningKeyArn"))
        val payload = """{ "key": 1234 }"""
        val kmsClient = AWSKMSClientBuilder.defaultClient()
        val generated = JWS(KmsSigner(parameterKeyLookup::kmsKeyId, kmsClient)).sign(payload)

        // get corresponding public key from kms and create java key from it
        val publicKey = kmsClient
            .getPublicKey(
                GetPublicKeyRequest()
                    .withKeyId(parameterKeyLookup.kmsKeyId().value)
            ).publicKey

        val kmsPublicKey = KeyFactory.getInstance("EC").let {
            it.generatePublic(
                X509EncodedKeySpec(
                    ByteArray(publicKey.remaining()).also { b ->
                        publicKey.get(b)
                    }
                )
            )
        }

        // verify the signature
        val jws = JsonWebSignature().also {
            it.key = kmsPublicKey
            it.compactSerialization = generated
        }

        expectThat(jws) {
            algorithmEqualTo("ES256")
            hasValidSignature()
            signaturePayload.isEqualTo(payload)
        }
    }
}
