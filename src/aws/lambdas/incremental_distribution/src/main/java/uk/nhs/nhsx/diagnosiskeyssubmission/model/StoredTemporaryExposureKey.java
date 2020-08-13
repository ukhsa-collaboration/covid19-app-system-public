package uk.nhs.nhsx.diagnosiskeyssubmission.model;

public class StoredTemporaryExposureKey {

    public final String key;
    public final Integer rollingStartNumber;
    public final Integer rollingPeriod;
    public final Integer transmissionRisk;

    public StoredTemporaryExposureKey(String key,
                                      Integer rollingStartNumber,
                                      Integer rollingPeriod) {
        this.key = key;
        this.rollingStartNumber = rollingStartNumber;
        this.rollingPeriod = rollingPeriod;
        this.transmissionRisk = 7;
    }

}