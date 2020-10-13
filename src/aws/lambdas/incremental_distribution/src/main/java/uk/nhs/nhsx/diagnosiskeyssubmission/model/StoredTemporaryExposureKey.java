package uk.nhs.nhsx.diagnosiskeyssubmission.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

public class StoredTemporaryExposureKey {

    public final String key;
    public final Integer rollingStartNumber;
    public final Integer rollingPeriod;
    public final Integer transmissionRisk;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public final Integer daysSinceOnsetOfSymptoms;

    public StoredTemporaryExposureKey(String key, Integer rollingStartNumber, Integer rollingPeriod, Integer transmissionRisk, Integer daysSinceOnsetOfSymptoms) {
        this.key = key;
        this.rollingStartNumber = rollingStartNumber;
        this.rollingPeriod = rollingPeriod;
        this.transmissionRisk = transmissionRisk;
        this.daysSinceOnsetOfSymptoms = daysSinceOnsetOfSymptoms;
    }

    @JsonCreator
    public StoredTemporaryExposureKey(String key, Integer rollingStartNumber, Integer rollingPeriod, Integer transmissionRisk) {
        this(key,rollingStartNumber,rollingPeriod,transmissionRisk, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StoredTemporaryExposureKey that = (StoredTemporaryExposureKey) o;
        return Objects.equals(key, that.key) &&
            Objects.equals(rollingStartNumber, that.rollingStartNumber) &&
            Objects.equals(rollingPeriod, that.rollingPeriod) &&
            Objects.equals(transmissionRisk, that.transmissionRisk) &&
            Objects.equals(daysSinceOnsetOfSymptoms, that.daysSinceOnsetOfSymptoms);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, rollingStartNumber, rollingPeriod, transmissionRisk, daysSinceOnsetOfSymptoms);
    }

    @Override
    public String toString() {
        return "StoredTemporaryExposureKey{" +
            "key='" + key + '\'' +
            ", rollingStartNumber=" + rollingStartNumber +
            ", rollingPeriod=" + rollingPeriod +
            ", transmissionRisk=" + transmissionRisk +
            ", daysSinceOnsetOfSymptoms=" + daysSinceOnsetOfSymptoms +
            '}';
    }
}