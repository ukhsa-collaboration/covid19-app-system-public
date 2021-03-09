package uk.nhs.nhsx.diagnosiskeydist.agspec;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static java.lang.String.format;

/**
 * see https://covid19-static.cdn-apple.com/applications/covid19/current/static/contact-tracing/pdf/ExposureNotification-CryptographySpecificationv1.2.pdf
 * see https://en.wikipedia.org/wiki/Unix_time
 * see https://developer.apple.com/documentation/exposurenotification/setting_up_an_exposure_notification_server
 * see https://developers.google.com/android/exposure-notifications/exposure-key-file-format
 */
public class ENIntervalNumber {

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
    public static ENIntervalNumber enIntervalNumberFromTimestamp(Instant timestamp) {
        return enIntervalNumberFromTimestampInMillis(timestamp.toEpochMilli());
    }

    /**
     * "This function provides a number for each 10 minute time window that’s shared between all devices participating in the protocol"
     *
     * @param timestampMillis "the milliseconds since January 1, 1970, 00:00:00 GMT."
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

    public Instant toTimestamp() {
        return Instant.ofEpochMilli(toTimestampInMillis());
    }

    /**
     * @return true, if <code>diagnosisKeyEnIntervalNumber</code> is valid until <code>date</code>
     */
    public boolean validUntil(Instant instant) {
        if (enIntervalNumber > enIntervalNumberFromTimestamp(instant).enIntervalNumber) {
            return false;
        }

        // check this. Didn't we agree on 15 days, instead of 14? -> yes, 14 from enInverval (start) + TEKRollingPeriod (1 day)
        return enIntervalNumber + TEKRollingPeriod > enIntervalNumberFromTimestamp(instant).enIntervalNumber - MAX_DIAGNOSIS_KEY_AGE_DAYS * TEKRollingPeriod;
    }

    @Override
    public String toString() {
        return format("ENIntervalNumber(%d: %s)", enIntervalNumber, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneOffset.UTC)
            .format(toTimestamp()));
    }
}
