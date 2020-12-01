package uk.nhs.nhsx.diagnosiskeydist.apispec;

import java.util.Date;
import java.util.List;

public abstract class ZIPSubmissionPeriod {
	protected final Date periodEndDateExclusive;

	public ZIPSubmissionPeriod(Date periodEndDateExclusive) {
		this.periodEndDateExclusive = periodEndDateExclusive;
	}

	public abstract String zipPath();
	public abstract boolean isCoveringSubmissionDate(Date diagnosisKeySubmissionDate, int periodOffsetMinutes); //for the server for calculation
	public abstract List<? extends ZIPSubmissionPeriod> allPeriodsToGenerate();
	public abstract Date getEndExclusive(); // < end from client perspective -> url
	public abstract Date getStartInclusive(); // >= start from client perspective
}
