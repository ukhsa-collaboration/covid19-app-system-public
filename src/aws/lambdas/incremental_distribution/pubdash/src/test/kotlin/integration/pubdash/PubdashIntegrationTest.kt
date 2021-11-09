package integration.pubdash

import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.services.athena.AmazonAthenaClient
import com.amazonaws.services.s3.AmazonS3Client
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.isContainedIn
import strikt.assertions.isEqualTo
import strikt.assertions.isNotBlank
import strikt.assertions.isSuccess
import uk.nhs.nhsx.core.Json
import uk.nhs.nhsx.core.aws.s3.AwsS3Client
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.s3.Locator
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.pubdash.Dataset
import uk.nhs.nhsx.pubdash.Dataset.Agnostic
import uk.nhs.nhsx.pubdash.Dataset.AppUsageDataByCountry
import uk.nhs.nhsx.pubdash.Dataset.AppUsageDataByLocalAuthority
import uk.nhs.nhsx.pubdash.Dataset.Country
import uk.nhs.nhsx.pubdash.Dataset.LocalAuthority
import uk.nhs.nhsx.pubdash.QueryId
import uk.nhs.nhsx.pubdash.QueryResult
import uk.nhs.nhsx.pubdash.persistence.AnalyticsDao
import uk.nhs.nhsx.pubdash.persistence.AthenaAsyncDbClient
import java.io.StringReader
import java.nio.file.Path
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.io.path.readText

class PubdashIntegrationTest {

    /* Note: run rake login:aa-dev before running these tests */
    private val targetEnvironment = "aa-ci"
    private val athenaOutputBucketName = BucketName.of("${targetEnvironment}-pubdash-athena-output")
    private val analyticsDevProfile = "analytics-aa-dev-ApplicationDeploymentUser"
    private val profileCredentialsProvider = ProfileCredentialsProvider(analyticsDevProfile)

    private companion object {
        private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        private val expectedCountries = listOf("England / Lloegr", "Wales / Cymru", "Wales / Cymru")
    }

    private val analyticsDao = AnalyticsDao(
        workspace = targetEnvironment,
        asyncDbClient = AthenaAsyncDbClient(
            athena = AmazonAthenaClient.builder()
                .withCredentials(profileCredentialsProvider)
                .build(),
            workgroup = "${targetEnvironment}_pubdash-athena-workgroup"
        ),
        mobileAnalyticsTable = "analytics_mobile"
    )

    private val s3Client = AwsS3Client(
        events = RecordingEvents(),
        client = AmazonS3Client.builder().withCredentials(profileCredentialsProvider).build()
    )

    @Test
    fun `agnostic dataset query returns expected headers`() {
        val results = runQueryAndExtractResults(Agnostic)

        val expectedHeaders = listOf(
            "Week starting (Wythnos yn dechrau)",
            "Week ending (Wythnos yn gorffen)",
            "Number of app downloads (Nifer o lawrlwythiadau ap)",
            "Number of venues the app has sent alerts about (Nifer o leoliadau mae’r ap wedi anfon hysbysiadau amdanynt)",
            "Number of NHS QR posters created (Nifer o bosteri cod QR y GIG a grëwyd)",
            "Cumulative number of app downloads (Nifer o lawrlwythiadau ap cronnus)",
            "Cumulative number of 'at risk' venues triggering venue alerts (Nifer o leoliadau 'dan risg' cronnus)",
            "Cumulative number of NHS QR posters created (Nifer o bosteri cod QR y GIG a grëwyd cronnus)"
        )
        expectThat(results.headerNames).isEqualTo(expectedHeaders)
    }

