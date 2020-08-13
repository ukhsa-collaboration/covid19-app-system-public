package uk.nhs.nhsx.activationsubmission.persist;

import uk.nhs.nhsx.activationsubmission.ActivationCode;
import uk.nhs.nhsx.activationsubmission.reporting.PersistedActivationCodeReporting;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;

public class ExpiringPersistedActivationCodeLookup implements PersistedActivationCodeLookup {

    private final Supplier<Instant> clock;
    private final Duration validity;
    private final PersistedActivationCodeLookup codes;
    private final PersistedActivationCodeReporting reporting;

    public ExpiringPersistedActivationCodeLookup(Supplier<Instant> clock, Duration validity, PersistedActivationCodeLookup codes, PersistedActivationCodeReporting reporting) {
        this.clock = clock;
        this.validity = validity;
        this.codes = codes;
        this.reporting = reporting;
    }

    @Override
    public Optional<PersistedActivationCode> find(ActivationCode code) {
        Optional<PersistedActivationCode> maybeFromDB = codes.find(code);
        if (maybeFromDB.isPresent()) {
            PersistedActivationCode persisted = maybeFromDB.get();
            if (Duration.between(persisted.activated, clock.get()).compareTo(validity) < 0) {
                if (!persisted.alreadyUsed) {
                    reporting.accepted(persisted);
                }
                return maybeFromDB;
            } else {
                reporting.expired(persisted);
                return Optional.empty();
            }
        } else {
            reporting.missing();
            return Optional.empty();
        }
    }
}
