package uk.nhs.nhsx.activationsubmission.reporting;

import uk.nhs.nhsx.activationsubmission.persist.PersistedActivationCode;

public interface PersistedActivationCodeReporting {
    void missing();

    void accepted(PersistedActivationCode code);

    void expired(PersistedActivationCode code);
}
