package uk.nhs.nhsx.diagnosiskeydist.apispec;

import uk.nhs.nhsx.diagnosiskeydist.agspec.ENIntervalNumber;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

public class DailyZIPSubmissionPeriod extends ZIPSubmissionPeriod {
	private static final TimeZone TIME_ZONE_UTC = TimeZone.getTimeZone("UTC");
	private static final String DAILY_PATH_PREFIX = "distribution/daily/";
	private static final int TOTAL_DAILY_ZIPS = ENIntervalNumber.MAX_DIAGNOSIS_KEY_AGE_DAYS;

	public DailyZIPSubmissionPeriod(Date dailyPeriodEndDate) {
		super(assertValid(dailyPeriodEndDate));
	}

	private static Date assertValid(Date dailyPeriodEndDate) {
		Calendar cal = utcCalendar(dailyPeriodEndDate);

		if (cal.get(Calendar.HOUR_OF_DAY) != 0) throw new IllegalStateException();
		if (cal.get(Calendar.MINUTE) != 0) throw new IllegalStateException();
		if (cal.get(Calendar.SECOND) != 0) throw new IllegalStateException();
		if (cal.get(Calendar.MILLISECOND) != 0) throw new IllegalStateException();

		return dailyPeriodEndDate;
	}

	public String zipPath() {
		StringBuilder sb = new StringBuilder();
		sb.append(DAILY_PATH_PREFIX);
		sb.append(dailyKey());
		sb.append(".zip");

		return sb.toString();
	}

	private String dailyKey() {
		return hourlyFormat().format(periodEndDateExclusive);
	}

	public boolean isCoveringSubmissionDate(Date diagnosisKeySubmissionDate, int twoHourlyDateOffsetMinutes) {
		Calendar cal = utcCalendar(periodEndDateExclusive);
		cal.add(Calendar.MINUTE, twoHourlyDateOffsetMinutes);

		Date toExclusive = cal.getTime();

		cal.add(Calendar.DATE, -1);
		Date fromInclusive = cal.getTime();

		return diagnosisKeySubmissionDate.getTime() >= fromInclusive.getTime() && diagnosisKeySubmissionDate.getTime() < toExclusive.getTime();
	}

	/**
	 * returns DailyPeriods for the past 14 days
	 */
	public List<DailyZIPSubmissionPeriod> allPeriodsToGenerate() {
		List<DailyZIPSubmissionPeriod> periods = new LinkedList<>();

		Calendar cal = utcCalendar(periodEndDateExclusive);
		for (int i = 0; i < TOTAL_DAILY_ZIPS + 1; i++) {
			periods.add(new DailyZIPSubmissionPeriod(cal.getTime()));
			cal.add(Calendar.DATE, -1);
		}

		return periods;
	}

	public static DailyZIPSubmissionPeriod periodForSubmissionDate(Date diagnosisKeySubmissionDate) {
		Calendar cal = utcCalendar(diagnosisKeySubmissionDate);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);

		cal.add(Calendar.DATE, 1);

		return new DailyZIPSubmissionPeriod(cal.getTime());
	}

	private static SimpleDateFormat hourlyFormat() {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd00");
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
		cal.add(Calendar.DATE, -1);

		return cal.getTime();
	}

	@Override
	public String toString() {
		return "1 day: from " + hourlyFormat().format(getStartInclusive()) + " (inclusive) to " + hourlyFormat().format(getEndExclusive()) + " (exclusive)";
	}
}
