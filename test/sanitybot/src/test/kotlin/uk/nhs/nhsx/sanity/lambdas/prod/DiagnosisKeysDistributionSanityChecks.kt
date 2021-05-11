package uk.nhs.nhsx.sanity.lambdas.prod

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request
import uk.nhs.nhsx.sanity.lambdas.LambdaSanityCheck
import uk.nhs.nhsx.sanity.lambdas.config.DeployedLambda
import uk.nhs.nhsx.sanity.lambdas.config.Distribution
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class DiagnosisKeysDistributionSanityChecks: LambdaSanityCheck() {

    @Test
    fun `Daily Diagnosis distribution returns a 200 and matches resource`() {
        val numberOfFiles = numberOfDaysSinceBeginning()
        val lambda = env.configFor(DeployedLambda.DiagnosisKeysDistribution, "diagnosis_keys_distribution_daily") as Distribution

        assertThat(countObjectsIn(lambda, "distribution/daily"), equalTo(numberOfFiles.toInt()))
    }

    @MethodSource("getDailyEndPointSuffixes")
    @ParameterizedTest(name = "Daily Diagnosis distribution returns a 200 {arguments}")
    fun `Daily Diagnosis distribution returns a 200`(endpointSuffix:String) {

        val lambda = env.configFor(DeployedLambda.DiagnosisKeysDistribution, "diagnosis_keys_distribution_daily") as Distribution
        val endPointURL = "${lambda.endpointUri}/${endpointSuffix}.zip"
        assertThat(insecureClient(Request(Method.GET, endPointURL)),
            hasStatus(Status.OK)
        )
    }

    private fun countObjectsIn(lambda: Distribution, type: String): Int {
        val s3 = S3Client.builder().build()
        return s3.listObjectsV2(ListObjectsV2Request.builder().bucket(lambda.storeName).build()).contents()
            .filter { it.key().startsWith(type) }.size
    }

    private fun numberOfDaysSinceBeginning(): Long {
        val sep01 = LocalDate.of(2020, 9, 1)

        val tomorrowMidnight = LocalDate
            .ofInstant(Instant.now(), ZoneId.of("UTC"))
            .plusDays(1)
        return (ChronoUnit.DAYS.between(sep01, tomorrowMidnight)+1)
    }

    @Suppress("unused")
    companion object{
        @JvmStatic
        private fun getDailyEndPointSuffixes(): List<String> {
            val tomorrowMidnight = LocalDateTime
                .ofInstant(Instant.now(), ZoneId.of("UTC"))
                .plusDays(1)
                .withHour(0)

            val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd00")

            return (0..14)
                .map { tomorrowMidnight.minusDays(it.toLong()).toLocalDate() }
                .map {it.format(dateTimeFormatter)}
        }
    }
}
