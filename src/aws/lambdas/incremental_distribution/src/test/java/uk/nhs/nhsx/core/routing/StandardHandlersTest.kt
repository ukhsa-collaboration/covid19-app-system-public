package uk.nhs.nhsx.core.routing

import org.apache.http.entity.ContentType
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.HttpResponses
import uk.nhs.nhsx.core.TestEnvironments
import uk.nhs.nhsx.core.exceptions.HttpStatusCode
import uk.nhs.nhsx.testhelper.ProxyRequestBuilder.Companion.request
import uk.nhs.nhsx.testhelper.matchers.ProxyResponseAssertions.hasStatus
import java.util.*

class StandardHandlersTest {

    @Test
    fun oaiHeaderIsRequiredToMatchWhenEnvironmentVariableIsSet_Set() {
        val env = mapOf("custom_oai" to "bob")
        val handler = StandardHandlers.requiringCustomAccessIdentity(TestEnvironments.TEST.apply(env)) { HttpResponses.ok() }
        assertThat(handler.handle(request().build()), hasStatus(HttpStatusCode.FORBIDDEN_403))
        assertThat(handler.handle(request().withHeader("x-custom-oai", "bob").build()), hasStatus(HttpStatusCode.OK_200))
        assertThat(handler.handle(request().withHeader("x-custom-oai", "jim").build()), hasStatus(HttpStatusCode.FORBIDDEN_403))
        assertThat(handler.handle(request().withHeader("x-custom-oai", "").build()), hasStatus(HttpStatusCode.FORBIDDEN_403))
        assertThat(handler.handle(request().withHeader("x-custom-oai-something", "bob").build()), hasStatus(HttpStatusCode.FORBIDDEN_403))
    }

    @Test
    fun oaiHeaderIsRequiredToMatchWhenEnvironmentVariableIsSet_NotSet() {
        val handler = StandardHandlers.requiringCustomAccessIdentity(TestEnvironments.EMPTY) { HttpResponses.ok() }
        assertThat(handler.handle(request().build()), hasStatus(HttpStatusCode.OK_200))
        assertThat(handler.handle(request().withHeader("x-custom-oai", "bob").build()), hasStatus(HttpStatusCode.OK_200))
    }

    @Test
    fun requiringAuthorizationHeader_supplied() {
        val handler = StandardHandlers.requiringAuthorizationHeader { HttpResponses.ok() }
        assertThat(handler.handle(request().withHeader("authorization", "something").build()), hasStatus(HttpStatusCode.OK_200))
    }

    @Test
    fun requiringAuthorizationHeader_notSupplied() {
        val handler = StandardHandlers.requiringAuthorizationHeader { HttpResponses.ok() }
        assertThat(handler.handle(request().build()), hasStatus(HttpStatusCode.FORBIDDEN_403))
    }

    @Test
    fun authorisedBy_notSupplied() {
        val handler = StandardHandlers.authorisedBy({ true }) { HttpResponses.ok() }
        assertThat(handler.handle(request().build()), hasStatus(HttpStatusCode.FORBIDDEN_403))
    }

    @Test
    fun authorisedBy_authorised() {
        val handler = StandardHandlers.authorisedBy({ true }) { HttpResponses.ok() }
        assertThat(handler.handle(request().withHeader("authorization", "something").build()), hasStatus(HttpStatusCode.OK_200))
    }

    @Test
    fun authorisedBy_notAuthorised() {
        val handler = StandardHandlers.authorisedBy({ false }) { HttpResponses.ok() }
        assertThat(handler.handle(request().withHeader("authorization", "something").build()), hasStatus(HttpStatusCode.FORBIDDEN_403))
    }

    @Test
    fun expectingContentType_suppliedCorrect() {
        val handler = StandardHandlers.expectingContentType(ContentType.APPLICATION_JSON) { HttpResponses.ok() }
        assertThat(handler.handle(request().withHeader("Content-Type", "application/json").build()), hasStatus(HttpStatusCode.OK_200))
        assertThat(handler.handle(request().withHeader("Content-Type", "application/json;charset=utf-8").build()), hasStatus(HttpStatusCode.OK_200))
    }

    @Test
    fun expectingContentType_suppliedIncorrect() {
        val handler = StandardHandlers.expectingContentType(ContentType.APPLICATION_JSON) { HttpResponses.ok() }
        assertThat(handler.handle(request().withHeader("Content-Type", "text/html").build()), hasStatus(HttpStatusCode.UNPROCESSABLE_ENTITY_422))
        assertThat(handler.handle(request().withHeader("Content-Type", "application/vnd.ms-excel").build()), hasStatus(HttpStatusCode.UNPROCESSABLE_ENTITY_422))
    }

    @Test
    fun expectingContentType_notSupplied() {
        val handler = StandardHandlers.expectingContentType(ContentType.APPLICATION_JSON) { HttpResponses.ok() }
        assertThat(handler.handle(request().build()), hasStatus(HttpStatusCode.BAD_REQUEST_400))
    }

    @Test
    fun filteringWhileMaintenanceModeEnabled_suppliedTrue() {
        val env = mapOf("MAINTENANCE_MODE" to "TRUE")
        val handler = StandardHandlers.filteringWhileMaintenanceModeEnabled(TestEnvironments.TEST.apply(env)) { HttpResponses.ok() }
        assertThat(handler.handle(request().build()), hasStatus(HttpStatusCode.SERVICE_UNAVAILABLE_503))
    }

    @Test
    fun filteringWhileMaintenanceModeEnabled_suppliedFalse() {
        val env = mapOf("MAINTENANCE_MODE" to "FALSE")
        val handler = StandardHandlers.filteringWhileMaintenanceModeEnabled(TestEnvironments.TEST.apply(env)) { HttpResponses.ok() }
        assertThat(handler.handle(request().build()), hasStatus(HttpStatusCode.OK_200))
    }

    @Test
    fun filteringWhileMaintenanceModeEnabled_suppliedEmpty() {
        val env = mapOf("MAINTENANCE_MODE" to "")
        val handler = StandardHandlers.filteringWhileMaintenanceModeEnabled(TestEnvironments.TEST.apply(env)) { HttpResponses.ok() }
        assertThat(handler.handle(request().build()), hasStatus(HttpStatusCode.OK_200))
    }

    @Test
    fun handlesEmptyAuthHeader() {
        val authorizationHeader = ""
        assertThat(StandardHandlers.apiKeyNameFrom(authorizationHeader), `is`(Optional.empty<Any>()))
    }

    @Test
    fun handlesEmptyApiKey() {
        val apiKey = ""
        val authorizationHeader = "Bearer $apiKey"
        assertThat(StandardHandlers.apiKeyNameFrom(authorizationHeader), `is`(Optional.empty<Any>()))
    }

    @Test
    fun handlesApiKeyWithNonBase64Encoding() {
        val apiKey = "name:value"
        val authorizationHeader = "Bearer $apiKey"
        assertThat(StandardHandlers.apiKeyNameFrom(authorizationHeader), `is`(Optional.empty<Any>()))
    }
}