package smoke.clients

import com.amazonaws.services.lambda.model.InvokeResult
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isNullOrEmptyString
import org.http4k.asString
import org.http4k.core.ContentType
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasContentType
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
import uk.nhs.nhsx.core.Jackson
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

fun Response.requireStatusCode(expectedStatus: Status): Response {
    assertThat(this, hasStatus(expectedStatus))
    return this
}

fun Response.requireSignatureHeaders(): Response {
    requireSignature()
    requireSignatureDate()
    return this
}

private fun Response.requireSignature() {
    assertThat(this, hasHeader("x-amz-meta-signature"))

    val signature = headerValueOrThrow("x-amz-meta-signature")

    val fullSignatureParts = signature.split(",")

    val keyIdParts = fullSignatureParts[0].split("=")
    assertThat(keyIdParts[0], equalTo("keyId"))
    assertThat(keyIdParts[1], !isNullOrEmptyString)

    val signatureParts = fullSignatureParts[1].split("=")
    assertThat(signatureParts[0], equalTo("signature"))
    assertThat(signatureParts[1], !isNullOrEmptyString)
}

private fun Response.requireSignatureDate(): Response {
    assertThat(this, hasHeader("x-amz-meta-signature-date"))
    val signatureDate = headerValueOrThrow("x-amz-meta-signature-date")

    try {
        DateTimeFormatter
            .ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz")
            .parse(signatureDate)
    } catch (e: DateTimeParseException) {
        throw IllegalStateException("Invalid signature date header", e)
    }

    return this
}

private fun Response.headerValueOrThrow(key: String): String =
    this.headers.filter { it.first == key }.map { it.second }.first()
        ?: throw IllegalStateException("Header not found: $key")

fun Response.requireJsonContentType(): Response {
    assertThat(this, hasContentType(ContentType(ContentType.APPLICATION_JSON.value)))
    return this
}

fun Response.requireZipContentType(): Response {
    assertThat(this, hasContentType(ContentType("application/zip")))
    return this
}

fun Response.requireNoPayload(): Response {
    assertThat(this, hasBody(""))
    return this
}

fun Response.requireBodyText(expectedText: String): Response {
    val actual = bodyString()
    if (actual != expectedText)
        throw IllegalStateException("Expected: '$expectedText', got: $actual")
    return this
}

inline fun <reified T> Response.deserializeOrThrow(): T {
    requireJsonContentType()
    val bodyString = bodyString()
    return Jackson
        .deserializeMaybe(bodyString, T::class.java)
        .orElseThrow { IllegalStateException("Unable to deserialize: $bodyString") }
}

fun InvokeResult.requireStatusCode(expectedStatus: Status): InvokeResult {
    assertThat(statusCode, equalTo(expectedStatus.code))
    return this
}

fun InvokeResult.requireBodyText(expectedBodyText: String): InvokeResult {
    assertThat(payload.asString(), equalTo(expectedBodyText))
    return this
}