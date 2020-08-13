package uk.nhs.nhsx.diagnosiskeyssubmission.model;

public class ClientTemporaryExposureKey {

    public final String key;
    public final int rollingStartNumber;
    public final int rollingPeriod;

    public ClientTemporaryExposureKey(String key,
                                      int rollingStartNumber,
                                      int rollingPeriod) {
        this.key = key;
        this.rollingStartNumber = rollingStartNumber;
        this.rollingPeriod = rollingPeriod;
    }

}