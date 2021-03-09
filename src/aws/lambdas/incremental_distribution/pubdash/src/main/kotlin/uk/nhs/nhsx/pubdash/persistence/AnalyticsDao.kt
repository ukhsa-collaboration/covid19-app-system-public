package uk.nhs.nhsx.pubdash.persistence

import uk.nhs.nhsx.pubdash.CsvS3Object
import uk.nhs.nhsx.pubdash.QueryId
import uk.nhs.nhsx.pubdash.QueryResult
import uk.nhs.nhsx.pubdash.datasets.AgnosticDataset
import uk.nhs.nhsx.pubdash.datasets.AgnosticDatasetRow
import uk.nhs.nhsx.pubdash.datasets.AnalyticsSource
import uk.nhs.nhsx.pubdash.datasets.CountryDataset
import uk.nhs.nhsx.pubdash.datasets.CountryDatasetRow
import uk.nhs.nhsx.pubdash.datasets.LocalAuthorityDataset
import uk.nhs.nhsx.pubdash.datasets.LocalAuthorityDatasetRow
import java.time.LocalDate

class AnalyticsDao(private val workspace: String, private val asyncDbClient: AsyncDbClient) : AnalyticsSource {

    override fun startAgnosticDatasetQueryAsync(): QueryId = asyncDbClient.submitQuery(
        """
        SELECT
            DATE_FORMAT(
                CASE day_of_week(appdate)
                    WHEN 7 THEN appdate + interval '3' day /* Sun */
                    WHEN 1 THEN appdate + interval '2' day /* Mon */
                    WHEN 2 THEN appdate + interval '1' day /* Tue */
                    WHEN 3 THEN appdate /* Wed */
                    WHEN 4 THEN appdate + interval '6' day /* Thu */
                    WHEN 5 THEN appdate + interval '5' day /* Fri */
                    WHEN 6 THEN appdate + interval '4' day /* Sat */
                END, '%Y-%m-%d'
            ) AS lastDayReportingWeek,
            CAST(CASE WHEN SUM(downloads) < 5 AND sum(downloads) > 0 THEN 5 ELSE SUM(downloads) END as BIGINT) as downloads,
            CAST(CASE WHEN SUM(risky_venue) < 5 AND SUM(risky_venue) > 0 THEN 5 ELSE SUM(risky_venue) END as BIGINT) as risky_venue,
            CAST(CASE WHEN SUM(posters) < 5 AND SUM(posters) > 0 THEN 5 ELSE SUM(posters) END as BIGINT) as posters
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
        WHERE appdate >= date('2020-08-13') 
        AND appdate <=
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

    override fun startCountryDatasetQueryAsync(): QueryId = asyncDbClient.submitQuery(
        """
        SELECT
            DATE_FORMAT(lastDayReportingWeek, '%Y-%m-%d') as lastDayReportingWeek,
            country,
            country_welsh,
            CASE
            WHEN sum(checkedin) < 5 AND sum(checkedin) > 0 THEN 5
            ELSE sum(checkedin) END as checkedin,
            CASE
            WHEN sum(receivedriskycontactnotificationind) < 5 AND sum(receivedriskycontactnotificationind) > 0 THEN 5
            ELSE sum(receivedriskycontactnotificationind) END as receivedriskycontactnotificationind,
            CASE
            WHEN sum(receivednegativetestresultind) < 5 AND sum(receivednegativetestresultind) > 0 THEN 5
            ELSE sum(receivednegativetestresultind) END as receivednegativetestresultind,
            CASE
            WHEN sum(receivedpositivetestresultind) < 5 AND sum(receivedpositivetestresultind) > 0 THEN 5
            ELSE sum(receivedpositivetestresultind) END as receivedpositivetestresultind,
            CASE
            WHEN sum(totaluserscompletedquestionnaireandstartedisolationind) < 5 AND sum(totaluserscompletedquestionnaireandstartedisolationind) > 0 THEN 5
            ELSE sum(totaluserscompletedquestionnaireandstartedisolationind) END as totaluserscompletedquestionnaireandstartedisolationind
            FROM
            (SELECT
                truncatedstartdate,
                platform,
                CASE day_of_week(truncatedstartdate)
                    WHEN 7 THEN truncatedstartdate - interval '3' day /* Sun */
                    WHEN 1 THEN truncatedstartdate - interval '4' day /* Mon */
                    WHEN 2 THEN truncatedstartdate - interval '5' day /* Tue */
                    WHEN 3 THEN truncatedstartdate - interval '6' day /* Wed */
                    WHEN 4 THEN truncatedstartdate /* Thu */
                    WHEN 5 THEN truncatedstartdate - interval '1' day /* Fri */
                    WHEN 6 THEN truncatedstartdate - interval '2' day /* Sat */
                END AS firstDayReportingWeek,
                CASE day_of_week(truncatedstartdate)
                    WHEN 7 THEN truncatedstartdate + interval '3' day /* Sun */
                    WHEN 1 THEN truncatedstartdate + interval '2' day /* Mon */
                    WHEN 2 THEN truncatedstartdate + interval '1' day /* Tue */
                    WHEN 3 THEN truncatedstartdate /* Wed */
                    WHEN 4 THEN truncatedstartdate + interval '6' day /* Thu */
                    WHEN 5 THEN truncatedstartdate + interval '5' day /* Fri */
                    WHEN 6 THEN truncatedstartdate + interval '4' day /* Sat */
                END AS lastDayReportingWeek,
                CASE day_of_week(current_date)
                    WHEN 7 THEN current_date - interval '7' day /* Sun */
                    WHEN 1 THEN current_date - interval '1' day /* Mon */
                    WHEN 2 THEN current_date - interval '2' day /* Tue */
                    WHEN 3 THEN current_date - interval '3' day /* Wed */
                    WHEN 4 THEN current_date - interval '4' day /* Thu */
                    WHEN 5 THEN current_date - interval '5' day /* Fri */
                    WHEN 6 THEN current_date - interval '6' day /* Sat */
                END AS latestSubmittedDate,
                COALESCE(pdgl.local_authority, pdgl2.local_authority) AS local_authority,
                COALESCE(pdgl.region, pdgl2.region) AS region,
                COALESCE(pdgl.country, pdgl2.country) AS country,
                CASE COALESCE(pdgl.country, pdgl2.country)
                    WHEN 'England' THEN 'Lloegr'
                    WHEN 'Wales' THEN 'Cymru'
                END AS country_welsh,
                NumberofRecords,
                totaluserscompletedquestionnaireandstartedisolationind,
                usersusingqrcheckinind,
                checkedin,
                canceledcheckin,
                receivednegativetestresultind,
                receivedpositivetestresultind,
                receivedvoidtestresultind,
                receivedriskycontactnotificationind,
                receivedriskycontactnotificationusingbgtaskind
            FROM(
                SELECT
                    truncatedstartdate,
                    platform,
                    postaldistrict,
                    lad20cd,
                    COUNT(*) AS NumberofRecords,
                    SUM(totaluserscompletedquestionnaireandstartedisolationind) AS totaluserscompletedquestionnaireandstartedisolationind,
                    SUM(usersusingqrcheckinind) AS usersusingqrcheckinind,
                    SUM(checkedin) AS checkedin,
                    SUM(canceledcheckin) AS canceledcheckin,
                    SUM(receivednegativetestresultind) AS receivednegativetestresultind,
                    SUM(receivedpositivetestresultind) AS receivedpositivetestresultind,
                    SUM(receivedvoidtestresultind) AS receivedvoidtestresultind,
                    SUM(receivedriskycontactnotificationind) AS receivedriskycontactnotificationind,
                    SUM(receivedriskycontactnotificationusingbgtaskind) AS receivedriskycontactnotificationusingbgtaskind
                FROM(
                    SELECT
                        date_parse(substring(aad.startdate,1,10), '%Y-%c-%d') AS truncatedstartdate,
                        aad.postaldistrict,
                        COALESCE(aad.localauthority,'') AS lad20cd,
                        (aad.checkedin - aad.canceledcheckin) as checkedin,
                        aad.canceledcheckin,
                        CASE WHEN aad.completedquestionnaireandstartedisolation > 0 THEN 1 ELSE 0 END AS totaluserscompletedquestionnaireandstartedisolationind,
                        CASE WHEN Upper(devicemodel) LIKE '%IPHONE%' THEN 'Apple' ELSE 'Android' END AS platform,
                        CASE WHEN aad.checkedin > 0 THEN 1 ELSE 0 END AS usersusingqrcheckinind,
                        CASE WHEN aad.receivedpositivetestresult > 0 THEN 1 ELSE 0 END AS receivedpositivetestresultind,
                        CASE WHEN aad.receivednegativetestresult > 0 THEN 1 ELSE 0 END AS receivednegativetestresultind,
                        CASE WHEN aad.receivedvoidtestresult > 0 THEN 1 ELSE 0 END AS receivedvoidtestresultind,
                        CASE
                            WHEN
                                (aad.receivedriskycontactnotification IS NOT NULL
                                AND aad.receivedriskycontactnotification > 0)
                            OR
                                (aad.receivedriskycontactnotification IS NULL
                            AND aad.runningnormallybackgroundtick > 0
                            AND aad.isisolatingbackgroundtick > 0
                            AND aad.hashadriskycontactbackgroundtick > 0
                            AND aad.hashadriskycontactbackgroundtick < aad.runningnormallybackgroundtick )
                            THEN 1
                            ELSE 0
                        END AS receivedriskycontactnotificationind,
                        CASE
                            WHEN
                                (aad.receivedriskycontactnotification IS NOT NULL
                                AND aad.receivedriskycontactnotification > 0)
                            OR
                                (aad.receivedriskycontactnotification IS NULL
                            AND aad.totalbackgroundtasks > 0
                            AND aad.isisolatingbackgroundtick > 0
                            AND aad.hashadriskycontactbackgroundtick > 0
                            AND aad.hashadriskycontactbackgroundtick < aad.totalbackgroundtasks )
                            THEN 1
                            ELSE 0
                        END AS receivedriskycontactnotificationusingbgtaskind
                    FROM "${workspace}_analytics_db"."${workspace}_analytics_mobile" aad
                    WHERE date_parse(substring(aad.startdate,1,10), '%Y-%c-%d') >= date('2020-09-24')
                    AND date_parse(substring(aad.startdate,1,10), '%Y-%c-%d') <=
                        (CASE day_of_week(current_date)
                            WHEN 7 THEN current_date - interval '11' day /* Sun */
                            WHEN 1 THEN current_date - interval '5' day /* Mon */
                            WHEN 2 THEN current_date - interval '6' day /* Tue */
                            WHEN 3 THEN current_date - interval '7' day /* Wed */
                            WHEN 4 THEN current_date - interval '8' day /* Thu */
                            WHEN 5 THEN current_date - interval '9' day /* Fri */
                            WHEN 6 THEN current_date - interval '10' day /* Sat */
                        END)
                    AND date_parse(aad.submitteddatehour,'%Y/%c/%d/%H') <=
                        (CASE day_of_week(current_date)
                            WHEN 7 THEN current_date - interval '7' day /* Sun */
                            WHEN 1 THEN current_date - interval '1' day /* Mon */
                            WHEN 2 THEN current_date - interval '2' day /* Tue */
                            WHEN 3 THEN current_date - interval '3' day /* Wed */
                            WHEN 4 THEN current_date - interval '4' day /* Thu */
                            WHEN 5 THEN current_date - interval '5' day /* Fri */
                            WHEN 6 THEN current_date - interval '6' day /* Sat */
                        END)
                    AND aad.startdate <> aad.enddate
                    )
                GROUP BY
                    truncatedstartdate,
                    lad20cd,
                    postaldistrict,
                    platform
            ) aad2
            LEFT JOIN "${workspace}_analytics_db"."${workspace}_analytics_postcode_demographic_geographic_lookup" AS pdgl
                ON (aad2.lad20cd <> '' AND aad2.postaldistrict = pdgl.postcode AND aad2.lad20cd = pdgl.lad20cd AND (pdgl.country NOT IN ('Scotland', 'Northern Ireland') OR pdgl.country IS NULL))
            LEFT JOIN "${workspace}_analytics_db"."${workspace}_analytics_postcode_demographic_geographic_lookup" AS pdgl2
                ON (aad2.lad20cd = '' AND aad2.postaldistrict = pdgl2.postcode AND pdgl2.lad20cd = '' AND (pdgl2.country NOT IN ('Scotland', 'Northern Ireland') OR pdgl2.country IS NULL))
            )
            WHERE
            country  = 'England' OR country = 'Wales'
            GROUP BY
            lastDayReportingWeek,
            country,
            country_welsh
    """
    )

    override fun startLocalAuthorityDatasetQueryAsync(): QueryId = asyncDbClient.submitQuery(
        """
        SELECT
            DATE_FORMAT(lastDayReportingWeek, '%Y-%m-%d') as lastDayReportingWeek,
            local_authority,
            CASE
            WHEN sum(checkedin) < 5 AND sum(checkedin) > 0 THEN 5
            ELSE sum(checkedin) END as checkedin,
            CASE
            WHEN sum(receivedriskycontactnotificationind) < 5 AND sum(receivedriskycontactnotificationind) > 0 THEN 5
            ELSE sum(receivedriskycontactnotificationind) END as receivedriskycontactnotificationind,
            CASE
            WHEN sum(receivednegativetestresultind) < 5 AND sum(receivednegativetestresultind) > 0 THEN 5
            ELSE sum(receivednegativetestresultind) END as receivednegativetestresultind,
            CASE
            WHEN sum(receivedpositivetestresultind) < 5 AND sum(receivedpositivetestresultind) > 0 THEN 5
            ELSE sum(receivedpositivetestresultind) END as receivedpositivetestresultind,
            CASE
            WHEN sum(totaluserscompletedquestionnaireandstartedisolationind) < 5 AND sum(totaluserscompletedquestionnaireandstartedisolationind) > 0 THEN 5
            ELSE sum(totaluserscompletedquestionnaireandstartedisolationind) END as totaluserscompletedquestionnaireandstartedisolationind
            FROM
            (SELECT
                truncatedstartdate,
                platform,
                CASE day_of_week(truncatedstartdate)
                    WHEN 7 THEN truncatedstartdate - interval '3' day /* Sun */
                    WHEN 1 THEN truncatedstartdate - interval '4' day /* Mon */
                    WHEN 2 THEN truncatedstartdate - interval '5' day /* Tue */
                    WHEN 3 THEN truncatedstartdate - interval '6' day /* Wed */
                    WHEN 4 THEN truncatedstartdate /* Thu */
                    WHEN 5 THEN truncatedstartdate - interval '1' day /* Fri */
                    WHEN 6 THEN truncatedstartdate - interval '2' day /* Sat */
                END AS firstDayReportingWeek,
                CASE day_of_week(truncatedstartdate)
                    WHEN 7 THEN truncatedstartdate + interval '3' day /* Sun */
                    WHEN 1 THEN truncatedstartdate + interval '2' day /* Mon */
                    WHEN 2 THEN truncatedstartdate + interval '1' day /* Tue */
                    WHEN 3 THEN truncatedstartdate /* Wed */
                    WHEN 4 THEN truncatedstartdate + interval '6' day /* Thu */
                    WHEN 5 THEN truncatedstartdate + interval '5' day /* Fri */
                    WHEN 6 THEN truncatedstartdate + interval '4' day /* Sat */
                END AS lastDayReportingWeek,
                CASE day_of_week(current_date)
                    WHEN 7 THEN current_date - interval '7' day /* Sun */
                    WHEN 1 THEN current_date - interval '1' day /* Mon */
                    WHEN 2 THEN current_date - interval '2' day /* Tue */
                    WHEN 3 THEN current_date - interval '3' day /* Wed */
                    WHEN 4 THEN current_date - interval '4' day /* Thu */
                    WHEN 5 THEN current_date - interval '5' day /* Fri */
                    WHEN 6 THEN current_date - interval '6' day /* Sat */
                END AS latestSubmittedDate,
                COALESCE(pdgl.local_authority, pdgl2.local_authority) AS local_authority,
                COALESCE(pdgl.region, pdgl2.region) AS region,
                COALESCE(pdgl.country, pdgl2.country) AS country,
                CASE COALESCE(pdgl.country, pdgl2.country)
                    WHEN 'England' THEN 'Lloegr'
                    WHEN 'Wales' THEN 'Cymru'
                END AS country_welsh,
                NumberofRecords,
                totaluserscompletedquestionnaireandstartedisolationind,
                usersusingqrcheckinind,
                checkedin,
                canceledcheckin,
                receivednegativetestresultind,
                receivedpositivetestresultind,
                receivedvoidtestresultind,
                receivedriskycontactnotificationind,
                receivedriskycontactnotificationusingbgtaskind
            FROM(
                SELECT
                    truncatedstartdate,
                    platform,
                    postaldistrict,
                    lad20cd,
                    COUNT(*) AS NumberofRecords,
                    SUM(totaluserscompletedquestionnaireandstartedisolationind) AS totaluserscompletedquestionnaireandstartedisolationind,
                    SUM(usersusingqrcheckinind) AS usersusingqrcheckinind,
                    SUM(checkedin) AS checkedin,
                    SUM(canceledcheckin) AS canceledcheckin,
                    SUM(receivednegativetestresultind) AS receivednegativetestresultind,
                    SUM(receivedpositivetestresultind) AS receivedpositivetestresultind,
                    SUM(receivedvoidtestresultind) AS receivedvoidtestresultind,
                    SUM(receivedriskycontactnotificationind) AS receivedriskycontactnotificationind,
                    SUM(receivedriskycontactnotificationusingbgtaskind) AS receivedriskycontactnotificationusingbgtaskind
                FROM(
                    SELECT
                        date_parse(substring(aad.startdate,1,10), '%Y-%c-%d') AS truncatedstartdate,
                        aad.postaldistrict,
                        COALESCE(aad.localauthority,'') AS lad20cd,
                        (aad.checkedin - aad.canceledcheckin) as checkedin,
                        aad.canceledcheckin,
                        CASE WHEN aad.completedquestionnaireandstartedisolation > 0 THEN 1 ELSE 0 END AS totaluserscompletedquestionnaireandstartedisolationind,
                        CASE WHEN Upper(devicemodel) LIKE '%IPHONE%' THEN 'Apple' ELSE 'Android' END AS platform,
                        CASE WHEN aad.checkedin > 0 THEN 1 ELSE 0 END AS usersusingqrcheckinind,
                        CASE WHEN aad.receivedpositivetestresult > 0 THEN 1 ELSE 0 END AS receivedpositivetestresultind,
                        CASE WHEN aad.receivednegativetestresult > 0 THEN 1 ELSE 0 END AS receivednegativetestresultind,
                        CASE WHEN aad.receivedvoidtestresult > 0 THEN 1 ELSE 0 END AS receivedvoidtestresultind,
                        CASE
                            WHEN
                                (aad.receivedriskycontactnotification IS NOT NULL
                                    AND aad.receivedriskycontactnotification > 0)
                            OR
                                (aad.receivedriskycontactnotification IS NULL
                            AND aad.runningnormallybackgroundtick > 0
                            AND aad.isisolatingbackgroundtick > 0
                            AND aad.hashadriskycontactbackgroundtick > 0
                            AND aad.hashadriskycontactbackgroundtick < aad.runningnormallybackgroundtick )
                            THEN 1
                            ELSE 0
                        END AS receivedriskycontactnotificationind,
                        CASE
                            WHEN
                            (aad.receivedriskycontactnotification IS NOT NULL
                                AND aad.receivedriskycontactnotification > 0)
                            OR
                            (aad.receivedriskycontactnotification IS NULL
                                AND aad.totalbackgroundtasks > 0
                                AND aad.isisolatingbackgroundtick > 0
                                AND aad.hashadriskycontactbackgroundtick > 0
                                AND aad.hashadriskycontactbackgroundtick < aad.totalbackgroundtasks )
                            THEN 1
                            ELSE 0
                        END AS receivedriskycontactnotificationusingbgtaskind
                    FROM "${workspace}_analytics_db"."${workspace}_analytics_mobile" aad
                    WHERE date_parse(substring(aad.startdate,1,10), '%Y-%c-%d') >= date('2020-09-24')
                    AND date_parse(substring(aad.startdate,1,10), '%Y-%c-%d') <=
                        (CASE day_of_week(current_date)
                            WHEN 7 THEN current_date - interval '11' day /* Sun */
                            WHEN 1 THEN current_date - interval '5' day /* Mon */
                            WHEN 2 THEN current_date - interval '6' day /* Tue */
                            WHEN 3 THEN current_date - interval '7' day /* Wed */
                            WHEN 4 THEN current_date - interval '8' day /* Thu */
                            WHEN 5 THEN current_date - interval '9' day /* Fri */
                            WHEN 6 THEN current_date - interval '10' day /* Sat */
                        END)
                    AND date_parse(aad.submitteddatehour,'%Y/%c/%d/%H') <=
                        (CASE day_of_week(current_date)
                            WHEN 7 THEN current_date - interval '7' day /* Sun */
                            WHEN 1 THEN current_date - interval '1' day /* Mon */
                            WHEN 2 THEN current_date - interval '2' day /* Tue */
                            WHEN 3 THEN current_date - interval '3' day /* Wed */
                            WHEN 4 THEN current_date - interval '4' day /* Thu */
                            WHEN 5 THEN current_date - interval '5' day /* Fri */
                            WHEN 6 THEN current_date - interval '6' day /* Sat */
                        END)
                    AND aad.startdate <> aad.enddate
                    )
                GROUP BY
                    truncatedstartdate,
                    lad20cd,
                    postaldistrict,
                    platform
            ) aad2
            LEFT JOIN "${workspace}_analytics_db"."${workspace}_analytics_postcode_demographic_geographic_lookup" AS pdgl
                ON (aad2.lad20cd <> '' AND aad2.postaldistrict = pdgl.postcode AND aad2.lad20cd = pdgl.lad20cd AND (pdgl.country NOT IN ('Scotland', 'Northern Ireland') OR pdgl.country IS NULL))
            LEFT JOIN "${workspace}_analytics_db"."${workspace}_analytics_postcode_demographic_geographic_lookup" AS pdgl2
                ON (aad2.lad20cd = '' AND aad2.postaldistrict = pdgl2.postcode AND pdgl2.lad20cd = '' AND (pdgl2.country NOT IN ('Scotland', 'Northern Ireland') OR pdgl2.country IS NULL))
            )
            WHERE
            country  = 'England' OR country = 'Wales'
            GROUP BY
            lastDayReportingWeek,
            local_authority
        """
    )

    override fun agnosticDataset(queryId: QueryId): QueryResult<CsvS3Object> =
        when (val queryResult = asyncDbClient.queryResults(queryId)) {
            is QueryResult.Waiting -> QueryResult.Waiting()
            is QueryResult.Error -> QueryResult.Error(queryResult.message)
            is QueryResult.Finished -> QueryResult.Finished(
                AgnosticDataset(
                    queryResult.results.resultSet.rows.drop(1).map {
                        AgnosticDatasetRow(
                            weekEnding = LocalDate.parse(it.data[0].varCharValue),
                            downloads = it.data[1]?.varCharValue?.toLong() ?: 0,
                            riskyVenues = it.data[2]?.varCharValue?.toLong() ?: 0,
                            posters = it.data[3]?.varCharValue?.toLong() ?: 0
                        )
                    }
                )
            )
        }

    override fun countryDataset(queryId: QueryId): QueryResult<CsvS3Object> =
        when (val queryResult = asyncDbClient.queryResults(queryId)) {
            is QueryResult.Waiting -> QueryResult.Waiting()
            is QueryResult.Error -> QueryResult.Error(queryResult.message)
            is QueryResult.Finished -> QueryResult.Finished(
                CountryDataset(
                    queryResult.results.resultSet.rows.drop(1).map {
                        CountryDatasetRow(
                            weekEnding = LocalDate.parse(it.data[0].varCharValue),
                            countryEnglish = it.data[1].varCharValue,
                            countryWelsh = it.data[2].varCharValue,
                            checkIns = it.data[3]?.varCharValue?.toLong() ?: 0,
                            contactTracingAlerts = it.data[4]?.varCharValue?.toLong() ?: 0,
                            negativeTestResults = it.data[5]?.varCharValue?.toLong() ?: 0,
                            positiveTestResults = it.data[6]?.varCharValue?.toLong() ?: 0,
                            symptomsReported = it.data[7]?.varCharValue?.toLong() ?: 0,
                        )
                    }
                )
            )
        }

    override fun localAuthorityDataset(queryId: QueryId): QueryResult<CsvS3Object> =
        when (val queryResult = asyncDbClient.queryResults(queryId)) {
            is QueryResult.Waiting -> QueryResult.Waiting()
            is QueryResult.Error -> QueryResult.Error(queryResult.message)
            is QueryResult.Finished -> QueryResult.Finished(
                LocalAuthorityDataset(
                    queryResult.results.resultSet.rows.drop(1).map {
                        LocalAuthorityDatasetRow(
                            weekEnding = LocalDate.parse(it.data[0].varCharValue),
                            localAuthority = it.data[1].varCharValue,
                            checkIns = it.data[2]?.varCharValue?.toLong() ?: 0,
                            contactTracingAlerts = it.data[3]?.varCharValue?.toLong() ?: 0,
                            negativeTestResults = it.data[4]?.varCharValue?.toLong() ?: 0,
                            positiveTestResults = it.data[5]?.varCharValue?.toLong() ?: 0,
                            symptomsReported = it.data[6]?.varCharValue?.toLong() ?: 0,
                        )
                    }
                )
            )
        }
}
