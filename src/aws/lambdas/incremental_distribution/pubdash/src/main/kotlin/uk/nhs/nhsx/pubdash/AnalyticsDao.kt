package uk.nhs.nhsx.pubdash

import uk.nhs.nhsx.pubdash.datasets.AnalyticsDataSet
import uk.nhs.nhsx.pubdash.datasets.CountryAgnosticDataset
import uk.nhs.nhsx.pubdash.datasets.CountryAgnosticRow
import uk.nhs.nhsx.pubdash.datasets.CountrySpecificDataset
import java.time.LocalDate

class AnalyticsDao(private val workspace: String, private val dbClient: DbClient) : AnalyticsDataSet {

    override fun countryAgnosticDataset(): CountryAgnosticDataset {
        val results = dbClient.query(
            """
            SELECT
                CASE day_of_week(appdate)
                    WHEN 7 THEN appdate + interval '3' day /* Sun */
                    WHEN 1 THEN appdate + interval '2' day /* Mon */
                    WHEN 2 THEN appdate + interval '1' day /* Tue */
                    WHEN 3 THEN appdate /* Wed */
                    WHEN 4 THEN appdate + interval '6' day /* Thu */
                    WHEN 5 THEN appdate + interval '5' day /* Fri */
                    WHEN 6 THEN appdate + interval '4' day /* Sat */
                END AS lastDayReportingWeek,
                CASE WHEN SUM(downloads) < 5 AND sum(downloads) > 0 THEN 5 ELSE SUM(downloads) END as downloads,
                CASE WHEN SUM(risky_venue) < 5 AND SUM(risky_venue) > 0 THEN 5 ELSE SUM(risky_venue) END as risky_venue,
                CASE WHEN SUM(posters) < 5 AND SUM(posters) > 0 THEN 5 ELSE SUM(posters) END as posters
            FROM(
                SELECT
                    appdate,
                    SUM(downloads) AS downloads,
                    SUM(risky_venue) AS risky_venue
                FROM(
                    SELECT
                        date_parse(app.date, '%Y-%c-%d') AS appdate,
                        app.platform AS platform,
                        CASE
                            WHEN app.platform = 'Android' OR  app.platform = 'Apple' THEN app.downloads
                        END AS downloads,
                        CASE
                            WHEN app.platform = 'Website' THEN app.opt_in_proportion END AS risky_venue
                    FROM "${workspace}_analytics_db"."${workspace}_analytics_app_store" AS app)
                    GROUP BY appdate)
                FULL OUTER JOIN(
                    SELECT
                        psdate,
                        count(*) AS posters
                    FROM(
                        SELECT
                            date_parse(substring(ps.created,1,10), '%Y-%c-%d') AS psdate
                        FROM "${workspace}_analytics_db"."${workspace}_analytics_qr_posters" AS ps)
                    GROUP BY psdate)
                ON appdate = psdate
            WHERE appdate <=
                        (CASE day_of_week(current_date)
                            WHEN 7 THEN current_date - interval '11' day /* Sun */
                            WHEN 1 THEN current_date - interval '5' day /* Mon */
                            WHEN 2 THEN current_date - interval '6' day /* Tue */
                            WHEN 3 THEN current_date - interval '7' day /* Wed */
                            WHEN 4 THEN current_date - interval '8' day /* Thu */
                            WHEN 5 THEN current_date - interval '9' day /* Fri */
                            WHEN 6 THEN current_date - interval '10' day /* Sat */
                        END)
            GROUP BY
            CASE day_of_week(appdate)
                    WHEN 7 THEN appdate + interval '3' day /* Sun */
                    WHEN 1 THEN appdate + interval '2' day /* Mon */
                    WHEN 2 THEN appdate + interval '1' day /* Tue */
                    WHEN 3 THEN appdate /* Wed */
                    WHEN 4 THEN appdate + interval '6' day /* Thu */
                    WHEN 5 THEN appdate + interval '5' day /* Fri */
                    WHEN 6 THEN appdate + interval '4' day /* Sat */
                END
        """
        )

        return CountryAgnosticDataset(
            results.resultSet.rows.drop(1).map {
                CountryAgnosticRow(
                    weekEnding = LocalDate.parse(it.data[0].varCharValue),
                    downloads = it.data[1].varCharValue.toInt(),
                    riskyVenues = it.data[2].varCharValue.toInt(),
                    posters = it.data[3].varCharValue.toInt()
                )
            }
        )
    }

    override fun countrySpecificDataset(): CountrySpecificDataset = CountrySpecificDataset(emptyList())
}
