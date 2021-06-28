package uk.nhs.nhsx.pubdash.persistence

import uk.nhs.nhsx.pubdash.QueryId
import uk.nhs.nhsx.pubdash.QueryResult
import uk.nhs.nhsx.pubdash.datasets.AnalyticsSource

class AnalyticsDao(
    private val workspace: String, private val asyncDbClient: AsyncDbClient, private val mobileAnalyticsTable: String,
) : AnalyticsSource {

    override fun startAgnosticDatasetQueryAsync(): QueryId = asyncDbClient.submitQuery(
        """
        SELECT
            DATE_FORMAT(firstDayReportingWeek, '%Y-%m-%d') AS "Week starting (Wythnos yn dechrau)",
            DATE_FORMAT(lastDayReportingWeek, '%Y-%m-%d') AS "Week ending (Wythnos yn gorffen)",
            SUM(downloads) AS "Number of app downloads (Nifer o lawrlwythiadau ap)",
            SUM(riskyVenues) AS "Number of venues the app has sent alerts about (Nifer o leoliadau mae’r ap wedi anfon hysbysiadau amdanynt)",
            SUM(posters) AS "Number of NHS QR posters created (Nifer o bosteri cod QR y GIG a grëwyd)",
            SUM(SUM(downloads)) OVER (ORDER BY lastDayReportingWeek) AS "Cumulative number of app downloads (Nifer o lawrlwythiadau ap cronnus)",
            SUM(SUM(riskyVenues)) OVER (ORDER BY lastDayReportingWeek) AS "Cumulative number of 'at risk' venues triggering venue alerts (Nifer o leoliadau 'dan risg' cronnus)",
            SUM(SUM(posters)) OVER (ORDER BY lastDayReportingWeek) AS "Cumulative number of NHS QR posters created (Nifer o bosteri cod QR y GIG a grëwyd cronnus)"
        FROM(
            SELECT
                /* This calculates the date of the first day of the reporting week
                the row belongs to. Reporting weeks is from Thurs-Wed (inclusive),
                therefore this finds the date of the preceding Thursday (or leave as Thurs). */
                CASE day_of_week("date")
                    WHEN 7 THEN "date" - interval '3' day /* Sun */
                    WHEN 1 THEN "date" - interval '4' day /* Mon */
                    WHEN 2 THEN "date" - interval '5' day /* Tue */
                    WHEN 3 THEN "date" - interval '6' day /* Wed */
                    WHEN 4 THEN "date" /* Thu */
                    WHEN 5 THEN "date" - interval '1' day /* Fri */
                    WHEN 6 THEN "date" - interval '2' day /* Sat */
                END AS firstDayReportingWeek,
                /* This calculates the date of the last day of the reporting week
                the row belongs to. Reporting weeks is from Thurs-Wed (inclusive),
                therefore this finds the date of the next Wednesday  (or leave as Wed). */
                CASE day_of_week("date")
                    WHEN 7 THEN "date" + interval '3' day /* Sun */
                    WHEN 1 THEN "date" + interval '2' day /* Mon */
                    WHEN 2 THEN "date" + interval '1' day /* Tue */
                    WHEN 3 THEN "date" /* Wed */
                    WHEN 4 THEN "date" + interval '6' day /* Thu */
                    WHEN 5 THEN "date" + interval '5' day /* Fri */
                    WHEN 6 THEN "date" + interval '4' day /* Sat */
                END AS lastDayReportingWeek,
                CAST(SUM(downloads) AS BIGINT) AS downloads,
                CAST(SUM(riskyVenues) AS BIGINT) AS riskyVenues,
                CAST(SUM(posters) AS BIGINT) AS posters
            FROM(
                SELECT
                    "date", /* Escape keywords in queries with "" */
                    SUM(downloads) AS downloads,
                    SUM(riskyVenues) AS riskyVenues
                FROM(
                    SELECT
                        date_parse(downloadsTable.date, '%Y-%c-%d') AS "date",
                        downloadsTable.platform AS platform,
                        CASE
                            WHEN downloadsTable.platform = 'Android' OR  downloadsTable.platform = 'Apple' THEN downloadsTable.downloads
                        END AS downloads,
                        CASE
                            WHEN downloadsTable.platform = 'Website' THEN downloadsTable.opt_in_proportion END AS riskyVenues
                    FROM "${workspace}_analytics_db"."${workspace}_analytics_app_store" AS downloadsTable)
                GROUP BY "date"
                )
                FULL OUTER JOIN(
                    SELECT
                        posterDate,
                        count(*) AS posters
                    FROM(
                        SELECT
                            date_parse(substring(posterTable.created,1,10), '%Y-%c-%d') AS posterDate
                        FROM "${workspace}_analytics_db"."${workspace}_analytics_qr_posters" AS posterTable)
                    GROUP BY posterDate
                )
                ON "date" = posterDate
            WHERE "date" >= date('2020-08-13')
            /* This filter ensures that the dataset only contains data up to the 
            last day of the newest reporting period, which is the Wednesday of the 
            preceding week */
            AND "date" <=
                (CASE day_of_week(current_date)
                    WHEN 7 THEN current_date - interval '11' day /* Sun --> Wed */
                    WHEN 1 THEN current_date - interval '5' day /* Mon */
                    WHEN 2 THEN current_date - interval '6' day /* Tue */
                    WHEN 3 THEN current_date - interval '7' day /* Wed */
                    WHEN 4 THEN current_date - interval '8' day /* Thu */
                    WHEN 5 THEN current_date - interval '9' day /* Fri */
                    WHEN 6 THEN current_date - interval '10' day /* Sat */
                END)
            GROUP BY
                /* Expression for firstDayReportingWeek */
                CASE day_of_week("date")
                    WHEN 7 THEN "date" - interval '3' day /* Sun */
                    WHEN 1 THEN "date" - interval '4' day /* Mon */
                    WHEN 2 THEN "date" - interval '5' day /* Tue */
                    WHEN 3 THEN "date" - interval '6' day /* Wed */
                    WHEN 4 THEN "date" /* Thu */
                    WHEN 5 THEN "date" - interval '1' day /* Fri */
                    WHEN 6 THEN "date" - interval '2' day /* Sat */
                END,
                /* Expression for lastDayReportingWeek */
                CASE day_of_week(date)
                        WHEN 7 THEN "date" + interval '3' day /* Sun */
                        WHEN 1 THEN "date" + interval '2' day /* Mon */
                        WHEN 2 THEN "date" + interval '1' day /* Tue */
                        WHEN 3 THEN "date" /* Wed */
                        WHEN 4 THEN "date" + interval '6' day /* Thu */
                        WHEN 5 THEN "date" + interval '5' day /* Fri */
                        WHEN 6 THEN "date" + interval '4' day /* Sat */
                END
        )
        GROUP BY
            firstDayReportingWeek,
            lastDayReportingWeek
        ORDER BY
            lastDayReportingWeek
    """
    )

    override fun startCountryDatasetQueryAsync(): QueryId = asyncDbClient.submitQuery(
        """
        SELECT
            /* Dimensions */
            DATE_FORMAT(firstDayReportingWeek, '%Y-%m-%d') AS "Week starting (Wythnos yn dechrau)",
            DATE_FORMAT(lastDayReportingWeek, '%Y-%m-%d') AS "Week ending (Wythnos yn gorffen)",
            country AS "Country (Wlad)",
            
            /* Measures */
            SUM(totalCheckIns) AS "Check-ins (Cofrestriadau)",
            SUM(countUsersCompletedQuestionnaireAndStartedIsolation) AS "Symptoms reported (Symptomau a adroddwyd)",
            SUM(countUsersReceivedPositiveTestResult) AS "Positive test results linked to app (Canlyniadau prawf positif)",
            SUM(countUsersReceivedNegativeTestResult) AS "Negative test results linked to app (Canlyniadau prawf negatif)",
            SUM(countUsersReceivedRiskyContactNotification) AS "Contact tracing alert (Hysbysiadau olrhain cyswllt)",
            
            /* Cumulative Measures */
            SUM(SUM(totalCheckIns)) OVER (PARTITION BY country ORDER BY lastDayReportingWeek) AS "Cumulative check-ins (Cofrestriadau cronnus)",
            SUM(SUM(countUsersCompletedQuestionnaireAndStartedIsolation)) OVER (PARTITION BY country ORDER BY lastDayReportingWeek) AS "Cumulative symptoms reported (Symptomau a adroddwyd cronnus)",
            SUM(SUM(countUsersReceivedPositiveTestResult)) OVER (PARTITION BY country ORDER BY lastDayReportingWeek) AS "Cumulative positive test results linked to app (Canlyniadau prawf positif cronnus)",
            SUM(SUM(countUsersReceivedNegativeTestResult)) OVER (PARTITION BY country ORDER BY lastDayReportingWeek) AS "Cumulative negative test results linked to app (Canlyniadau prawf negatif cronnus)",
            SUM(SUM(countUsersReceivedRiskyContactNotification)) OVER (PARTITION BY country ORDER BY lastDayReportingWeek) AS "Cumulative contact tracing alert (Hysbysiadau olrhain cyswllt cronnus)"
            
        FROM(
            SELECT
                CASE day_of_week(truncatedStartDate)
                    WHEN 7 THEN truncatedStartDate - interval '3' day /* Sun */
                    WHEN 1 THEN truncatedStartDate - interval '4' day /* Mon */
                    WHEN 2 THEN truncatedStartDate - interval '5' day /* Tue */
                    WHEN 3 THEN truncatedStartDate - interval '6' day /* Wed */
                    WHEN 4 THEN truncatedStartDate /* Thu */
                    WHEN 5 THEN truncatedStartDate - interval '1' day /* Fri */
                    WHEN 6 THEN truncatedStartDate - interval '2' day /* Sat */
                END AS firstDayReportingWeek,
                CASE day_of_week(truncatedStartDate)
                    WHEN 7 THEN truncatedStartDate + interval '3' day /* Sun */
                    WHEN 1 THEN truncatedStartDate + interval '2' day /* Mon */
                    WHEN 2 THEN truncatedStartDate + interval '1' day /* Tue */
                    WHEN 3 THEN truncatedStartDate /* Wed */
                    WHEN 4 THEN truncatedStartDate + interval '6' day /* Thu */
                    WHEN 5 THEN truncatedStartDate + interval '5' day /* Fri */
                    WHEN 6 THEN truncatedStartDate + interval '4' day /* Sat */
                END AS lastDayReportingWeek,
                
                COALESCE(pdgl.local_authority, pdgl2.local_authority) AS localAuthority,
                
                CASE COALESCE(pdgl.country, pdgl2.country)
                    WHEN 'England' THEN 'England / Lloegr'
                    WHEN 'Wales' THEN 'Wales / Cymru'
                END AS country,
                
                SUM(totalCheckIns)  AS totalCheckIns,
                SUM(countUsersReceivedRiskyContactNotification) AS countUsersReceivedRiskyContactNotification,
                SUM(countUsersReceivedPositiveTestResult) AS countUsersReceivedPositiveTestResult,
                SUM(countUsersReceivedNegativeTestResult) AS countUsersReceivedNegativeTestResult,
                SUM(countUsersCompletedQuestionnaireAndStartedIsolation) AS countUsersCompletedQuestionnaireAndStartedIsolation
        
            FROM(
                SELECT
                    truncatedStartDate,
                    postalDistrict,
                    lad20cd,
                    SUM(checkedin) AS totalCheckIns,
                    SUM(completedQuestionnaireAndStartedIsolationInd) AS countUsersCompletedQuestionnaireAndStartedIsolation,
                    SUM(receivedPositiveTestResultInd) AS countUsersReceivedPositiveTestResult,
                    SUM(receivedNegativeTestResultInd) AS countUsersReceivedNegativeTestResult,
                    SUM(receivedRiskyContactNotificationInd) AS countUsersReceivedRiskyContactNotification
                FROM(
                    SELECT
                        /* Dimensions */
                        aad.postaldistrict AS postalDistrict,
                        COALESCE(aad.localauthority,'') AS lad20cd,
                        date_parse(substring(aad.startdate,1,10), '%Y-%c-%d') AS truncatedStartDate,
                        
                        /* Measures*/
                        (aad.checkedin - aad.canceledcheckin) AS checkedin,
                        CASE WHEN aad.completedquestionnaireandstartedisolation > 0 THEN 1 ELSE 0 END AS completedQuestionnaireAndStartedIsolationInd,
                        CASE WHEN aad.receivedpositivetestresult > 0 THEN 1 ELSE 0 END AS receivedPositiveTestResultInd,
                        CASE WHEN aad.receivednegativetestresult > 0 THEN 1 ELSE 0 END AS receivedNegativeTestResultInd,
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
                        END AS receivedRiskyContactNotificationInd
                    FROM "${workspace}_analytics_db"."${workspace}_${mobileAnalyticsTable}" aad
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
                        AND coalesce(
                            try(date_parse(aad.submitteddatehour,'%Y/%c/%d/%H')),
                            try(date_parse(aad.submitteddatehour,'%Y-%c-%d-%H')))<=
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
                    postalDistrict,
                    lad20cd,
                    truncatedStartDate
            ) aad2
                LEFT JOIN "${workspace}_analytics_db"."${workspace}_analytics_postcode_demographic_geographic_lookup" AS pdgl
                    ON (aad2.lad20cd <> '' AND aad2.postaldistrict = pdgl.postcode AND aad2.lad20cd = pdgl.lad20cd AND (pdgl.country NOT IN ('Scotland', 'Northern Ireland') OR pdgl.country IS NULL))
                LEFT JOIN "${workspace}_analytics_db"."${workspace}_analytics_postcode_demographic_geographic_lookup" AS pdgl2
                    ON (aad2.lad20cd = '' AND aad2.postaldistrict = pdgl2.postcode AND pdgl2.lad20cd = '' AND (pdgl2.country NOT IN ('Scotland', 'Northern Ireland') OR pdgl2.country IS NULL))
            GROUP BY
                CASE day_of_week(truncatedStartDate)
                    WHEN 7 THEN truncatedStartDate - interval '3' day /* Sun */
                    WHEN 1 THEN truncatedStartDate - interval '4' day /* Mon */
                    WHEN 2 THEN truncatedStartDate - interval '5' day /* Tue */
                    WHEN 3 THEN truncatedStartDate - interval '6' day /* Wed */
                    WHEN 4 THEN truncatedStartDate /* Thu */
                    WHEN 5 THEN truncatedStartDate - interval '1' day /* Fri */
                    WHEN 6 THEN truncatedStartDate - interval '2' day /* Sat */
                END,
                CASE day_of_week(truncatedStartDate)
                    WHEN 7 THEN truncatedStartDate + interval '3' day /* Sun */
                    WHEN 1 THEN truncatedStartDate + interval '2' day /* Mon */
                    WHEN 2 THEN truncatedStartDate + interval '1' day /* Tue */
                    WHEN 3 THEN truncatedStartDate /* Wed */
                    WHEN 4 THEN truncatedStartDate + interval '6' day /* Thu */
                    WHEN 5 THEN truncatedStartDate + interval '5' day /* Fri */
                    WHEN 6 THEN truncatedStartDate + interval '4' day /* Sat */
                END,
                
                COALESCE(pdgl.local_authority, pdgl2.local_authority),
                
                CASE COALESCE(pdgl.country, pdgl2.country)
                    WHEN 'England' THEN 'England / Lloegr'
                    WHEN 'Wales' THEN 'Wales / Cymru'
                END
        )
        WHERE localAuthority NOT IN ('Dumfries and Galloway','Scottish Borders') 
            AND localAuthority IS NOT NULL
            AND country NOT IN ('Scotland', 'Northern Ireland') 
            AND country IS NOT NULL
        GROUP BY
            firstDayReportingWeek,
            lastDayReportingWeek,
            country
        ORDER BY
            lastDayReportingWeek,
            country
    """
    )

    override fun startLocalAuthorityDatasetQueryAsync(): QueryId = asyncDbClient.submitQuery(
        """
        SELECT
            /* Dimensions */
            DATE_FORMAT(firstDayReportingWeek, '%Y-%m-%d') AS "Week starting (Wythnos yn dechrau)",
            DATE_FORMAT(lastDayReportingWeek, '%Y-%m-%d') AS "Week ending (Wythnos yn gorffen)",
            localAuthority AS "Local authority (Awdurdod lleol)",
            country AS "Country (Wlad)",
            
            /* Measures */
            SUM(totalCheckIns) AS "Check-ins (Cofrestriadau)",
            SUM(countUsersCompletedQuestionnaireAndStartedIsolation) AS "Symptoms reported (Symptomau a adroddwyd)",
            SUM(countUsersReceivedPositiveTestResult) AS "Positive test results linked to app (Canlyniadau prawf positif)",
            SUM(countUsersReceivedNegativeTestResult) AS "Negative test results linked to app (Canlyniadau prawf negatif)",
            SUM(countUsersReceivedRiskyContactNotification) AS "Contact tracing alert (Hysbysiadau olrhain cyswllt)",
            
            /* Cumulative Measures */
            SUM(SUM(totalCheckIns)) OVER (PARTITION BY localAuthority ORDER BY lastDayReportingWeek) AS "Cumulative check-ins (Cofrestriadau cronnus)",
            SUM(SUM(countUsersCompletedQuestionnaireAndStartedIsolation)) OVER (PARTITION BY localAuthority ORDER BY lastDayReportingWeek) AS "Cumulative symptoms reported (Symptomau a adroddwyd cronnus)",
            SUM(SUM(countUsersReceivedPositiveTestResult)) OVER (PARTITION BY localAuthority ORDER BY lastDayReportingWeek) AS "Cumulative positive test results linked to app (Canlyniadau prawf positif cronnus)",
            SUM(SUM(countUsersReceivedNegativeTestResult)) OVER (PARTITION BY localAuthority ORDER BY lastDayReportingWeek) AS "Cumulative negative test results linked to app (Canlyniadau prawf negatif cronnus)",
            SUM(SUM(countUsersReceivedRiskyContactNotification)) OVER (PARTITION BY localAuthority ORDER BY lastDayReportingWeek) AS "Cumulative contact tracing alert (Hysbysiadau olrhain cyswllt cronnus)"
            
        FROM(
            SELECT
                CASE day_of_week(truncatedStartDate)
                    WHEN 7 THEN truncatedStartDate - interval '3' day /* Sun */
                    WHEN 1 THEN truncatedStartDate - interval '4' day /* Mon */
                    WHEN 2 THEN truncatedStartDate - interval '5' day /* Tue */
                    WHEN 3 THEN truncatedStartDate - interval '6' day /* Wed */
                    WHEN 4 THEN truncatedStartDate /* Thu */
                    WHEN 5 THEN truncatedStartDate - interval '1' day /* Fri */
                    WHEN 6 THEN truncatedStartDate - interval '2' day /* Sat */
                END AS firstDayReportingWeek,
                CASE day_of_week(truncatedStartDate)
                    WHEN 7 THEN truncatedStartDate + interval '3' day /* Sun */
                    WHEN 1 THEN truncatedStartDate + interval '2' day /* Mon */
                    WHEN 2 THEN truncatedStartDate + interval '1' day /* Tue */
                    WHEN 3 THEN truncatedStartDate /* Wed */
                    WHEN 4 THEN truncatedStartDate + interval '6' day /* Thu */
                    WHEN 5 THEN truncatedStartDate + interval '5' day /* Fri */
                    WHEN 6 THEN truncatedStartDate + interval '4' day /* Sat */
                END AS lastDayReportingWeek,
                
                CASE COALESCE(pdgl.local_authority, pdgl2.local_authority)
                    WHEN 'Isle of Anglesey' THEN 'Isle of Anglesey / Ynys Môn'
                    WHEN 'Denbighshire' THEN 'Denbighshire / Sir Ddinbych'
                    WHEN 'Flintshire' THEN 'Flintshire / Sir y Fflint'
                    WHEN 'Wrexham' THEN 'Wrexham / Wrecsam'
                    WHEN 'Pembrokeshire' THEN 'Pembrokeshire / Sir Benfro'
                    WHEN 'Carmarthenshire' THEN 'Carmarthenshire / Sir Gaerfyrddin'
                    WHEN 'Swansea' THEN 'Swansea / Abertawe'
                    WHEN 'Neath Port Talbot' THEN 'Neath Port Talbot / Castell-nedd Port Talbot'
                    WHEN 'Bridgend' THEN 'Bridgend / Pen-y-bont ar Ogwr'
                    WHEN 'Vale of Glamorgan' THEN 'Vale of Glamorgan / Bro Morgannwg'
                    WHEN 'Cardiff' THEN 'Cardiff / Caerdydd'
                    WHEN 'Caerphilly' THEN 'Caerphilly / Caerffili'
                    WHEN 'Monmouthshire' THEN 'Monmouthshire / Sir Fynwy'
                    WHEN 'Newport' THEN 'Newport / Casnewydd'
                    WHEN 'Merthyr Tydfil' THEN 'Merthyr Tydfil / Merthyr Tudful'
                ELSE COALESCE(pdgl.local_authority, pdgl2.local_authority)
                END AS localAuthority,
                
                CASE COALESCE(pdgl.country, pdgl2.country)
                    WHEN 'England' THEN 'England / Lloegr'
                    WHEN 'Wales' THEN 'Wales / Cymru'
                END AS country,
                
                /* Small number suppression on measures (done at this level to apply to cumulative measures) */
                CASE WHEN
                    SUM(totalCheckIns) < 5 AND SUM(totalCheckIns) > 0 THEN 5
                    ELSE SUM(totalCheckIns) 
                END AS totalCheckIns,
                CASE WHEN
                    SUM(countUsersReceivedRiskyContactNotification) < 5 AND SUM(countUsersReceivedRiskyContactNotification) > 0 THEN 5
                    ELSE SUM(countUsersReceivedRiskyContactNotification) 
                END AS countUsersReceivedRiskyContactNotification,
                CASE WHEN
                    SUM(countUsersReceivedPositiveTestResult) < 5 AND SUM(countUsersReceivedPositiveTestResult) > 0 THEN 5
                    ELSE SUM(countUsersReceivedPositiveTestResult) 
                END AS countUsersReceivedPositiveTestResult,
                CASE WHEN
                    SUM(countUsersReceivedNegativeTestResult) < 5 AND SUM(countUsersReceivedNegativeTestResult) > 0 THEN 5
                    ELSE SUM(countUsersReceivedNegativeTestResult) 
                END AS countUsersReceivedNegativeTestResult,
                CASE WHEN
                    SUM(countUsersCompletedQuestionnaireAndStartedIsolation) < 5 AND SUM(countUsersCompletedQuestionnaireAndStartedIsolation) > 0 THEN 5
                    ELSE SUM(countUsersCompletedQuestionnaireAndStartedIsolation) 
                END AS countUsersCompletedQuestionnaireAndStartedIsolation
        
            FROM(
                SELECT
                    truncatedStartDate,
                    postalDistrict,
                    lad20cd,
                    SUM(checkedin) AS totalCheckIns,
                    SUM(completedQuestionnaireAndStartedIsolationInd) AS countUsersCompletedQuestionnaireAndStartedIsolation,
                    SUM(receivedPositiveTestResultInd) AS countUsersReceivedPositiveTestResult,
                    SUM(receivedNegativeTestResultInd) AS countUsersReceivedNegativeTestResult,
                    SUM(receivedRiskyContactNotificationInd) AS countUsersReceivedRiskyContactNotification
                FROM(
                    SELECT
                        /* Dimensions */
                        aad.postaldistrict AS postalDistrict,
                        COALESCE(aad.localauthority,'') AS lad20cd,
                        date_parse(substring(aad.startdate,1,10), '%Y-%c-%d') AS truncatedStartDate,
                        
                        /* Measures*/
                        (aad.checkedin - aad.canceledcheckin) AS checkedin,
                        CASE WHEN aad.completedquestionnaireandstartedisolation > 0 THEN 1 ELSE 0 END AS completedQuestionnaireAndStartedIsolationInd,
                        CASE WHEN aad.receivedpositivetestresult > 0 THEN 1 ELSE 0 END AS receivedPositiveTestResultInd,
                        CASE WHEN aad.receivednegativetestresult > 0 THEN 1 ELSE 0 END AS receivedNegativeTestResultInd,
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
                        END AS receivedRiskyContactNotificationInd
                    FROM "${workspace}_analytics_db"."${workspace}_${mobileAnalyticsTable}" aad
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
                        AND coalesce(
                            try(date_parse(aad.submitteddatehour,'%Y/%c/%d/%H')),
                            try(date_parse(aad.submitteddatehour,'%Y-%c-%d-%H'))) <=
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
                    postalDistrict,
                    lad20cd,
                    truncatedStartDate
            ) aad2
                LEFT JOIN "${workspace}_analytics_db"."${workspace}_analytics_postcode_demographic_geographic_lookup" AS pdgl
                    ON (aad2.lad20cd <> '' AND aad2.postaldistrict = pdgl.postcode AND aad2.lad20cd = pdgl.lad20cd AND (pdgl.country NOT IN ('Scotland', 'Northern Ireland') OR pdgl.country IS NULL))
                LEFT JOIN "${workspace}_analytics_db"."${workspace}_analytics_postcode_demographic_geographic_lookup" AS pdgl2
                    ON (aad2.lad20cd = '' AND aad2.postaldistrict = pdgl2.postcode AND pdgl2.lad20cd = '' AND (pdgl2.country NOT IN ('Scotland', 'Northern Ireland') OR pdgl2.country IS NULL))
            GROUP BY
                CASE day_of_week(truncatedStartDate)
                    WHEN 7 THEN truncatedStartDate - interval '3' day /* Sun */
                    WHEN 1 THEN truncatedStartDate - interval '4' day /* Mon */
                    WHEN 2 THEN truncatedStartDate - interval '5' day /* Tue */
                    WHEN 3 THEN truncatedStartDate - interval '6' day /* Wed */
                    WHEN 4 THEN truncatedStartDate /* Thu */
                    WHEN 5 THEN truncatedStartDate - interval '1' day /* Fri */
                    WHEN 6 THEN truncatedStartDate - interval '2' day /* Sat */
                END,
                CASE day_of_week(truncatedStartDate)
                    WHEN 7 THEN truncatedStartDate + interval '3' day /* Sun */
                    WHEN 1 THEN truncatedStartDate + interval '2' day /* Mon */
                    WHEN 2 THEN truncatedStartDate + interval '1' day /* Tue */
                    WHEN 3 THEN truncatedStartDate /* Wed */
                    WHEN 4 THEN truncatedStartDate + interval '6' day /* Thu */
                    WHEN 5 THEN truncatedStartDate + interval '5' day /* Fri */
                    WHEN 6 THEN truncatedStartDate + interval '4' day /* Sat */
                END,
                
                CASE COALESCE(pdgl.local_authority, pdgl2.local_authority)
                    WHEN 'Isle of Anglesey' THEN 'Isle of Anglesey / Ynys Môn'
                    WHEN 'Denbighshire' THEN 'Denbighshire / Sir Ddinbych'
                    WHEN 'Flintshire' THEN 'Flintshire / Sir y Fflint'
                    WHEN 'Wrexham' THEN 'Wrexham / Wrecsam'
                    WHEN 'Pembrokeshire' THEN 'Pembrokeshire / Sir Benfro'
                    WHEN 'Carmarthenshire' THEN 'Carmarthenshire / Sir Gaerfyrddin'
                    WHEN 'Swansea' THEN 'Swansea / Abertawe'
                    WHEN 'Neath Port Talbot' THEN 'Neath Port Talbot / Castell-nedd Port Talbot'
                    WHEN 'Bridgend' THEN 'Bridgend / Pen-y-bont ar Ogwr'
                    WHEN 'Vale of Glamorgan' THEN 'Vale of Glamorgan / Bro Morgannwg'
                    WHEN 'Cardiff' THEN 'Cardiff / Caerdydd'
                    WHEN 'Caerphilly' THEN 'Caerphilly / Caerffili'
                    WHEN 'Monmouthshire' THEN 'Monmouthshire / Sir Fynwy'
                    WHEN 'Newport' THEN 'Newport / Casnewydd'
                    WHEN 'Merthyr Tydfil' THEN 'Merthyr Tydfil / Merthyr Tudful'
                ELSE COALESCE(pdgl.local_authority, pdgl2.local_authority)
                END,
                
                CASE COALESCE(pdgl.country, pdgl2.country)
                    WHEN 'England' THEN 'England / Lloegr'
                    WHEN 'Wales' THEN 'Wales / Cymru'
                END
        )
        WHERE localAuthority NOT IN ('Dumfries and Galloway','Scottish Borders') 
            AND localAuthority IS NOT NULL
            AND country NOT IN ('Scotland', 'Northern Ireland') 
            AND country IS NOT NULL
        GROUP BY
            localAuthority,
            firstDayReportingWeek,
            lastDayReportingWeek,
            country
        ORDER BY
            lastDayReportingWeek,
            localAuthority
        """
    )

    override fun checkQueryState(queryId: QueryId): QueryResult<Unit> = asyncDbClient.queryResults(queryId)

}
