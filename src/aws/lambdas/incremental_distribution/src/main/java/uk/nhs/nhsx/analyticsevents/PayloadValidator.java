package uk.nhs.nhsx.analyticsevents;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.nhs.nhsx.core.Jackson;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PayloadValidator {

    private final static Logger log = LogManager.getLogger(PayloadValidator.class);

    public Optional<Map<String, Object>> maybeValidPayload(String requestBody) {
        try {
            Map<String, Object> payload = Jackson.readJson(requestBody, new TypeReference<>() {
            });

            validateMetadata(payload);
            validateEvents(payload);

            return Optional.of(payload);
        } catch (IOException | ValidationException | IllegalArgumentException e) {
            log.info("Payload was invalid", e);
            return Optional.empty();
        }
    }

    private void validateMetadata(Map<String, ?> payload) throws ValidationException {
        requireMapAttribute(payload, "metadata");

        Object metadataRaw = payload.get("metadata");
        if (!(metadataRaw instanceof Map)) {
            throw new ValidationException("metadata must be a map");
        }

        Map<?, ?> metadata = (Map<?, ?>) metadataRaw;

        requireMapAttribute(metadata, "operatingSystemVersion");
        requireMapAttribute(metadata, "latestApplicationVersion");
        requireMapAttribute(metadata, "deviceModel");
        requireMapAttribute(metadata, "postalDistrict");
    }

    private void validateEvents(Map<String, ?> payload) throws ValidationException {
        requireMapAttribute(payload, "events");

        Object eventsRaw = payload.get("events");

        if (!(eventsRaw instanceof List)) {
            throw new ValidationException("events must be a list");
        }

        List<?> events = (List<?>) eventsRaw;

        for (Object event : events) {
            requireMapAttribute(event, "type");
            requireMapAttribute(event, "version");
            requireMapAttribute(event, "payload");
        }
    }

    private void requireMapAttribute(Object mapRaw, String key) throws ValidationException {
        if (!(mapRaw instanceof Map)) {
            throw new ValidationException("expected map type but was " + mapRaw.getClass().getSimpleName());
        }

        Map<?, ?> map = (Map<?, ?>) mapRaw;

        if (!(map.containsKey(key))) {
            throw new ValidationException("Map did not contain expected key + " + key);
        }
    }

    static class ValidationException extends Exception {
        ValidationException(String message) {
            super(message);
        }
    }

}
