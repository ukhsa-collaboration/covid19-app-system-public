package uk.nhs.nhsx.diagnosiskeydist.apispec;

import uk.nhs.nhsx.diagnosiskeydist.agspec.ENIntervalNumber;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

public class TwoHourlyZIPSubmissionPeriod extends ZIPSubmissionPeriod {
	private static final TimeZone TIME_ZONE_UTC = TimeZone.getTimeZone("UTC");
	private static final String TWO_HOURLY_PATH_PREFIX = "distribution/two-hourly/";
	private static final int TOTAL_TWO_HOURLY_ZIPS = ENIntervalNumber.MAX_DIAGNOSIS_KEY_AGE_DAYS * 12;

	public TwoHourlyZIPSubmissionPeriod(Date twoHourlyDate) {
		super(assertValid(twoHourlyDate));
	}

	private static Date assertValid(Date twoHourlyDate) {
		Calendar cal = utcCalendar(twoHourlyDate);

		if (cal.get(Calendar.MINUTE) != 0) throw new IllegalStateException();
		if (cal.get(Calendar.SECOND) != 0) throw new IllegalStateException();
		if (cal.get(Calendar.MILLISECOND) != 0) throw new IllegalStateException();
		if (cal.get(Calendar.HOUR_OF_DAY) % 2 == 1) throw new IllegalStateException();

		return twoHourlyDate;
	}

	public String zipPath() {
		StringBuilder sb = new StringBuilder();
		sb.append(TWO_HOURLY_PATH_PREFIX);
		sb.append(twoHourlyKey());
		sb.append(".zip");

		return sb.toString();
	}

	private String twoHourlyKey() {
		return hourlyFormat().format(periodEndDateExclusive);
	}

	/**
	 * @return true, if <code>diagnosisKeySubmissionDate</code> is covered by the two-hourly period represented by (<code>twoHourlyDate</code> shifted by <code>twoHourlyDateOffsetMinutes</code>)
	 */
	public boolean isCoveringSubmissionDate(Date diagnosisKeySubmissionDate, int twoHourlyDateOffsetMinutes) {
		Calendar cal = utcCalendar(periodEndDateExclusive);
		cal.add(Calendar.MINUTE, twoHourlyDateOffsetMinutes);

		Date toExclusive = cal.getTime();

		cal.add(Calendar.HOUR_OF_DAY, -2);
		Date fromInclusive = cal.getTime();

		return diagnosisKeySubmissionDate.getTime() >= fromInclusive.getTime() && diagnosisKeySubmissionDate.getTime() < toExclusive.getTime();
	}

	/**
	 * @return list of valid <code>TwoHourlyPeriod</code> ending with <code>this</code> <code>TwoHourlyPeriod</code>
	 */
	public List<TwoHourlyZIPSubmissionPeriod> allPeriodsToGenerate() {
		List<TwoHourlyZIPSubmissionPeriod> twoHourlyDates = new LinkedList<>();

		Calendar cal = utcCalendar(periodEndDateExclusive);
		for (int i = 0; i < TOTAL_TWO_HOURLY_ZIPS; i++) {
			twoHourlyDates.add(new TwoHourlyZIPSubmissionPeriod(cal.getTime()));

			cal.add(Calendar.HOUR_OF_DAY, -2);
		}

		return twoHourlyDates;
	}

	/**
	 * @return end date (exclusive) of the two-hourly period comprising the Diagnosis Keys posted to the Submission Service at <code>diagnosisKeySubmissionDate</code>
	 */
	public static TwoHourlyZIPSubmissionPeriod periodForSubmissionDate(Date diagnosisKeySubmissionDate) {
		Calendar cal = utcCalendar(diagnosisKeySubmissionDate);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);

		int hour = cal.get(Calendar.HOUR_OF_DAY);
		cal.set(Calendar.HOUR_OF_DAY, hour - hour % 2);

		cal.add(Calendar.HOUR_OF_DAY, 2);

		return new TwoHourlyZIPSubmissionPeriod(cal.getTime());
	}

	private static SimpleDateFormat hourlyFormat() {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHH");
		simpleDateFormat.setTimeZone(TIME_ZONE_UTC);

		return simpleDateFormat;
	}

	private static Calendar utcCalendar(Date dailyDate) {
		Calendar cal = Calendar.getInstance(TIME_ZONE_UTC);
		cal.setTime(dailyDate);

		return cal;
	}


	public Date getEndExclusive() {
		return periodEndDateExclusive;
	}

	public Date getStartInclusive() {
		Calendar cal = utcCalendar(periodEndDateExclusive);
		cal.add(Calendar.HOUR_OF_DAY, -2);

		return cal.getTime();
	}

	@Override
	public String toString() {
		return "2 hours: from " + hourlyFormat().format(getStartInclusive()) + " (inclusive) to " + hourlyFormat().format(getEndExclusive()) + " (exclusive)";
	}
}
