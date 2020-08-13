package uk.nhs.nhsx.diagnosiskeydist;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class DistributionServiceWindow {
	private static final TimeZone TIME_ZONE_UTC = TimeZone.getTimeZone("UTC");

	public static final int ZIP_SUBMISSION_PERIOD_OFFSET_MINUTES = -15;
	private static final int DISTRIBUTION_EARLIEST_START_OFFSET_MINUTES = ZIP_SUBMISSION_PERIOD_OFFSET_MINUTES + 1;
	private static final int DISTRIBUTION_LATEST_START_OFFSET_MINUTES = ZIP_SUBMISSION_PERIOD_OFFSET_MINUTES + 3;
	private static final int DISTRIBUTION_FREQUENCY_HOURS = 2;

	private final Date now;

	public DistributionServiceWindow(Date now) {
		this.now = now;
	}

	public Date nextFullHour() {
		Calendar cal = Calendar.getInstance(TIME_ZONE_UTC);
		cal.setTime(now);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);

		int hour = cal.get(Calendar.HOUR_OF_DAY);
		cal.set(Calendar.HOUR_OF_DAY, hour - hour % 2);
		cal.add(Calendar.HOUR_OF_DAY, 2);

		return cal.getTime();
	}

	public Date zipExpirationExclusive() {
		Calendar cal = Calendar.getInstance(TIME_ZONE_UTC);
		cal.setTime(nextFullHour());

		cal.add(Calendar.HOUR_OF_DAY, DISTRIBUTION_FREQUENCY_HOURS);

		return cal.getTime();
	}


	public Date earliestBatchStartDateWithinHourInclusive() {
		Calendar cal = Calendar.getInstance(TIME_ZONE_UTC);
		cal.setTime(nextFullHour());

		cal.add(Calendar.MINUTE, DISTRIBUTION_EARLIEST_START_OFFSET_MINUTES);

		return cal.getTime();
	}

	public Date latestBatchStartDateWithinHourExclusive() {
		Calendar cal = Calendar.getInstance(TIME_ZONE_UTC);
		cal.setTime(nextFullHour());

		cal.add(Calendar.MINUTE, DISTRIBUTION_LATEST_START_OFFSET_MINUTES);

		return cal.getTime();
	}

	public boolean validBatchStartDate() {
		return (!now.before(earliestBatchStartDateWithinHourInclusive())) && now.before(latestBatchStartDateWithinHourExclusive());
	}
}