    @Test
    fun `country dataset query returns expected headers`() {
        val results = runQueryAndExtractResults(Country)

        val expectedHeaders = listOf(
            "Week starting (Wythnos yn dechrau)",
            "Week ending (Wythnos yn gorffen)",
            "Country (Wlad)",
            "Check-ins (Cofrestriadau)",
            "Symptoms reported (Symptomau a adroddwyd)",
            "Positive test results linked to app (Canlyniadau prawf positif)",
            "Negative test results linked to app (Canlyniadau prawf negatif)",
            "Contact tracing alert (Hysbysiadau olrhain cyswllt)",
            "Positive PCR test results linked to app (Canlyniadau prawf PCR positif cysylltiedig i ap)",
            "Positive LFD test results linked to app (Canlyniadau prawf LFD positif cysylltiedig i ap)",
            "Cumulative check-ins (Cofrestriadau cronnus)",
            "Cumulative symptoms reported (Symptomau a adroddwyd cronnus)",
            "Cumulative positive test results linked to app (Canlyniadau prawf positif cronnus)",
            "Cumulative negative test results linked to app (Canlyniadau prawf negatif cronnus)",
            "Cumulative contact tracing alert (Hysbysiadau olrhain cyswllt cronnus)",
            "Cumulative Positive PCR test results linked to app (Canlyniadau prawf PCR positif cronnol cysylltiedig i ap)",
            "Cumulative Positive LFD test results linked to app (Canlyniadau prawf LFD positif cronnol cysylltiedig i ap)"
        )
        expectThat(results.headerNames).isEqualTo(expectedHeaders)

        results.records.forEach {
            val weekStarting = it.get("Week starting (Wythnos yn dechrau)")
            expect { catching { LocalDate.parse(weekStarting, dateFormatter) }.isSuccess() }

            val weekEnding = it.get("Week ending (Wythnos yn gorffen)")
            expect { catching { LocalDate.parse(weekEnding, dateFormatter) }.isSuccess() }

            val country = it.get("Country (Wlad)")
            expectThat(country).isContainedIn(expectedCountries)
        }
    }

    @Test
    fun `local authority dataset query returns expected headers`() {
        val results = runQueryAndExtractResults(LocalAuthority)

        val expectedHeaders = listOf(
            "Week starting (Wythnos yn dechrau)",
            "Week ending (Wythnos yn gorffen)",
            "Local authority (Awdurdod lleol)",
            "Country (Wlad)",
            "Check-ins (Cofrestriadau)",
            "Symptoms reported (Symptomau a adroddwyd)",
            "Positive test results linked to app (Canlyniadau prawf positif)",
            "Negative test results linked to app (Canlyniadau prawf negatif)",
            "Contact tracing alert (Hysbysiadau olrhain cyswllt)",
            "Positive PCR test results linked to app (Canlyniadau prawf PCR positif cysylltiedig i ap)",
            "Positive LFD test results linked to app (Canlyniadau prawf LFD positif cysylltiedig i ap)",
            "Cumulative check-ins (Cofrestriadau cronnus)",
            "Cumulative symptoms reported (Symptomau a adroddwyd cronnus)",
            "Cumulative positive test results linked to app (Canlyniadau prawf positif cronnus)",
            "Cumulative negative test results linked to app (Canlyniadau prawf negatif cronnus)",
            "Cumulative contact tracing alert (Hysbysiadau olrhain cyswllt cronnus)",
            "Cumulative Positive PCR test results linked to app (Canlyniadau prawf PCR positif cronnol cysylltiedig i ap)",
            "Cumulative Positive LFD test results linked to app (Canlyniadau prawf LFD positif cronnol cysylltiedig i ap)"
        )
        expectThat(results.headerNames).isEqualTo(expectedHeaders)

        results.records.forEach {
            val weekStarting = it.get("Week starting (Wythnos yn dechrau)")
            expect { catching { LocalDate.parse(weekStarting, dateFormatter) }.isSuccess() }

            val weekEnding = it.get("Week ending (Wythnos yn gorffen)")
            expect { catching { LocalDate.parse(weekEnding, dateFormatter) }.isSuccess() }

            val country = it.get("Country (Wlad)")
            expectThat(country).isContainedIn(expectedCountries)

            val localAuthority = it.get("Local authority (Awdurdod lleol)")
            expectThat(localAuthority).isNotBlank()
        }
    }

