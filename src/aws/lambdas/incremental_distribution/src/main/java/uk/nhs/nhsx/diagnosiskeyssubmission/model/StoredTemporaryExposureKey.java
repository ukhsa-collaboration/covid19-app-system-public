package uk.nhs.nhsx.diagnosiskeyssubmission.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;

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

}