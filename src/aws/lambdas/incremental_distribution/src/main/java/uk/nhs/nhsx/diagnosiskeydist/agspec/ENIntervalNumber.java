package uk.nhs.nhsx.diagnosiskeydist.agspec;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * see https://covid19-static.cdn-apple.com/applications/covid19/current/static/contact-tracing/pdf/ExposureNotification-CryptographySpecificationv1.2.pdf
 * see https://en.wikipedia.org/wiki/Unix_time
 * see https://developer.apple.com/documentation/exposurenotification/setting_up_an_exposure_notification_server
 * see https://developers.google.com/android/exposure-notifications/exposure-key-file-format
 */
public class ENIntervalNumber {
    private static final TimeZone TIME_ZONE_UTC = TimeZone.getTimeZone("UTC");

    /**
     * "The duration for which a Temporary Exposure Key is valid (in multiples of 10 minutes)"
     */
    public static final int TEKRollingPeriod = 144;

    /**
     * Number of days Diagnosis Keys are valid
     */
    public static final int MAX_DIAGNOSIS_KEY_AGE_DAYS = 14;

    private final long enIntervalNumber;

    public ENIntervalNumber(long enIntervalNumber) {
        this.enIntervalNumber = enIntervalNumber;
    }

    public long getEnIntervalNumber() {
        return this.enIntervalNumber;
    }

    /**
     * "This function provides a number for each 10 minute time window that’s shared between all devices participating in the protocol"
     */
    public static ENIntervalNumber enIntervalNumberFromTimestamp(Date timestamp) {
        return enIntervalNumberFromTimestampInMillis(timestamp.getTime());
    }

    /**
     * "This function provides a number for each 10 minute time window that’s shared between all devices participating in the protocol"
     *
     * @param timestampMillis "the milliseconds since January 1, 1970, 00:00:00 GMT." (e.g. System.currentTimeMillis())
     */
    public static ENIntervalNumber enIntervalNumberFromTimestampInMillis(long timestampMillis) {
        return enIntervalNumberFromTimestampInUnixEpochTime(timestampMillis / 1000);
    }

    /**
     * "This function provides a number for each 10 minute time window that’s shared between all devices participating in the protocol"
     *
     * @param timestampSeconds "the number of seconds that have elapsed since the Unix epoch, minus leap seconds; the Unix epoch is 00:00:00 UTC on 1 January 1970."
     */
    public static ENIntervalNumber enIntervalNumberFromTimestampInUnixEpochTime(long timestampSeconds) {
        return new ENIntervalNumber(timestampSeconds / (60 * 10));
    }

    public long toTimestampInUnixEpochTime() {
        return enIntervalNumber * 10 * 60L;
    }

    public long toTimestampInMillis() {
        return toTimestampInUnixEpochTime() * 1000;
    }

    public Date toTimestamp() {
        return new Date(toTimestampInMillis());
    }

    /**
     * @return true, if <code>diagnosisKeyEnIntervalNumber</code> is valid until <code>date</code>
     */
    public boolean validUntil(Date date) {
        if (enIntervalNumber > enIntervalNumberFromTimestamp(date).enIntervalNumber) {
            return false;
        }

        // check this. Didn't we agree on 15 days, instead of 14? -> yes, 14 from enInverval (start) + TEKRollingPeriod (1 day)
        return enIntervalNumber + TEKRollingPeriod > enIntervalNumberFromTimestamp(date).enIntervalNumber - MAX_DIAGNOSIS_KEY_AGE_DAYS * TEKRollingPeriod;
    }

    @Override
    public String toString() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        simpleDateFormat.setTimeZone(TIME_ZONE_UTC);

        return "ENIntervalNumber(" + enIntervalNumber + ": " + simpleDateFormat.format(toTimestamp()) + ")";
    }
}
