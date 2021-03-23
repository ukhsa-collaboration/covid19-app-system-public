package uk.nhs.nhsx.diagnosiskeydist.agspec

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * see https://covid19-static.cdn-apple.com/applications/covid19/current/static/contact-tracing/pdf/ExposureNotification-CryptographySpecificationv1.2.pdf
 * see https://en.wikipedia.org/wiki/Unix_time
 * see https://developer.apple.com/documentation/exposurenotification/setting_up_an_exposure_notification_server
 * see https://developers.google.com/android/exposure-notifications/exposure-key-file-format
 */
class ENIntervalNumber(val enIntervalNumber: Long) {
    fun toTimestampInUnixEpochTime(): Long = enIntervalNumber * 10 * 60L

    fun toTimestampInMillis(): Long = toTimestampInUnixEpochTime() * 1000

    fun toTimestamp(): Instant = Instant.ofEpochMilli(toTimestampInMillis())

    /**
     * @return true, if `diagnosisKeyEnIntervalNumber` is valid until `date`
     */
    fun validUntil(instant: Instant) =
        // check this. Didn't we agree on 15 days, instead of 14? -> yes, 14 from enInverval (start) + TEKRollingPeriod (1 day)
        when {
            enIntervalNumber > enIntervalNumberFromTimestamp(instant).enIntervalNumber -> false
            else -> enIntervalNumber + TEKRollingPeriod > enIntervalNumberFromTimestamp(instant).enIntervalNumber - MAX_DIAGNOSIS_KEY_AGE_DAYS * TEKRollingPeriod
        }

    // TODO [DD] is this needed??
    override fun toString(): String = String.format(
        "ENIntervalNumber(%d: %s)", enIntervalNumber, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneOffset.UTC)
            .format(toTimestamp())
    )

    companion object {
        /**
         * "The duration for which a Temporary Exposure Key is valid (in multiples of 10 minutes)"
         */
        const val TEKRollingPeriod = 144

        /**
         * Number of days Diagnosis Keys are valid
         */
        const val MAX_DIAGNOSIS_KEY_AGE_DAYS = 14

        /**
         * "This function provides a number for each 10 minute time window that’s shared between all devices participating in the protocol"
         */
        fun enIntervalNumberFromTimestamp(timestamp: Instant): ENIntervalNumber =
            enIntervalNumberFromTimestampInMillis(timestamp.toEpochMilli())

        /**
         * "This function provides a number for each 10 minute time window that’s shared between all devices participating in the protocol"
         *
         * @param timestampMillis "the milliseconds since January 1, 1970, 00:00:00 GMT."
         */
        fun enIntervalNumberFromTimestampInMillis(timestampMillis: Long): ENIntervalNumber =
            enIntervalNumberFromTimestampInUnixEpochTime(timestampMillis / 1000)

        /**
         * "This function provides a number for each 10 minute time window that’s shared between all devices participating in the protocol"
         *
         * @param timestampSeconds "the number of seconds that have elapsed since the Unix epoch, minus leap seconds; the Unix epoch is 00:00:00 UTC on 1 January 1970."
         */
        fun enIntervalNumberFromTimestampInUnixEpochTime(timestampSeconds: Long): ENIntervalNumber =
            ENIntervalNumber(timestampSeconds / (60 * 10))
    }
}
