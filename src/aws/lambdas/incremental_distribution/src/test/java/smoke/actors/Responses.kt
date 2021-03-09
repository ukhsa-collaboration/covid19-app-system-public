package smoke.actors

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isNullOrEmptyString
import org.http4k.core.ContentType
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasContentType
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
import software.amazon.awssdk.services.lambda.model.InvokeResponse
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
            .ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", java.util.Locale.ENGLISH)
            .parse(signatureDate)
    } catch (e: DateTimeParseException) {
        throw IllegalStateException("Invalid signature date header", e)
    }

    return this
}

private fun Response.headerValueOrThrow(key: String): String =
    header(key) ?: throw IllegalStateException("Header not found: $key")

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
    return Jackson.readOrNull<T>(bodyString()) ?: error("Unable to deserialize: ${bodyString()}")
}

fun InvokeResponse.requireStatusCode(expectedStatus: Status): InvokeResponse {
    assertThat(statusCode(), equalTo(expectedStatus.code))
    return this
}

fun InvokeResponse.requireBodyText(matcher: Matcher<String>): InvokeResponse {
    assertThat(payload().asUtf8String(), matcher)
    return this
}
