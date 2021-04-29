package uk.nhs.nhsx.analyticsedge.persistence

import uk.nhs.nhsx.analyticsedge.QueryId
import uk.nhs.nhsx.analyticsedge.QueryResult
import uk.nhs.nhsx.analyticsedge.datasets.AnalyticsSource

class AnalyticsDao(private val workspace: String, private val asyncDbClient: AsyncDbClient) : AnalyticsSource {

    override fun startAdoptionDatasetQueryAsync(): QueryId = asyncDbClient.submitQuery(
        """
            SELECT
            DATE_FORMAT(truncatedstartdate, '%Y-%m-%d') as date,
            COALESCE(pdgl.local_authority, pdgl2.local_authority) AS localAuthority,
            platform,
            numberOfRecords
            FROM
            (SELECT
            date_parse(substring(aad.startdate,1,10), '%Y-%c-%d') as truncatedstartdate,
            COALESCE(aad.localauthority,'') AS lad20cd,
            aad.postaldistrict,
            CASE WHEN Upper(devicemodel) LIKE '%IPHONE%' THEN 'Apple' ELSE 'Android' END AS platform,
            COUNT(*) as numberOfRecords
            FROM "${workspace}_analytics_db"."${workspace}_analytics_mobile" aad
                    WHERE date_parse(substring(aad.startdate,1,10), '%Y-%c-%d') < current_date - interval '3' day
                        AND date_parse(substring(aad.startdate,1,10), '%Y-%c-%d') >= date('2020-08-13')
                        AND aad.startdate <> aad.enddate

                GROUP BY
                    COALESCE(aad.localauthority,''),
                    aad.postaldistrict,
                    date_parse(substring(aad.startdate,1,10), '%Y-%c-%d'),
                    CASE WHEN Upper(devicemodel) LIKE '%IPHONE%' THEN 'Apple' ELSE 'Android' END
            ) aad2
            LEFT JOIN "${workspace}_analytics_db"."${workspace}_analytics_postcode_demographic_geographic_lookup" AS pdgl
                ON (aad2.lad20cd <> '' AND aad2.postaldistrict = pdgl.postcode AND aad2.lad20cd = pdgl.lad20cd AND (pdgl.country NOT IN ('Scotland', 'Northern Ireland') OR pdgl.country IS NULL))
            LEFT JOIN "${workspace}_analytics_db"."${workspace}_analytics_postcode_demographic_geographic_lookup" AS pdgl2
                ON (aad2.lad20cd = '' AND aad2.postaldistrict = pdgl2.postcode AND pdgl2.lad20cd = '' AND (pdgl2.country NOT IN ('Scotland', 'Northern Ireland') OR pdgl2.country IS NULL))
        """.trimIndent()
    )

    override fun startAggregateDatasetQueryAsync(): QueryId = asyncDbClient.submitQuery(
        """
            SELECT
            DATE_FORMAT(appdate, '%Y-%m-%d') as date,
            SUM(AppleDownloads) as appleDownloads,
            SUM(AndroidDownloads) as androidDownloads,
                SUM(posters) as posters
            FROM(
                SELECT
                    appdate,
                    SUM(AppleDownloads) as AppleDownloads,
                    SUM(AndroidDownloads) as AndroidDownloads,
                    SUM(risky_venue) AS risky_venue
                FROM(
                    SELECT
            			date_parse(app.date, '%Y-%c-%d') AS appdate,
            			app.platform AS platform,
                        CASE
                            WHEN app.platform = 'Android' THEN app.downloads else 0 END as AndroidDownloads,
                        CASE
                            WHEN app.platform = 'Apple' THEN app.downloads else 0
                        END as AppleDownloads,
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
            WHERE appdate > date('2020-09-23')
            AND appdate < current_date - interval '3' day
            GROUP BY appdate
        """.trimIndent()
    )

