package uk.nhs.nhsx.keyfederation.upload

import uk.nhs.nhsx.core.signature.Signer
import java.util.*

class JWS(private val signer: Signer) {
    fun sign(payload: String): String {
        val signedComponent = "${encode("""{"alg":"ES256"}""")}.${encode(payload)}"
        val signature = signer.sign(signedComponent.toByteArray())
        val encodedSignature = Base64.getUrlEncoder().encodeToString(signature.asJWSCompatible())
        return "$signedComponent.$encodedSignature"
    }

    private fun encode(string: String): String = Base64.getUrlEncoder().encodeToString(string.toByteArray())

}
