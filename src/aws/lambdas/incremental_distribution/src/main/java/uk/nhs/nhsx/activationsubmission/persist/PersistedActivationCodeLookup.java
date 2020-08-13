package uk.nhs.nhsx.activationsubmission.persist;

import uk.nhs.nhsx.activationsubmission.ActivationCode;

import java.util.Optional;

public interface PersistedActivationCodeLookup {
    Optional<PersistedActivationCode> find(ActivationCode code);
}