    override fun startEnpicDatasetQueryAsync(): QueryId = asyncDbClient.submitQuery(
        """
            SELECT
            DATE_FORMAT(truncatedstartdate, '%Y-%m-%d') as date,
            localAuthority,
            (rollingriskycontactnotification/rollingpositivetestresults)/NULLIF((rollingnumberofrecords/(local_authority_population*7)),0) as sevenDayRollingENPICwRollingAdoption,
            (receivedriskycontactnotificationind_combined/receivedpositivetestresultind)/NULLIF((rollingnumberofrecords/(local_authority_population*7)),0) as combinedDailyENPICwRollingAdoption,
            (rollingreceivedriskycontactnotificationind_combined/rollingpositivetestresults)/NULLIF((rollingnumberofrecords/(local_authority_population*7)),0) as sevenDayRollingCombinedENPICwRollingAdoption
            FROM
            (
            SELECT
                truncatedstartdate,
                aggaad.local_authority as localAuthority,
                NULLIF(CAST(SUM(receivedpositivetestresultind)AS DECIMAL (12,4)),0) as receivedpositivetestresultind,
                CAST(SUM(receivedriskycontactnotification)AS DECIMAL (12,4)) as receivedriskycontactnotification,
                NULLIF(CAST(SUM(SUM(NumberofRecords)) OVER (partition by aggaad.local_authority order by truncatedstartdate ASC rows between 6 preceding and current row)AS DECIMAL (12,4)),0) as rollingnumberofrecords,
                CAST(SUM(rollingriskycontactnotification) AS DECIMAL (12,4)) as rollingriskycontactnotification,
                NULLIF(CAST(SUM(rollingpositivetestresults) AS DECIMAL (12,4)),0) as rollingpositivetestresults,
                CAST(SUM(rollingreceivedriskycontactnotificationind_combined) AS DECIMAL (12,4)) as rollingreceivedriskycontactnotificationind_combined,
                CAST(SUM(receivedriskycontactnotificationind_combined) AS DECIMAL (12,4)) as receivedriskycontactnotificationind_combined,
                NULLIF(SUM(cast(lagl.local_authority_population AS DECIMAL(18,4))),0) AS local_authority_population,
                NULLIF(CAST(SUM(NumberofRecords)AS DECIMAL (12,4)),0) as NumberofRecords,
                lagl.latitude,
                lagl.longitude,
                lagl.country,
                lagl.region
            FROM(
                SELECT
                    truncatedstartdate,
                    local_authority,
                    SUM(receivedpositivetestresultind) as receivedpositivetestresultind,
                    SUM(SUM(receivedpositivetestresultind)) OVER (partition by local_authority order by truncatedstartdate ASC rows between 6 preceding and current row) as rollingpositivetestresults,
              SUM(SUM(receivedriskycontactnotification_approximation)) OVER (partition by local_authority order by truncatedstartdate ASC rows between 6 preceding and current row) as rollingreceivedriskycontactnotification_approximation,
                    SUM(SUM(receivedriskycontactnotification)) OVER (partition by local_authority order by truncatedstartdate ASC rows between 6 preceding and current row) as rollingriskycontactnotification,
                    SUM(SUM(receivedriskycontactnotificationind_combined)) OVER (partition by local_authority order by truncatedstartdate ASC rows between 6 preceding and current row) as rollingreceivedriskycontactnotificationind_combined,
                    SUM(receivedriskycontactnotificationind_combined) as receivedriskycontactnotificationind_combined,
                    SUM(receivedriskycontactnotification) as receivedriskycontactnotification,
                    COUNT(*) AS NumberofRecords
                FROM(
                    SELECT 
                        date_parse(substring(aad.startdate,1,10), '%Y-%c-%d') AS truncatedstartdate,
                        CASE WHEN aad.receivedpositivetestresult > 0 THEN 1 ELSE 0 END as receivedpositivetestresultind,
                         CASE
                            WHEN
                                
                                aad.receivedriskycontactnotification IS NULL
                            AND aad.runningnormallybackgroundtick > 0 
                            AND aad.isisolatingbackgroundtick > 0 
                            AND aad.hashadriskycontactbackgroundtick > 0
                            AND aad.hashadriskycontactbackgroundtick < aad.runningnormallybackgroundtick 
                            THEN 1
                            ELSE 0
                        END AS receivedriskycontactnotification_approximation,
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
                        END AS receivedriskycontactnotificationind_combined,
                        aad.receivedriskycontactnotification AS receivedriskycontactnotification,
                        COALESCE(pdgl.local_authority, pdgl2.local_authority) AS local_authority
                    FROM "${workspace}_analytics_db"."${workspace}_analytics_mobile" aad
                    LEFT JOIN "${workspace}_analytics_db"."${workspace}_analytics_postcode_demographic_geographic_lookup" AS pdgl
                        ON (aad.localauthority <> '' AND aad.postaldistrict = pdgl.postcode AND aad.localauthority = pdgl.lad20cd AND (pdgl.country NOT IN ('Scotland', 'Northern Ireland') OR pdgl.country IS NULL))
                    LEFT JOIN "${workspace}_analytics_db"."${workspace}_analytics_postcode_demographic_geographic_lookup" AS pdgl2
                        ON ((aad.localauthority = '' OR aad.localauthority IS NULL) AND aad.postaldistrict = pdgl2.postcode AND pdgl2.lad20cd = '' AND (pdgl2.country NOT IN ('Scotland', 'Northern Ireland') OR pdgl2.country IS NULL))
                    WHERE date_parse(substring(aad.startdate,1,10), '%Y-%c-%d') < current_date - interval '3' day
                    AND date_parse(substring(aad.startdate,1,10), '%Y-%c-%d') >= date('2020-09-23')
                    AND aad.startdate <> aad.enddate
                    )
                GROUP BY 
                    truncatedstartdate,
                    local_authority
            ) aggaad
            LEFT JOIN "${workspace}_analytics_db"."${workspace}_analytics_local_authorities_demographic_geographic_lookup" AS lagl
            ON aggaad.local_authority = lagl.local_authority
            WHERE country NOT IN ('Scotland', 'Northern Ireland') or country is null
            GROUP BY 
            truncatedstartdate,
            aggaad.local_authority,
            lagl.country,
            lagl.region,
            lagl.longitude,
            lagl.latitude)
            WHERE localAuthority NOT IN ('Dumfries and Galloway','Scottish Borders') OR localAuthority is not null
        """.trimIndent()
    )

