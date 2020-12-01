package smoke

import com.amazonaws.services.kms.AWSKMSClientBuilder
import com.amazonaws.services.kms.model.GetPublicKeyRequest
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.jose4j.jws.JsonWebSignature
import org.junit.jupiter.api.Test
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
        val generated = JWS(KmsSigner { parameterKeyLookup.kmsKeyId }).sign(payload)

        // get corresponding public key from kms and create java key from it
        val publicKey = AWSKMSClientBuilder.defaultClient().getPublicKey(GetPublicKeyRequest().withKeyId(parameterKeyLookup.kmsKeyId.value)).publicKey

        val kmsPublicKey = KeyFactory.getInstance("EC").let {
            it.generatePublic(
                X509EncodedKeySpec(
                    ByteArray(publicKey.remaining()).also {
                        publicKey.get(it)
                    }
                )
            )
        }

        // verify the signature
        val jws = JsonWebSignature().also {
            it.key = kmsPublicKey
            it.compactSerialization = generated
        }
        assertThat(jws.payload, equalTo(payload));
        assertThat("signature is valid", jws.verifySignature(), equalTo(true));
        assertThat(jws.headers.getStringHeaderValue("alg"), equalTo("ES256"))
    }
}