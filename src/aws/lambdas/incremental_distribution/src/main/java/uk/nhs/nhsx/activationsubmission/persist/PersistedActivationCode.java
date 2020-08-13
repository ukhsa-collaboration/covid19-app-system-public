package uk.nhs.nhsx.activationsubmission.persist;

import uk.nhs.nhsx.activationsubmission.ActivationCode;

import java.time.Instant;

public class PersistedActivationCode {
    public final Instant created;
    public final ActivationCodeBatchName batchName;
    public final ActivationCode activationCode;
    public final Instant activated;
    public final boolean alreadyUsed;

    public PersistedActivationCode(Instant created, ActivationCodeBatchName batchName, ActivationCode activationCode, Instant activated, boolean alreadyUsed) {
        this.created = created;
        this.batchName = batchName;
        this.activationCode = activationCode;
        this.activated = activated;
        this.alreadyUsed = alreadyUsed;
    }

    @Override
    public String toString() {
        return "PersistedActivationCode{" +
                "created=" + created +
                ", batchName='" + batchName + '\'' +
                ", activationCode=" + activationCode +
                ", activated=" + activated +
                '}';
    }
}
