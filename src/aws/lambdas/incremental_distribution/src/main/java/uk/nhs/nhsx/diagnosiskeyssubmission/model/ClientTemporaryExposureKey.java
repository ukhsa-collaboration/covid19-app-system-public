package uk.nhs.nhsx.diagnosiskeyssubmission.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import java.util.Objects;

public class ClientTemporaryExposureKey {

    public final String key;
    public final int rollingStartNumber;
    public final int rollingPeriod;
    public int transmissionRiskLevel = 7;
    @JsonInclude(Include.NON_NULL)
    public Integer daysSinceOnsetOfSymptoms;

    public ClientTemporaryExposureKey(String key,
                                      int rollingStartNumber,
                                      int rollingPeriod) {
        this.key = key;
        this.rollingStartNumber = rollingStartNumber;
        this.rollingPeriod = rollingPeriod;
    }

    public void setTransmissionRiskLevel(int transmissionRiskLevel) {
        this.transmissionRiskLevel = transmissionRiskLevel;
    }

    public void setDaysSinceOnsetOfSymptoms(Integer daysSinceOnsetOfSymptoms){
        this.daysSinceOnsetOfSymptoms = daysSinceOnsetOfSymptoms;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClientTemporaryExposureKey that = (ClientTemporaryExposureKey) o;
        return rollingStartNumber == that.rollingStartNumber &&
            rollingPeriod == that.rollingPeriod &&
            Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, rollingStartNumber, rollingPeriod);
    }

    @Override
    public String toString() {
        return "ClientTemporaryExposureKey{" +
            "key='" + key + '\'' +
            ", rollingStartNumber=" + rollingStartNumber +
            ", transmissionRiskLevel=" + transmissionRiskLevel +
            ", daysSinceOnsetOfSymptoms=" + daysSinceOnsetOfSymptoms +
            '}';
    }
}