    override fun startIsolationDatasetQueryAsync(): QueryId = asyncDbClient.submitQuery(
        """
            SELECT
            DATE_FORMAT(date, '%Y-%m-%d') as date,
            localAuthority,
            region,
            country,
            platform,
            SUM(checkInsSum) AS checkInsSum,
            SUM(usersUsingCheckInSum) AS usersUsingCheckInCount,
            SUM(RiskyContactIsolationSum) as usersIsolatingRiskyContactCount,
            SUM(receivedriskycontactnotification) as usersReceivedExposureNotificationCount,
            SUM(usersIsolatingSelfDiagnosedSum) AS usersIsolatingSelfDiagnosedCount,
            SUM(usersIsolatingduetoPositiveTest) as usersIsolatingPositiveTestCount,
            SUM(usersReportedSymptomsViaQuestionnaireSum) AS usersReportedSymptomsViaQuestionnaireCount,
            SUM(usersReceivedPositiveTestResultSum) AS usersReceivedPositiveTestResultCount,
            SUM(usersReceivedNagativeTestResultSum) AS usersReceivedNegativeTestResultCount,
            SUM(usersReceivedVoidTestResultSum) AS usersReceivedVoidTestResultCount,
            SUM(usersUsingPauseSum) AS usersUsingPauseCount
        FROM(
            SELECT
                truncatedstartdate AS date,
                platform,
                postaldistrict,
                aad2.lad20cd,
                COALESCE(pdgl.local_authority, pdgl2.local_authority) AS localAuthority,
                COALESCE(pdgl.region, pdgl2.region) AS region,
                COALESCE(pdgl.country, pdgl2.country) AS country,
                usersReportedSymptomsViaQuestionnaireSum,
                usersUsingPauseSum,
                usersUsingCheckInSum,
                RiskyContactIsolationApprox,
                checkInsSum,
                receivedriskycontactnotification,
                usersReceivedNagativeTestResultSum,
                usersReceivedPositiveTestResultSum,
                usersReceivedVoidTestResultSum,
                usersIsolatingSelfDiagnosedSum,
                RiskyContactIsolationSum,
                usersIsolatingduetoPositiveTest
            FROM(
                SELECT
                    truncatedstartdate,
                    platform,
                    postaldistrict,
                    lad20cd,
                    SUM(totaluserscompletedquestionnaireandstartedisolationind) AS usersReportedSymptomsViaQuestionnaireSum,
                    SUM(encounterdetectionpausedbackgroundtickind) AS usersUsingPauseSum,
                    SUM(usersusingqrcheckinind) AS usersUsingCheckInSum,
                    SUM(checkedin) AS checkInsSum,
                    SUM(riskycontactisolationapproximation) as RiskyContactIsolationApprox,
                    SUM(receivednegativetestresultind) AS usersReceivedNagativeTestResultSum,
                    SUM(receivedpositivetestresultind) AS usersReceivedPositiveTestResultSum,
                    SUM(receivedvoidtestresultind) AS usersReceivedVoidTestResultSum,
                    SUM(selfdiagnosedisolationind) AS usersIsolatingSelfDiagnosedSum,
                    SUM(riskycontactisolationind) + SUM(riskycontactisolationapproximation) as RiskyContactIsolationSum,
                    SUM(NumberofUsersIsolatingduetoPositiveTest) as usersIsolatingduetoPositiveTest,
                    SUM(receivedriskycontactnotification) as receivedriskycontactnotification
                FROM(
                    SELECT 
                        date_parse(substring(aad.startdate,1,10), '%Y-%c-%d') AS truncatedstartdate,
                        CASE WHEN receivedriskycontactnotification > 0 THEN 1 ELSE 0 END as receivedriskycontactnotification,
                        aad.postaldistrict,
                        aad.isisolatingbackgroundtick,
                        aad.hasselfdiagnosedpositivebackgroundtick,
                        aad.hashadriskycontactbackgroundtick,
                        COALESCE(aad.localauthority,'') AS lad20cd,
                        (aad.checkedin - aad.canceledcheckin) as checkedin,
                        CASE WHEN Upper(devicemodel) LIKE '%IPHONE%' THEN 'Apple' ELSE 'Android' END AS platform,
                        CASE WHEN aad.receivedpositivetestresult > 0 THEN 1 ELSE 0 END AS receivedpositivetestresultind,
                        CASE WHEN aad.receivednegativetestresult > 0 THEN 1 ELSE 0 END AS receivednegativetestresultind,
                        CASE WHEN aad.receivedvoidtestresult > 0 THEN 1 ELSE 0 END AS receivedvoidtestresultind,
                        CASE WHEN aad.encounterdetectionpausedbackgroundtick > 0 THEN 1 ELSE 0 END AS encounterdetectionpausedbackgroundtickind,
                        CASE WHEN aad.completedquestionnaireandstartedisolation > 0 THEN 1 ELSE 0 END AS totaluserscompletedquestionnaireandstartedisolationind,
                        CASE
                        WHEN aad.isisolatingbackgroundtick > 0
                            AND aad.isisolatingfortestedpositivebackgroundtick > 0
                            AND aad.isisolatingforselfdiagnosedbackgroundtick = 0
                            AND aad.isisolatingforhadriskycontactbackgroundtick = 0
                        THEN 1
                        ELSE 0
                    END AS numberofusersisolatingduetopositivetest,
                    aad.isisolatingforhadriskycontactbackgroundtick,
                        CASE WHEN aad.checkedin > 0 THEN 1 ELSE 0 END AS usersusingqrcheckinind,
                        CASE 
                            WHEN isisolatingforhadriskycontactbackgroundtick IS NOT NULL
                                AND aad.isisolatingbackgroundtick > 0
                                AND aad.isisolatingforhadriskycontactbackgroundtick > 0
                                AND aad.isisolatingforselfdiagnosedbackgroundtick = 0
                                AND aad.isisolatingfortestedpositivebackgroundtick = 0
                            THEN 1 
                            ELSE 0 
                        END AS riskycontactisolationind,
                        CASE 
                            WHEN 
                                isisolatingforhadriskycontactbackgroundtick IS NULL
                                AND isisolatingbackgroundtick > 0
                                AND hashadriskycontactbackgroundtick > 0
                                AND hasselfdiagnosedpositivebackgroundtick = 0
                            THEN 1 
                            ELSE 0
                        END AS riskycontactisolationapproximation,
                        CASE 
                            WHEN aad.isisolatingbackgroundtick > 0 
                                AND aad.isisolatingforselfdiagnosedbackgroundtick > 0 
                                AND aad.isisolatingfortestedpositivebackgroundtick = 0
                                AND aad.isisolatingforhadriskycontactbackgroundtick = 0
                            THEN 1 
                            ELSE 0 
                        END AS selfdiagnosedisolationind
                    FROM "${workspace}_analytics_db"."${workspace}_analytics_mobile" aad
                    WHERE date_parse(substring(aad.startdate,1,10), '%Y-%c-%d') < current_date - interval '3' day
                        AND date_parse(substring(aad.startdate,1,10), '%Y-%c-%d') >= date('2020-09-23')
                        AND aad.startdate <> aad.enddate
                    )
                GROUP BY 
                    truncatedstartdate,
                    platform,
                    postaldistrict,
                    lad20cd
            ) aad2
            LEFT JOIN "${workspace}_analytics_db"."${workspace}_analytics_postcode_demographic_geographic_lookup" AS pdgl
                ON (aad2.lad20cd <> '' AND aad2.postaldistrict = pdgl.postcode AND aad2.lad20cd = pdgl.lad20cd AND (pdgl.country NOT IN ('Scotland', 'Northern Ireland') OR pdgl.country IS NULL))
            LEFT JOIN "${workspace}_analytics_db"."${workspace}_analytics_postcode_demographic_geographic_lookup" AS pdgl2
                ON (aad2.lad20cd = '' AND aad2.postaldistrict = pdgl2.postcode AND pdgl2.lad20cd = '' AND (pdgl2.country NOT IN ('Scotland', 'Northern Ireland') OR pdgl2.country IS NULL))
        )
        GROUP BY
            date,
            localAuthority,
            region,
            country,
            platform
        """.trimIndent()
    )

