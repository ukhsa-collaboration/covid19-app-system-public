package uk.nhs.nhsx.analyticsevents

import uk.nhs.nhsx.core.Jackson.readOrNull
import java.io.IOException

class PayloadValidator {
    fun maybeValidPayload(requestBody: String?) = try {
        readOrNull<Map<String, Any>>(requestBody)
            ?.also {
                validateMetadata(it)
                validateEvents(it)
            }
    } catch (e: IOException) {
        null
    } catch (e: ValidationException) {
        null
    } catch (e: IllegalArgumentException) {
        null
    }

    private fun validateMetadata(payload: Map<String, *>) {
        requireMapAttribute(payload, "metadata")
        val metadataRaw = payload["metadata"] as? Map<*, *>
            ?: throw ValidationException("metadata must be a map")
        requireMapAttribute(metadataRaw, "operatingSystemVersion")
        requireMapAttribute(metadataRaw, "latestApplicationVersion")
        requireMapAttribute(metadataRaw, "deviceModel")
        requireMapAttribute(metadataRaw, "postalDistrict")
    }

    private fun validateEvents(payload: Map<String, *>) {
        requireMapAttribute(payload, "events")
        val eventsRaw = payload["events"] as? List<*>
            ?: throw ValidationException("events must be a list")
        for (event in eventsRaw) {
            requireMapAttribute(event, "type")
            requireMapAttribute(event, "version")
            requireMapAttribute(event, "payload")
        }
    }

    private fun requireMapAttribute(mapRaw: Any?, key: String) {
        if (mapRaw !is Map<*, *>) throw ValidationException("expected map type but was " + mapRaw?.javaClass?.simpleName)
        if (!mapRaw.containsKey(key)) throw ValidationException("Map did not contain expected key + $key")
    }

    internal class ValidationException(message: String?) : Exception(message)
}
