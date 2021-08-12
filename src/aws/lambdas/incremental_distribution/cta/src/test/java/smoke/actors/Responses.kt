package smoke.actors

import assertions.AwsSdkAssertions.asString
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES
import org.http4k.core.ContentType
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.format.ConfigurableJackson
import org.http4k.strikt.contentType
import software.amazon.awssdk.services.lambda.model.InvokeResponse
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNullOrEmpty
import strikt.assertions.isSuccess
import strikt.assertions.withElementAt
import uk.nhs.nhsx.core.AppServicesJson
import uk.nhs.nhsx.core.Json
import uk.nhs.nhsx.testhelper.assertions.bodyString
import uk.nhs.nhsx.testhelper.assertions.hasStatus
import uk.nhs.nhsx.testhelper.assertions.header
import uk.nhs.nhsx.testhelper.assertions.isNotNullOrBlank
import uk.nhs.nhsx.testhelper.assertions.signatureDateHeader
import uk.nhs.nhsx.testhelper.assertions.signatureHeader
import java.time.format.DateTimeFormatter
import java.util.Locale.ENGLISH

fun Response.requireStatusCode(expectedStatus: Status) = apply {
    expectThat(this).hasStatus(expectedStatus)
}

fun Response.requireSignatureHeaders() = apply {
    requireSignature()
    requireSignatureDate()
}

private fun Response.requireSignature() {
    expectThat(this) {
        signatureHeader.isNotNull()
        signatureDateHeader.isNotNull()

        get("keyIdParts") {
            header("x-amz-meta-signature")
            headerValueOrThrow("x-amz-meta-signature")
                .split(",")[0]
                .split("=")
        }
            .withElementAt(0) { isEqualTo("keyId") }
            .withElementAt(1) { not().isNullOrEmpty() }

        get("signatureParts") {
            headerValueOrThrow("x-amz-meta-signature")
                .split(",")[1]
                .split("=")
        }
            .withElementAt(0) { isEqualTo("signature") }
            .withElementAt(1) { not().isNullOrEmpty() }
    }
}

private fun Response.requireSignatureDate() = apply {
    expectThat(this)
        .signatureHeader
        .isNotNullOrBlank()

    expectCatching {
        DateTimeFormatter
            .ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", ENGLISH)
            .parse(headerValueOrThrow("x-amz-meta-signature-date"))
    }.isSuccess()
}

private fun Response.headerValueOrThrow(key: String): String {
    val header = header(key)
    expectThat(this).header(key).isNotNull()
    return header!!
}

fun Response.requireJsonContentType() = apply {
    expectThat(this).contentType.isEqualTo(ContentType(ContentType.APPLICATION_JSON.value))
}

fun Response.requireZipContentType() = apply {
    expectThat(this).contentType.isEqualTo(ContentType("application/zip"))
}

fun Response.requireNoPayload() = apply {
    expectThat(this).bodyString.isEmpty()
}

fun Response.requireBodyText(expectedText: String) = apply {
    expectThat(this).bodyString.isEqualTo(expectedText)
}

inline fun <reified T : Any> Response.deserializeOrThrow(): T {
    requireJsonContentType()
    return Json.readJsonOrNull(bodyString()) ?: error("Unable to deserialize: ${bodyString()}")
}

object JsonAllowingNullCreators : ConfigurableJackson(
    AppServicesJson.mapper.copy().configure(FAIL_ON_NULL_CREATOR_PROPERTIES, false)
)

inline fun <reified T : Any> Response.deserializeWithNullCreatorsOrThrow(): T {
    requireJsonContentType()
    return JsonAllowingNullCreators.asA(bodyString(), T::class)
}

fun InvokeResponse.requireStatusCode(expectedStatus: Status) = apply {
    expectThat(this).get(InvokeResponse::statusCode).isEqualTo(expectedStatus.code)
}

fun InvokeResponse.requireBodyContains(expected: String) = apply {
    expectThat(this)
        .get(InvokeResponse::payload)
        .asString()
        .contains(expected)
}