    override fun startPosterDatasetQueryAsync(): QueryId = asyncDbClient.submitQuery(
        """
            SELECT
            substring(posters.created,1,10) AS "date",
            posters.locationname AS locationName,
            posters.addressline1 AS addressLine1,
            posters.addressline2 AS addressLine2,
            posters.addressline3 AS addressLine3,
            posters.townorcity AS townOrCity,
            lookup.postcode AS postcode,
            lookup.localauthorityname AS localAuthority,
            posters.county AS county,
            lookup.regionname AS region,
            lookup.countryname AS country,
            CASE posters.venuetypeid
                WHEN '000' THEN 'Other'
                WHEN '001' THEN 'Accommodation'
                WHEN '002' THEN 'Medical Facility'
                WHEN '003' THEN 'Non-residential institution'
                WHEN '004' THEN 'Personal care'
                WHEN '005' THEN 'Place of worship'
                WHEN '006' THEN 'Private event'
                WHEN '007' THEN 'Recreation and leisure'
                WHEN '008' THEN 'Restaurants, cafes, pubs and bars'
                WHEN '009' THEN 'Residential care'
                WHEN '010' THEN 'Retail shops'
                WHEN '011' THEN 'Sports and fitness facilities'
                WHEN '012' THEN 'Finance and professional service'
                WHEN '013' THEN 'Education'
                WHEN '014' THEN 'Events and conference space'
                WHEN '015' THEN 'Office location and workspace'
                WHEN '016' THEN 'Childcare'
                WHEN '017' THEN 'Transport'
                WHEN '018' THEN 'Rental/hire locations'
                WHEN '999' THEN 'Unspecified'
                ELSE 'Unspecified'
            END AS venueTypeName
        FROM "${workspace}_analytics_db"."${workspace}_analytics_qr_posters" posters
        LEFT JOIN "${workspace}_analytics_db"."${workspace}_analytics_full_postcode_lookup" lookup ON posters.postcode = lookup.postcodenospace
        """.trimIndent()
    )

    override fun checkQueryState(queryId: QueryId): QueryResult<Unit> = asyncDbClient.queryResults(queryId)
}
