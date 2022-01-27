package uk.nhs.nhsx.localstats.scenario

import org.http4k.testing.Approver
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.nhs.nhsx.localstats.DailyLocalStats
import uk.nhs.nhsx.localstats.LocalStatsJson
import uk.nhs.nhsx.localstats.data.CoronavirusApi
import uk.nhs.nhsx.localstats.data.CoronavirusApiResponses.LTLA_REDCAR_DAILY_FIRST
import uk.nhs.nhsx.localstats.data.CoronavirusApiResponses.LTLA_REDCAR_DAILY_SECOND
import uk.nhs.nhsx.localstats.data.DownloadHandler
import uk.nhs.nhsx.localstats.data.FakeCoronavirusApiBackend
import uk.nhs.nhsx.localstats.data.Http
import uk.nhs.nhsx.localstats.domain.ReleaseDate
import uk.nhs.nhsx.testhelper.approvals.StrictJsonApprovalTest
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset.UTC

@ExtendWith(StrictJsonApprovalTest::class)
class DailyLocalStatsDocumentTest {

    private val clock = { Instant.parse("2021-11-16T17:00:00Z") }

    @Test
    fun `generates daily document`(approver: Approver) {
        val backend = FakeCoronavirusApiBackend()
        val coronavirusApi = CoronavirusApi.Http(backend)

        val localStats = DailyLocalStats(
            coronavirusApi = coronavirusApi,
            clock = clock
        )

        val document = localStats.generateDocument(ReleaseDate.of(clock()))
        val json = LocalStatsJson.asFormatString(document)

        approver.assertApproved(json)
    }

    @Test
    fun `with metrics not belonging to the same group`(approver: Approver) {
        val backend = FakeCoronavirusApiBackend(DownloadHandler(LTLA_REDCAR_DAILY_FIRST, LTLA_REDCAR_DAILY_SECOND))
        val coronavirusApi = CoronavirusApi.Http(backend)

        val localStats = DailyLocalStats(
            coronavirusApi = coronavirusApi,
            clock = clock
        )

        val document = localStats.generateDocument(ReleaseDate.of(clock()))
        val json = LocalStatsJson.asFormatString(document)

        approver.assertApproved(json)
    }
}
