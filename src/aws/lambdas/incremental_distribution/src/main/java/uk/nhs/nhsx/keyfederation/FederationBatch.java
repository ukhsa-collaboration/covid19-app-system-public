package uk.nhs.nhsx.keyfederation;

import java.time.LocalDate;

public class FederationBatch {
    public final BatchTag batchTag;
    public final LocalDate batchDate;

    public FederationBatch(BatchTag batchTag, LocalDate batchDate) {
        this.batchTag = batchTag;
        this.batchDate = batchDate;
    }
}
