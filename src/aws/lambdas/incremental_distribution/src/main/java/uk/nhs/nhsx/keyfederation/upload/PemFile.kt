package uk.nhs.nhsx.keyfederation.upload

import uk.nhs.nhsx.core.aws.secretsmanager.SecretValue
import uk.nhs.nhsx.core.exceptions.Defect
import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.PrivateKey
import java.security.spec.InvalidKeySpecException
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*

class PemFile private constructor(private val contents: SecretValue) {

    val decoder = Base64.getDecoder()

    companion object {
        @JvmStatic
        fun of(secret: SecretValue): PemFile {
            return PemFile(secret)
        }
    }

    fun toECPrivateKey(): PrivateKey {
        val pem = contents.value
        return try {
            pem.split("\n")
                .filter { ! it.isEmpty() }
                .filter { ! it.startsWith("-----") }
                .joinToString("")
                .let {
                    decoder.decode(it)
                }
                .let {
                    PKCS8EncodedKeySpec(it)
                }
                .let {
                    KeyFactory.getInstance("EC").generatePrivate(it)
                }
        } catch (e: NoSuchAlgorithmException) {
            throw Defect("JVM doesn't have EC keys?", e)
        } catch (e: InvalidKeySpecException) {
            throw Defect("Private Key Spec Incorrect", e)
        }
    }
}