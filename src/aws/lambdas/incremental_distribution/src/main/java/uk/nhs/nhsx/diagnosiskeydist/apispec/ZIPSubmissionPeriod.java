package uk.nhs.nhsx.diagnosiskeydist.apispec;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public interface ZIPSubmissionPeriod {
    String zipPath();

    boolean isCoveringSubmissionDate(Instant diagnosisKeySubmission, Duration periodOffset); //for the server for calculation

    List<? extends ZIPSubmissionPeriod> allPeriodsToGenerate();

    Instant getEndExclusive(); // < end from client perspective -> url

    Instant getStartInclusive(); // >= start from client perspective
}