    @Test
    fun `app usage data by local authority dataset query returns expected headers`() {
        val results = runQueryAndExtractResults(AppUsageDataByLocalAuthority)

        val expectedHeaders = listOf(
            "Week starting (Wythnos yn dechrau)",
            "Week ending (Wythnos yn gorffen)",
            "Country (Wlad)",
            "Local authority (Awdurdod lleol)",
            "Average Daily Number of Users With App Installed (Cyfartaledd Nifer o Ddefnyddwyr Dyddiol gyda'r Ap wedi ei Osod)",
            "Average Daily Number of Users Where App is Contact Traceable (Cyfartaledd Nifer o Ddefnyddwyr Dyddiol ble mae'r Ap yn Caniatáu Olrhain Cyswllt)",
            "Average Daily Number of Users With App Installed as Percentage of Population (Cyfartaledd Nifer o Ddefnyddwyr Dyddiol gyda'r Ap wedi ei Osod fel Canran o'r Boblogaeth)",
            "Average Daily Number of Users Where App is Contact Traceable as Percentage of Population (Cyfartaledd Nifer o Ddefnyddwyr Dyddiol ble mae'r Ap yn Caniatáu Olrhain Cyswllt fel Canran o'r Boblogaeth)"
        )
        expectThat(results.headerNames).isEqualTo(expectedHeaders)

        results.records.forEach {
            val weekStarting = it.get("Week starting (Wythnos yn dechrau)")
            expect { catching { LocalDate.parse(weekStarting, dateFormatter) }.isSuccess() }

            val weekEnding = it.get("Week ending (Wythnos yn gorffen)")
            expect { catching { LocalDate.parse(weekEnding, dateFormatter) }.isSuccess() }

            val country = it.get("Country (Wlad)")
            expectThat(country).isContainedIn(expectedCountries)

            val localAuthority = it.get("Local authority (Awdurdod lleol)")
            expectThat(localAuthority).isNotBlank()
        }
    }

    @Test
    fun `app usage data by country dataset query returns expected headers`() {
        val results = runQueryAndExtractResults(AppUsageDataByCountry)

        val expectedHeaders = listOf(
            "Week starting (Wythnos yn dechrau)",
            "Week ending (Wythnos yn gorffen)",
            "Country (Wlad)",
            "Average Daily Number of Users With App Installed (Cyfartaledd Nifer o Ddefnyddwyr Dyddiol gyda'r Ap wedi ei Osod)",
            "Average Daily Number of Users Where App is Contact Traceable (Cyfartaledd Nifer o Ddefnyddwyr Dyddiol ble mae'r Ap yn Caniatáu Olrhain Cyswllt)",
            "Average Daily Number of Users With App Installed as Percentage of Population (Cyfartaledd Nifer o Ddefnyddwyr Dyddiol gyda'r Ap wedi ei Osod fel Canran o'r Boblogaeth)",
            "Average Daily Number of Users Where App is Contact Traceable as Percentage of Population (Cyfartaledd Nifer o Ddefnyddwyr Dyddiol ble mae'r Ap yn Caniatáu Olrhain Cyswllt fel Canran o'r Boblogaeth)"
        )
        expectThat(results.headerNames).isEqualTo(expectedHeaders)

        results.records.forEach {
            val weekStarting = it.get("Week starting (Wythnos yn dechrau)")
            expect { catching { LocalDate.parse(weekStarting, dateFormatter) }.isSuccess() }

            val weekEnding = it.get("Week ending (Wythnos yn gorffen)")
            expect { catching { LocalDate.parse(weekEnding, dateFormatter) }.isSuccess() }

            val country = it.get("Country (Wlad)")
            expectThat(country).isContainedIn(expectedCountries)
        }
    }

    private fun runQueryAndExtractResults(dataset: Dataset): CSVParser {
        val queryId = when (dataset) {
            Agnostic -> analyticsDao.startAgnosticDatasetQueryAsync()
            Country -> analyticsDao.startCountryDatasetQueryAsync()
            LocalAuthority -> analyticsDao.startLocalAuthorityDatasetQueryAsync()
            AppUsageDataByLocalAuthority -> analyticsDao.startAppUsageDataByLocalAuthorityDatasetQueryAsync()
            AppUsageDataByCountry -> analyticsDao.startAppUsageDataByCountryDatasetQueryAsync()
        }
        waitForQueryToFinish(queryId)

        val locator = Locator.of(athenaOutputBucketName, ObjectKey.of("${queryId.id}.csv"))
        val s3Object = s3Client.getObject(locator)
            .orElseThrow { RuntimeException("Object $locator not found") }

        val csvContent = s3Object.objectContent.bufferedReader().readText()

        return CSVFormat.RFC4180
            .withFirstRecordAsHeader()
            .parse(StringReader(csvContent))
    }

    private fun waitForQueryToFinish(queryId: QueryId) {
        while (true) {
            when (val state = analyticsDao.checkQueryState(queryId)) {
                is QueryResult.Waiting -> Thread.sleep(1000)
                is QueryResult.Error -> throw RuntimeException("Error:${state.message}, executing query:$queryId")
                is QueryResult.Finished -> return
            }
        }
    }
}
