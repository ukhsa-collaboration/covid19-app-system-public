package uk.nhs.nhsx.pubdash.persistence

import uk.nhs.nhsx.pubdash.QueryId
import uk.nhs.nhsx.pubdash.QueryResult
import uk.nhs.nhsx.pubdash.datasets.AnalyticsSource

class AnalyticsDao(
    private val workspace: String,
    private val asyncDbClient: AsyncDbClient,
    private val mobileAnalyticsTable: String,
) : AnalyticsSource {

    override fun startAgnosticDatasetQueryAsync(): QueryId = asyncDbClient.submitQuery(
        """
        SELECT
            DATE_FORMAT(firstDayReportingWeek, '%Y-%m-%d') AS "Week starting (Wythnos yn dechrau)",
            DATE_FORMAT(lastDayReportingWeek, '%Y-%m-%d') AS "Week ending (Wythnos yn gorffen)",
            SUM(coalesce(downloads,0)) AS "Number of app downloads (Nifer o lawrlwythiadau ap)",
            SUM(coalesce(riskyVenues,0)) AS "Number of venues the app has sent alerts about (Nifer o leoliadau mae’r ap wedi anfon hysbysiadau amdanynt)",
            SUM(coalesce(posters, 0)) AS "Number of NHS QR posters created (Nifer o bosteri cod QR y GIG a grëwyd)",
            SUM(SUM(coalesce(downloads,0))) OVER (ORDER BY lastDayReportingWeek) AS "Cumulative number of app downloads (Nifer o lawrlwythiadau ap cronnus)",
            SUM(SUM(coalesce(riskyVenues,0))) OVER (ORDER BY lastDayReportingWeek) AS "Cumulative number of 'at risk' venues triggering venue alerts (Nifer o leoliadau 'dan risg' cronnus)",
            SUM(SUM(coalesce(posters, 0))) OVER (ORDER BY lastDayReportingWeek) AS "Cumulative number of NHS QR posters created (Nifer o bosteri cod QR y GIG a grëwyd cronnus)"
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
			SUM(countUsersReceivedPositivePCRTestResult) AS "Positive PCR test results linked to app (Canlyniadau prawf PCR positif cysylltiedig i ap)",
			SUM(countUsersReceivedPositiveLFDTestResult) AS "Positive LFD test results linked to app (Canlyniadau prawf LFD positif cysylltiedig i ap)",
            
            /* Cumulative Measures */
            SUM(SUM(totalCheckIns)) OVER (PARTITION BY country ORDER BY lastDayReportingWeek) AS "Cumulative check-ins (Cofrestriadau cronnus)",
            SUM(SUM(countUsersCompletedQuestionnaireAndStartedIsolation)) OVER (PARTITION BY country ORDER BY lastDayReportingWeek) AS "Cumulative symptoms reported (Symptomau a adroddwyd cronnus)",
            SUM(SUM(countUsersReceivedPositiveTestResult)) OVER (PARTITION BY country ORDER BY lastDayReportingWeek) AS "Cumulative positive test results linked to app (Canlyniadau prawf positif cronnus)",
            SUM(SUM(countUsersReceivedNegativeTestResult)) OVER (PARTITION BY country ORDER BY lastDayReportingWeek) AS "Cumulative negative test results linked to app (Canlyniadau prawf negatif cronnus)",
            SUM(SUM(countUsersReceivedRiskyContactNotification)) OVER (PARTITION BY country ORDER BY lastDayReportingWeek) AS "Cumulative contact tracing alert (Hysbysiadau olrhain cyswllt cronnus)",
			SUM(SUM(countUsersReceivedPositivePCRTestResult)) OVER (PARTITION BY country ORDER BY lastDayReportingWeek) AS "Cumulative Positive PCR test results linked to app (Canlyniadau prawf PCR positif cronnol cysylltiedig i ap)",
			SUM(SUM(countUsersReceivedPositiveLFDTestResult)) OVER (PARTITION BY country ORDER BY lastDayReportingWeek) AS "Cumulative Positive LFD test results linked to app (Canlyniadau prawf LFD positif cronnol cysylltiedig i ap)"


            
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
                END AS countUsersCompletedQuestionnaireAndStartedIsolation,
				CASE WHEN
					SUM(countUsersReceivedPositivePCRTestResult) < 5 AND SUM(countUsersReceivedPositivePCRTestResult) > 0 THEN 5
					ELSE SUM(countUsersReceivedPositivePCRTestResult)
				END AS countUsersReceivedPositivePCRTestResult,
				CASE WHEN 
					SUM(countUsersReceivedPositiveLFDTestResult) < 5 AND SUM(countUsersReceivedPositiveLFDTestResult) > 0 THEN 5
					ELSE SUM(countUsersReceivedPositiveLFDTestResult)
				END AS countUsersReceivedPositiveLFDTestResult
        
            FROM(
                SELECT
                    truncatedStartDate,
                    postalDistrict,
                    lad20cd,
                    SUM(checkedin) AS totalCheckIns,
                    SUM(completedQuestionnaireAndStartedIsolationInd) AS countUsersCompletedQuestionnaireAndStartedIsolation,
                    SUM(receivedPositiveTestResultInd) AS countUsersReceivedPositiveTestResult,
                    SUM(receivedNegativeTestResultInd) AS countUsersReceivedNegativeTestResult,
                    SUM(receivedRiskyContactNotificationInd) AS countUsersReceivedRiskyContactNotification,
					SUM(receivedPositivePCRResultInd) AS countUsersReceivedPositivePCRTestResult, 
					SUM(receivedPositiveLFDTestResultInd) AS countUsersReceivedPositiveLFDTestResult

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
						
						/* Positive PCR */ 
						CASE WHEN aad.receivedpositivetestresultviapolling > 0 
							OR aad.receivedpositivetestresultenteredmanually > 0 
							OR (aad.receivedpositivetestresult > 0
								AND(aad.receivedpositivelfdtestresultenteredmanually IS NULL OR aad.receivedpositivelfdtestresultenteredmanually = 0)
								AND(aad.receivedunconfirmedpositivetestresult IS NULL OR aad.receivedunconfirmedpositivetestresult = 0)
								AND(aad.receivedpositiveselfrapidtestresultenteredmanually IS NULL OR aad.receivedpositiveselfrapidtestresultenteredmanually = 0)) THEN 1 ELSE 0 END AS receivedPositivePCRResultInd,

						/* Positive LFD - here we are amalgamating self administered and and assisted LFD*/				
						CASE WHEN (aad.latestapplicationversion IN ('4.3', '4.4', '4.5', '4.6', '4.6.1', '4.7', '4.7.1', '4.8', '4.9', '4.10')
							AND aad.receivedpositivelfdtestresultenteredmanually > 0 
							AND(aad.receivedunconfirmedpositivetestresult IS NULL OR aad.receivedunconfirmedpositivetestresult = 0))
							OR
							(aad.latestapplicationversion NOT IN ('4.3', '4.4', '4.5', '4.6', '4.6.1', '4.7', '4.7.1', '4.8', '4.9', '4.10')
							AND aad.receivedpositivelfdtestresultenteredmanually > 0 
							AND aad.receivedunconfirmedpositivetestresult > 0
							AND aad.receivedunconfirmedpositivetestresult IS NOT NULL)	
							OR
							(aad.latestapplicationversion IN ('4.4', '4.5') 
							AND aad.receivedpositivelfdtestresultenteredmanually > 0 
							AND aad.receivedunconfirmedpositivetestresult > 0
							AND aad.receivedpositiveselfrapidtestresultenteredmanually IS NULL)
							OR
							(aad.latestapplicationversion NOT IN ('4.4', '4.5') 
							AND aad.receivedpositiveselfrapidtestresultenteredmanually > 0
							AND aad.receivedunconfirmedpositivetestresult > 0 
							AND aad.receivedpositivelfdtestresultenteredmanually = 0
							AND aad.receivedpositiveselfrapidtestresultenteredmanually IS NOT NULL)
							THEN 1 ELSE 0 END AS receivedPositiveLFDTestResultInd,
						
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
					AND aad.latestApplicationVersion NOT LIKE '%-internal%'
					AND aad.latestApplicationVersion NOT IN ('30','32','36','39','42','29','3.11','3.2','3.3','3.4','3.5','3.8','4.11','4.8','4.15','4.20') 
					AND  date_parse(substring(aad.startdate, 1, 10), '%Y-%c-%d') <= date_parse(substring(aad.submitteddatehour,1,13), '%Y/%c/%d/%H') 
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
 			SUM(countUsersRecievedPositivePCRTestResult) AS "Positive PCR test results linked to app (Canlyniadau prawf PCR positif cysylltiedig i ap)",
			SUM(countUsersRecievedPositiveLFDTestResult) AS "Positive LFD test results linked to app (Canlyniadau prawf LFD positif cysylltiedig i ap)",
            
            /* Cumulative Measures */
            SUM(SUM(totalCheckIns)) OVER (PARTITION BY localAuthority ORDER BY lastDayReportingWeek) AS "Cumulative check-ins (Cofrestriadau cronnus)",
            SUM(SUM(countUsersCompletedQuestionnaireAndStartedIsolation)) OVER (PARTITION BY localAuthority ORDER BY lastDayReportingWeek) AS "Cumulative symptoms reported (Symptomau a adroddwyd cronnus)",
            SUM(SUM(countUsersReceivedPositiveTestResult)) OVER (PARTITION BY localAuthority ORDER BY lastDayReportingWeek) AS "Cumulative positive test results linked to app (Canlyniadau prawf positif cronnus)",
            SUM(SUM(countUsersReceivedNegativeTestResult)) OVER (PARTITION BY localAuthority ORDER BY lastDayReportingWeek) AS "Cumulative negative test results linked to app (Canlyniadau prawf negatif cronnus)",
            SUM(SUM(countUsersReceivedRiskyContactNotification)) OVER (PARTITION BY localAuthority ORDER BY lastDayReportingWeek) AS "Cumulative contact tracing alert (Hysbysiadau olrhain cyswllt cronnus)",
 			SUM(SUM(countUsersRecievedPositivePCRTestResult)) OVER (PARTITION BY localAuthority ORDER BY lastDayReportingWeek) AS "Cumulative Positive PCR test results linked to app (Canlyniadau prawf PCR positif cronnol cysylltiedig i ap)",
			SUM(SUM(countUsersRecievedPositiveLFDTestResult)) OVER (PARTITION BY localAuthority ORDER BY lastDayReportingWeek) AS "Cumulative Positive LFD test results linked to app (Canlyniadau prawf LFD positif cronnol cysylltiedig i ap)"


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
                END AS countUsersCompletedQuestionnaireAndStartedIsolation,
				CASE WHEN
					SUM(countUsersRecievedPositivePCRTestResult) < 5 AND SUM(countUsersRecievedPositivePCRTestResult) > 0 THEN 5
					ELSE SUM(countUsersRecievedPositivePCRTestResult)
				END AS countUsersRecievedPositivePCRTestResult,
				CASE WHEN
					SUM(countUsersRecievedPositiveLFDTestResult) < 5 AND SUM(countUsersRecievedPositiveLFDTestResult) > 0 THEN 5
					ELSE SUM(countUsersRecievedPositiveLFDTestResult)
				END AS countUsersRecievedPositiveLFDTestResult
        
            FROM(
                SELECT
                    truncatedStartDate,
                    postalDistrict,
                    lad20cd,
                    SUM(checkedin) AS totalCheckIns,
                    SUM(completedQuestionnaireAndStartedIsolationInd) AS countUsersCompletedQuestionnaireAndStartedIsolation,
                    SUM(receivedPositiveTestResultInd) AS countUsersReceivedPositiveTestResult,
                    SUM(receivedNegativeTestResultInd) AS countUsersReceivedNegativeTestResult,
                    SUM(receivedRiskyContactNotificationInd) AS countUsersReceivedRiskyContactNotification,
					SUM(recievedPositivePCRResultInd) AS countUsersRecievedPositivePCRTestResult, 
					SUM(recievedPositiveLFDTestResultInd) AS countUsersRecievedPositiveLFDTestResult
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
                        
                        /* Positive PCR */ 
						CASE WHEN aad.receivedpositivetestresultviapolling > 0 
							OR aad.receivedpositivetestresultenteredmanually > 0 
							OR (aad.receivedpositivetestresult > 0
								AND(aad.receivedpositivelfdtestresultenteredmanually IS NULL OR aad.receivedpositivelfdtestresultenteredmanually = 0)
								AND(aad.receivedunconfirmedpositivetestresult IS NULL OR aad.receivedunconfirmedpositivetestresult = 0)
								AND(aad.receivedpositiveselfrapidtestresultenteredmanually IS NULL OR aad.receivedpositiveselfrapidtestresultenteredmanually = 0)) THEN 1 ELSE 0 END AS recievedPositivePCRResultInd,
						
						/* Positive LFD - here we are amalgamating self administered and and assisted LFD*/			
						CASE WHEN (aad.latestapplicationversion IN ('4.3', '4.4', '4.5', '4.6', '4.6.1', '4.7', '4.7.1', '4.8', '4.9', '4.10')
							AND aad.receivedpositivelfdtestresultenteredmanually > 0 
							AND(aad.receivedunconfirmedpositivetestresult IS NULL OR aad.receivedunconfirmedpositivetestresult = 0))
							OR
							(aad.latestapplicationversion NOT IN ('4.3', '4.4', '4.5', '4.6', '4.6.1', '4.7', '4.7.1', '4.8', '4.9', '4.10')
							AND aad.receivedpositivelfdtestresultenteredmanually > 0 
							AND aad.receivedunconfirmedpositivetestresult > 0
							AND aad.receivedunconfirmedpositivetestresult IS NOT NULL)	
							OR
							(aad.latestapplicationversion IN ('4.4', '4.5') 
							AND aad.receivedpositivelfdtestresultenteredmanually > 0 
							AND aad.receivedunconfirmedpositivetestresult > 0
							AND aad.receivedpositiveselfrapidtestresultenteredmanually IS NULL)
							OR
							(aad.latestapplicationversion NOT IN ('4.4', '4.5') 
							AND aad.receivedpositiveselfrapidtestresultenteredmanually > 0
							AND aad.receivedunconfirmedpositivetestresult > 0 
							AND aad.receivedpositivelfdtestresultenteredmanually = 0
							AND aad.receivedpositiveselfrapidtestresultenteredmanually IS NOT NULL)
							THEN 1 ELSE 0 END AS recievedPositiveLFDTestResultInd,
						
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
					    AND aad.latestApplicationVersion NOT LIKE '%-internal%'
					    AND aad.latestApplicationVersion NOT IN ('30','32','36','39','42','29','3.11','3.2','3.3','3.4','3.5','3.8','4.11','4.8','4.15','4.20')
						AND date_parse(substring(aad.startdate, 1, 10), '%Y-%c-%d') <= date_parse(substring(aad.submitteddatehour,1,13), '%Y/%c/%d/%H') 
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

    override fun startAppUsageDataByLocalAuthorityDatasetQueryAsync(): QueryId = asyncDbClient.submitQuery(
        """
            WITH

            population

            AS

            (
            SELECT
            CASE 
            WHEN country = 'England' THEN 'England / Lloegr' 
            WHEN country = 'Wales' THEN 'Wales / Cymru'
            ELSE NULL
            END AS "Country (Wlad)",
            local_authority AS "Local authority (Awdurdod lleol)",
            SUM (local_authority_population) AS population

            FROM "${workspace}_analytics_db"."${workspace}_analytics_Local_Authorities_demographic_geographic_lookup"

            GROUP BY
            CASE 
            WHEN country = 'England' THEN 'England / Lloegr' 
            WHEN country = 'Wales' THEN 'Wales / Cymru'
            ELSE NULL
            END,
            local_authority
            )
            ,


            main

            AS

            (
            SELECT 
            DATE_FORMAT(firstDayReportingWeek, '%Y-%m-%d') AS "Week starting (Wythnos yn dechrau)",
                DATE_FORMAT(lastDayReportingWeek, '%Y-%m-%d') AS "Week ending (Wythnos yn gorffen)",
                CASE country 
                WHEN 'England' THEN 'England / Lloegr' 
                WHEN 'Wales' THEN 'Wales / Cymru'
                END AS "Country (Wlad)",
                local_authority AS "Local authority (Awdurdod lleol)",	
                
                /* Small number suppression */
                CASE
                WHEN CAST(ROUND(AVG(NumberOfUsersAppInstalledInd)) AS BIGINT) <5
                AND CAST(ROUND(AVG(NumberOfUsersAppInstalledInd)) AS BIGINT) >0
                THEN 5
                ELSE CAST(ROUND(AVG(NumberOfUsersAppInstalledInd)) AS BIGINT)
            END AS "Average Daily Number of Users With App Installed (Cyfartaledd Nifer o Ddefnyddwyr Dyddiol gyda'r Ap wedi ei Osod)",	
                
                CASE
                WHEN CAST(ROUND(AVG(NumberOfUsersAppIsContactTraceable)) AS BIGINT) <5
                AND CAST(ROUND(AVG(NumberOfUsersAppIsContactTraceable)) AS BIGINT) >0
                THEN 5
                ELSE CAST(ROUND(AVG(NumberOfUsersAppIsContactTraceable)) AS BIGINT)
            END AS "Average Daily Number of Users Where App is Contact Traceable (Cyfartaledd Nifer o Ddefnyddwyr Dyddiol ble mae'r Ap yn Caniatáu Olrhain Cyswllt)"	


            FROM(SELECT
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
                
                truncatedStartDate,
                local_authority,
                country,

                SUM(AppInstalled) AS NumberOfUsersAppInstalledInd,
                SUM(AppIsContactTraceable) AS NumberOfUsersAppIsContactTraceable

            FROM (SELECT
                truncatedStartDate,
                local_authority,
                country,
                
                1 AS AppInstalled, 

                CASE WHEN appstate in (
                    'Fully Contact Traceable (Scenario 7.1)',
                    'Partly Contact Traceable Due to Disabled(Scenario 7.2)',
                    'Partly Contact Traceable Due to Pause or Disabled(Scenario 16.3)',
                    'Partly Contact Traceable Due to Pause(Scenario 19.1)',
                    'Partly Contact Traceable Due to Pause and Disabled(Scenario 19.2)',
                    'Partly Contact Traceable Due to Disabled(Scenario 8.2)',
                    'Fully Contact Traceable (Scenario 8.1)',
                    'Partly Contact Traceable Due to Pause(Scenario 15.1)',
                    'Partly Contact Traceable Due to Both(Scenario 16.2)',
                    'Partly Contact Traceable Due to Pause(Scenario 16.1)',
                    'Partly Contact Traceable Due to Both(Scenario 15.2)',
                    'App Fully Usable and Contact Tracing On',
                    'App Partially Usable and Contact Tracing On',
                    'App Fully Useable and Contact Tracing Paused',
                    'App Partially Usable and Contact Tracing Paused')  THEN 1 ELSE 0 END AS AppIsContactTraceable 

                    FROM(

            SELECT date_parse(substring(aad.startdate,1,10),'%Y-%c-%d') AS truncatedstartdate,  
                /*This section contains geographic information about the packet at district and LA level*/
                COALESCE(pdgl.local_authority, pdgl2.local_authority) AS local_authority,
                COALESCE(pdgl.country, pdgl2.country) as country,

             /* New appstate field scenarios (v4.17+) */
              
            CASE
                WHEN aad.latestApplicationVersion NOT IN ('4.5','3.0.1','3.8','4.1.1','3.0','4.9','3.6.2-internal','4.2.1','39','3.10',
            	'4.13.1','29','4.0','4.6.1','4.2','3.10.2','3.10.1','4.16','3.6-internal','3.1.2','4.6','4.1','32','42','4.15','3.12.2','3.7.1','3.2','4.13','4.3','3.5',
            	'3.5-internal','3.3-internal','3.9','3.3','3.7','3.12','30','3.11','4.7','35','3.12.1','4.12','3.7-internal',
            	'4.11','4.14.1','3.4','4.4','3.7.2','3.1','41','3.9-internal','3.6.1','3.1.1','4.7.1','4.18','4.14','3.4-internal','36','3.6.2','4.0.1','3.6','4.10','4.8') THEN
             
            CASE
               
            /* ANDROID FULLY USABLE AND CONTACT TRACING ON v4.17+ */
              
            /* The appisusablebackgroundtick ran every time a background task ran. The appiscontacttraceablebackgroundtick ran every time the appisusablebackgroundtick ran. The background task has run at least once (so it is definitely working). */
                WHEN aad.appisusablebackgroundtick = aad.totalBackgroundTasks 
            	AND aad.appiscontacttraceablebackgroundtick = aad.appisusablebackgroundtick
            	AND aad.totalBackgroundTasks > 0
            	THEN 'App Fully Usable and Contact Tracing On'
                
            /* ANDROID FULLY USABLE AND CONTACT TRACING OFF v4.17+ */
              
            /* The appisusablebackgroundtick ran every time a background task ran. The background task has run at least once (so it is definitely working). The appiscontacttraceablebackgroundtick did not run at all, so contact tracing is assumed to be off. */	    
                WHEN aad.appisusablebackgroundtick = aad.totalBackgroundTasks 
            	AND aad.totalBackgroundTasks > 0
            	AND aad.appiscontacttraceablebackgroundtick = 0
            	THEN 'App Fully Usable and Contact Tracing Off'
               
            /* ANDROID FULLY USABLE AND CONTACT TRACING PAUSED v4.17+ */
              
            /* The appisusablebackgroundtick ran every time a background task ran. The background task has run at least once (so it is definitely working). The appiscontacttraceablebackgroundtick did run at some stage, but not every time the appisusablebackgroundtick happened - so contact tracing was on for some of the time. */	
            	WHEN aad.appisusablebackgroundtick = aad.totalBackgroundTasks 
            	AND aad.totalBackgroundTasks > 0
            	AND aad.appiscontacttraceablebackgroundtick > 0
            	AND aad.appiscontacttraceablebackgroundtick < aad.appisusablebackgroundtick
            	THEN 'App Fully Useable and Contact Tracing Paused'
              
              
            /* ANDROID NOT USABLE v4.17+ */
              
            /* The appisusablebackgroundtick did not run. The background task has run at least once (so it is definitely working) The app is therefore assumed to be unusable. */			    
                WHEN aad.appisusablebackgroundtick = 0
            	AND aad.totalBackgroundTasks > 0
            	THEN 'App Not Usable'
            			    
            /* ANDROID PARTIALLY USABLE AND CONTACT TRACING ON v4.17+ */
              
            /* The appisusablebackgroundtick ran, but not every time a background task ran - so the app was not usable at all times during the day. The background task has run at least once (so it is definitely working). The appiscontacttraceablebackgroundtick ran every time the appisusablebackgroundtick ran. */
                WHEN aad.appisusablebackgroundtick < aad.totalBackgroundTasks 
            	AND aad.appisusablebackgroundtick > 0
            	AND aad.totalBackgroundTasks > 0
            	AND aad.appiscontacttraceablebackgroundtick = aad.appisusablebackgroundtick
            	THEN 'App Partially Usable and Contact Tracing On'
            			    
            /* ANDROID PARTIALLY USABLE AND CONTACT TRACING PAUSED v4.17+ */
              
            /* The appisusablebackgroundtick ran, but not every time a background task ran - so the app was not usable at all times during the day. The background task has run at least once (so it is definitely working). The appiscontacttraceablebackgroundtick did run at some stage, but not every time the appisusablebackgroundtick happened - so contact tracing was on for some of the time. */
                WHEN aad.appisusablebackgroundtick < aad.totalBackgroundTasks 
            	AND aad.appisusablebackgroundtick > 0
            	AND aad.totalBackgroundTasks > 0
            	AND  aad.appiscontacttraceablebackgroundtick < aad.appisusablebackgroundtick
            	AND  aad.appiscontacttraceablebackgroundtick> 0    
                THEN 'App Partially Usable and Contact Tracing Paused' 
            			    

            /* ANDROID PARTIALLY USABLE AND CONTACT TRACING OFF v4.17+ */
              
            /* The appisusablebackgroundtick ran, but not every time a background task ran - so the app was not usable at all times during the day. The background task has run at least once (so it is definitely working). The appiscontacttraceablebackgroundtick did not run at all, so contact tracing is assumed to be off. */
                WHEN aad.appisusablebackgroundtick < aad.totalBackgroundTasks 
            	AND aad.appisusablebackgroundtick > 0
            	AND aad.totalBackgroundTasks > 0
            	AND  aad.appiscontacttraceablebackgroundtick = 0
                THEN 'App Partially Usable and Contact Tracing Off'
            			    
            /* ANDROID BACKGROUND TASKS NOT RUNNING v4.17+ */
              
            /* No background tasks running */
                WHEN  aad.totalBackgroundTasks = 0
                THEN 'Background Tasks Not Running'
            	  
                ELSE 'Other'
            END
              
            /* Android appstate scenarios */      
              
              WHEN Upper(devicemodel) NOT LIKE '%IPHONE%' THEN
              
            CASE 
              

            /* ANDROID NON-CONTACT TRACEABLE SCENARIO 1 */
              
            /* No background tasks are running (eg. due to not being fully onboarded, exposure notifications being turned off in the settings, bluetooth being switched off, network connectivity unavailable, etc). Alarm Manager did not run, so a packet was not sent via Alarm Manager. */
                WHEN (aad.latestApplicationVersion NOT IN ('3.0','3.1','3.10','3.11','3.12','3.2','3.3','3.4','3.5','3.6','3.7','3.8','3.9','4.0','4.1','4.2','4.3','4.4','4.5','4.6','4.7','4.8','4.9','29','30','32','35','36','39','41','42','3.0.1','3.1.1','3.1.2','3.10.1','3.10.2','3.12.1','3.12.2','3.3-internal','3.4-internal','3.5-internal','3.6.1','3.6.2','3.6.2-internal','3.6-internal','3.7.1','3.7.2','3.7-internal','3.9-internal','4.0.1','4.1.1','4.2.1','4.6.1','4.7.1')
            	AND aad.encounterdetectionpausedbackgroundtick = 0
            	AND aad.runningnormallybackgroundtick = 0 
            	AND aad.totalBackgroundTasks = 0
            	AND aad.totalAlarmManagerBackgroundTasks = 0)
            	
                OR
              
            /* No background tasks are running (eg. due to not being fully onboarded, exposure notifications being turned off in the settings, bluetooth being switched off, network connectivity unavailable, etc). */
                (aad.latestApplicationVersion IN ('4.3','4.4','4.5','4.6','4.6.1','4.7','4.7.1','4.9','4.8')
            	AND aad.encounterdetectionpausedbackgroundtick = 0
            	AND aad.runningnormallybackgroundtick = 0 
            	AND aad.totalBackgroundTasks = 0)
            				
                OR
              
            /* No background tasks are running (eg. due to not being fully onboarded, exposure notifications being turned off in the settings, bluetooth being switched off, network connectivity unavailable, etc). */
                (aad.latestApplicationVersion IN ('4.2.1','4.2','4.1.1','4.1','4.0.1','4.0','3.12.2','3.12.1','3.12','3.10','3.9','3.7.2','3.7.1','3.7','3.6.2','3.6.1','3.6','35','41')
                AND aad.encounterdetectionpausedbackgroundtick = 0
                AND aad.runningnormallybackgroundtick = 0 
                AND aad.totalBackgroundTasks = 0)
                THEN 'Not Contact Traceable (Scenario 1)'			

            /* ANDROID NON-CONTACT TRACEABLE SCENARIO 2 */
              
            /* No background tasks are running (eg. due to not being fully onboarded, exposure notifications being turned off in the settings, bluetooth being switched off, network connectivity unavailable, etc). But Alarm Manager ran, so a packet was sent via Alarm Manager. */
            	WHEN aad.latestApplicationVersion NOT IN ('3.0','3.1','3.10','3.11','3.12','3.2','3.3','3.4','3.5','3.6','3.7','3.8','3.9','4.0','4.1','4.2','4.3','4.4','4.5','4.6','4.7','4.8','4.9','29','30','32','35','36','39','41','42','3.0.1','3.1.1','3.1.2','3.10.1','3.10.2','3.12.1','3.12.2','3.3-internal','3.4-internal','3.5-internal','3.6.1','3.6.2','3.6.2-internal','3.6-internal','3.7.1','3.7.2','3.7-internal','3.9-internal','4.0.1','4.1.1','4.2.1','4.6.1','4.7.1')
            	AND aad.encounterdetectionpausedbackgroundtick = 0
            	AND aad.runningnormallybackgroundtick = 0 
            	AND aad.totalBackgroundTasks = 0
            	AND aad.totalAlarmManagerBackgroundTasks > 0
            	THEN 'Not Contact Traceable (Scenario 2)'	

            /* ANDROID NON-CONTACT TRACEABLE SCENARIO 3 */
              
            /* Encounter detection is not paused but the app is not running normally (eg. due to not being fully onboarded, exposure notifications being turned off in the settings, bluetooth being switched off, network connectivity unavailable, etc). The background task has run at least once (so it is definitely working). Alarm Manager did not run, so a packet was not sent via Alarm Manager. */
            	WHEN (aad.latestApplicationVersion NOT IN ('3.0','3.1','3.10','3.11','3.12','3.2','3.3','3.4','3.5','3.6','3.7','3.8','3.9','4.0','4.1','4.2','4.3','4.4','4.5','4.6','4.7','4.8','4.9','29','30','32','35','36','39','41','42','3.0.1','3.1.1','3.1.2','3.10.1','3.10.2','3.12.1','3.12.2','3.3-internal','3.4-internal','3.5-internal','3.6.1','3.6.2','3.6.2-internal','3.6-internal','3.7.1','3.7.2','3.7-internal','3.9-internal','4.0.1','4.1.1','4.2.1','4.6.1','4.7.1')
            	AND aad.encounterdetectionpausedbackgroundtick = 0
            	AND aad.runningnormallybackgroundtick = 0 
            	AND aad.totalBackgroundTasks > 0
            	AND aad.totalAlarmManagerBackgroundTasks = 0)
            	
                OR
              
            /* Encounter detection is not paused but the app is not running normally (eg. due to not being fully onboarded, exposure notifications being turned off in the settings, bluetooth being switched off, network connectivity unavailable, etc). The background task has run at least once (so it is definitely working). */
                (aad.latestApplicationVersion IN ('4.3','4.4','4.5','4.6','4.6.1','4.7','4.7.1','4.9','4.8')
                AND aad.encounterdetectionpausedbackgroundtick = 0
                AND aad.runningnormallybackgroundtick = 0 
                AND aad.totalBackgroundTasks > 0)
                THEN 'Not Contact Traceable (Scenario 3)'
            			

            /* ANDROID NON-CONTACT TRACEABLE SCENARIO 4 */

            /* Encounter detection is not paused but the app is not running normally (eg. due to not being fully onboarded, exposure notifications being turned off in the settings, bluetooth being switched off, network connectivity unavailable, etc). runningNormallyBackgroundTick + encounterDetectionPausedBackgroundTick will normally equal totalBackgroundTasks. In this case it does not, suggesting EN is enabled but Bluetooth is disabled. runningnormallybackgroundtick will not tick up as Bluetooth is off, but a background task has still occurred as EN is on. Alarm Manager ran, so a packet was sent via Alarm Manager. */
            	WHEN aad.latestApplicationVersion NOT IN ('3.0','3.1','3.10','3.11','3.12','3.2','3.3','3.4','3.5','3.6','3.7','3.8','3.9','4.0','4.1','4.2','4.3','4.4','4.5','4.6','4.7','4.8','4.9','29','30','32','35','36','39','41','42','3.0.1','3.1.1','3.1.2','3.10.1','3.10.2','3.12.1','3.12.2','3.3-internal','3.4-internal','3.5-internal','3.6.1','3.6.2','3.6.2-internal','3.6-internal','3.7.1','3.7.2','3.7-internal','3.9-internal','4.0.1','4.1.1','4.2.1','4.6.1','4.7.1')
            	AND aad.encounterdetectionpausedbackgroundtick = 0
            	AND aad.runningnormallybackgroundtick = 0 
            	AND aad.totalBackgroundTasks > 0
            	AND aad.totalAlarmManagerBackgroundTasks > 0
            	THEN 'Not Contact Traceable (Scenario 4)'		

            /* ANDROID FULLY CONTACT TRACEABLE SCENARIO 7.1 */
              
            /* Encounter detection is not paused. runningnormallybackgroundtick = totalBackgroundTasks (so every time the app has run in the background (totalBackgroundTasks), the app has been fully functional (runningnormallybackgroundtick). The background task has run at least once (so it is definitely working). Alarm Manager did not run, so a packet was not sent via Alarm Manager. */
            	WHEN (aad.latestApplicationVersion NOT IN ('3.0','3.1','3.10','3.11','3.12','3.2','3.3','3.4','3.5','3.6','3.7','3.8','3.9','4.0','4.1','4.2','4.3','4.4','4.5','4.6','4.7','4.8','4.9','29','30','32','35','36','39','41','42','3.0.1','3.1.1','3.1.2','3.10.1','3.10.2','3.12.1','3.12.2','3.3-internal','3.4-internal','3.5-internal','3.6.1','3.6.2','3.6.2-internal','3.6-internal','3.7.1','3.7.2','3.7-internal','3.9-internal','4.0.1','4.1.1','4.2.1','4.6.1','4.7.1')
            	AND aad.encounterdetectionpausedbackgroundtick = 0
            	AND aad.runningnormallybackgroundtick = aad.totalBackgroundTasks
            	AND aad.totalBackgroundTasks > 0
            	AND aad.totalAlarmManagerBackgroundTasks = 0)
            	
                OR
              
            /* Encounter detection is not paused. runningnormallybackgroundtick = totalBackgroundTasks (so every time the app has run in the background (totalBackgroundTasks), the app has been fully functional (runningnormallybackgroundtick). The background task has run at least once (so it is definitely working). */
                (aad.latestApplicationVersion IN ('4.3','4.4','4.5','4.6','4.6.1','4.7','4.7.1','4.9','4.8')
                AND aad.encounterdetectionpausedbackgroundtick = 0
                AND aad.runningnormallybackgroundtick = aad.totalBackgroundTasks
                AND aad.totalBackgroundTasks >0)
            	
                OR
              
              /* Encounter detection is not paused. runningnormallybackgroundtick = totalBackgroundTasks (so every time the app has run in the background (totalBackgroundTasks), the app has been fully functional (runningnormallybackgroundtick). And the background task has run at least once (so it is definitely working). */
                (aad.latestApplicationVersion IN ('4.2.1','4.2','4.1.1','4.1','4.0.1','4.0','3.12.2','3.12.1','3.12','3.10','3.9','3.7.2','3.7.1','3.7','3.6.2','3.6.1','3.6','35','41')
                AND aad.encounterdetectionpausedbackgroundtick = 0
                AND aad.runningnormallybackgroundtick = aad.totalBackgroundTasks 
                AND aad.totalBackgroundTasks >0)
                THEN 'Fully Contact Traceable (Scenario 7.1)'
            			
            /* ANDROID FULLY CONTACT TRACEABLE SCENARIO 8.1 */
                                 
            /* Encounter detection is not paused. runningnormallybackgroundtick = totalBackgroundTasks (so every time the app has run in the background (totalBackgroundTasks), the app has been fully functional (runningnormallybackgroundtick). The background task has run at least once (so it is definitely working). Alarm Manager ran, so a packet was sent via Alarm Manager. */
                WHEN aad.latestApplicationVersion NOT IN ('3.0','3.1','3.10','3.11','3.12','3.2','3.3','3.4','3.5','3.6','3.7','3.8','3.9','4.0','4.1','4.2','4.3','4.4','4.5','4.6','4.7','4.8','4.9','29','30','32','35','36','39','41','42','3.0.1','3.1.1','3.1.2','3.10.1','3.10.2','3.12.1','3.12.2','3.3-internal','3.4-internal','3.5-internal','3.6.1','3.6.2','3.6.2-internal','3.6-internal','3.7.1','3.7.2','3.7-internal','3.9-internal','4.0.1','4.1.1','4.2.1','4.6.1','4.7.1')
                AND aad.encounterdetectionpausedbackgroundtick = 0
                AND aad.runningnormallybackgroundtick = aad.totalBackgroundTasks
                AND aad.totalBackgroundTasks > 0
                AND aad.totalAlarmManagerBackgroundTasks > 0
                THEN 'Fully Contact Traceable (Scenario 8.1)'		

            /* ANDROID PARTLY CONTACT TRACEABLE SCENARIO 7.2 */
                                 
            /* Encounter detection is not paused. runningnormallybackgroundtick is less than totalBackgroundTasks (so there have been occasions during the day where the app has not been fully functional) - but runningnormallybackgroundtick is greater than 0 - so the app has been fully functional for part of the day. The background task has run at least once (so it is definitely working). Alarm Manager did not run, so a packet was not sent via Alarm Manager. */
                WHEN (aad.latestApplicationVersion NOT IN ('3.0','3.1','3.10','3.11','3.12','3.2','3.3','3.4','3.5','3.6','3.7','3.8','3.9','4.0','4.1','4.2','4.3','4.4','4.5','4.6','4.7','4.8','4.9','29','30','32','35','36','39','41','42','3.0.1','3.1.1','3.1.2','3.10.1','3.10.2','3.12.1','3.12.2','3.3-internal','3.4-internal','3.5-internal','3.6.1','3.6.2','3.6.2-internal','3.6-internal','3.7.1','3.7.2','3.7-internal','3.9-internal','4.0.1','4.1.1','4.2.1','4.6.1','4.7.1')
                AND aad.encounterdetectionpausedbackgroundtick = 0
                AND aad.runningnormallybackgroundtick < aad.totalBackgroundTasks
                AND aad.runningnormallybackgroundtick > 0
                AND aad.totalBackgroundTasks > 0
                AND aad.totalAlarmManagerBackgroundTasks = 0)
                
                OR
              
            /* Encounter detection is not paused. runningnormallybackgroundtick is less than totalBackgroundTasks (so there have been occasions during the day where the app has not been fully functional) - but runningnormallybackgroundtick is greater than 0 - so the app has been fully functional for part of the day. The background task has run at least once (so it is definitely working). */
                (aad.latestApplicationVersion IN ('4.3','4.4','4.5','4.6','4.6.1','4.7','4.7.1','4.9','4.8')
                AND aad.encounterdetectionpausedbackgroundtick = 0
                AND aad.runningnormallybackgroundtick < aad.totalBackgroundTasks
                AND aad.runningnormallybackgroundtick > 0
                AND aad.totalBackgroundTasks > 0)
                THEN 'Partly Contact Traceable Due to Disabled(Scenario 7.2)'
              
            /* ANDROID PARTLY CONTACT TRACEABLE SCENARIO 8.2 */
                                 
            /* Encounter detection is not paused. runningnormallybackgroundtick is less than totalBackgroundTasks (so there have been occasions during the day where the app has not been fully functional) - but runningnormallybackgroundtick is greater than 0 - so the app has been fully functional for part of the day. The background task has run at least once (so it definitely working). Alarm Manager ran, so a packet was sent via Alarm Manager. */
                WHEN aad.latestApplicationVersion NOT IN ('3.0','3.1','3.10','3.11','3.12','3.2','3.3','3.4','3.5','3.6','3.7','3.8','3.9','4.0','4.1','4.2','4.3','4.4','4.5','4.6','4.7','4.8','4.9','29','30','32','35','36','39','41','42','3.0.1','3.1.1','3.1.2','3.10.1','3.10.2','3.12.1','3.12.2','3.3-internal','3.4-internal','3.5-internal','3.6.1','3.6.2','3.6.2-internal','3.6-internal','3.7.1','3.7.2','3.7-internal','3.9-internal','4.0.1','4.1.1','4.2.1','4.6.1','4.7.1')
                AND aad.encounterdetectionpausedbackgroundtick = 0
                AND aad.runningnormallybackgroundtick < aad.totalBackgroundTasks
                AND aad.runningnormallybackgroundtick > 0
                AND aad.totalBackgroundTasks > 0
                AND aad.totalAlarmManagerBackgroundTasks > 0
                THEN 'Partly Contact Traceable Due to Disabled(Scenario 8.2)'
              
            /* ANDROID PARTLY CONTACT TRACEABLE SCENARIO 11.1 */
                                 
            /* Encounter detection is showing as paused every time a background task is run. runningnormallybackgroundtick = 0, indicating the app is not running normally (eg. due to not being fully onboarded, exposure notifications being turned off in the settings, bluetooth being switched off, network connectivity unavailable, etc). The background task has run at least once (so it is definitely working). Alarm Manager did not run, so a packet was not sent via Alarm Manager. */
                WHEN (aad.latestApplicationVersion NOT IN ('3.0','3.1','3.10','3.11','3.12','3.2','3.3','3.4','3.5','3.6','3.7','3.8','3.9','4.0','4.1','4.2','4.3','4.4','4.5','4.6','4.7','4.8','4.9','29','30','32','35','36','39','41','42','3.0.1','3.1.1','3.1.2','3.10.1','3.10.2','3.12.1','3.12.2','3.3-internal','3.4-internal','3.5-internal','3.6.1','3.6.2','3.6.2-internal','3.6-internal','3.7.1','3.7.2','3.7-internal','3.9-internal','4.0.1','4.1.1','4.2.1','4.6.1','4.7.1')
                AND aad.encounterdetectionpausedbackgroundtick = aad.totalBackgroundTasks
                AND aad.runningnormallybackgroundtick = 0
                AND aad.totalBackgroundTasks > 0
                AND aad.totalAlarmManagerBackgroundTasks = 0)
  
                OR
              
            /* Encounter detection is showing as paused every time a background task is run. runningnormallybackgroundtick = 0, indicating the app is not running normally (eg. due to not being fully onboarded, exposure notifications being turned off in the settings, bluetooth being switched off, network connectivity unavailable, etc). The background task has run at least once (so it is definitely working). */
                (aad.latestApplicationVersion IN ('4.3','4.4','4.5','4.6','4.6.1','4.7','4.7.1','4.9','4.8')
                AND aad.encounterdetectionpausedbackgroundtick = aad.totalBackgroundTasks
                AND aad.runningnormallybackgroundtick = 0
                AND aad.totalBackgroundTasks > 0)
                
                OR
              
            /* Encounter detection is showing as paused every time a background task is run. runningnormallybackgroundtick = 0, indicating the app is not running normally (eg. due to not being fully onboarded, exposure notifications being turned off in the settings, bluetooth being switched off, network connectivity unavailable, etc). The background task has run at least once (so it is definitely working). */
                (aad.latestApplicationVersion IN ('4.2.1','4.2','4.1.1','4.1','4.0.1','4.0','3.12.2','3.12.1','3.12','3.10','3.9','3.7.2','3.7.1','3.7','3.6.2','3.6.1','3.6','35','41')
                AND aad.encounterdetectionpausedbackgroundtick = aad.totalBackgroundTasks
                AND aad.runningnormallybackgroundtick = aad.totalBackgroundTasks 
                AND aad.totalBackgroundTasks >0)
                THEN 'Not Contact Traceable (Scenario 11.1)'	
             
            /* ANDROID NON-CONTACT TRACEABLE SCENARIO 11.2*/
                                 
            /* Encounter detection is paused at least once in the day, but not every time a background task is run. runningnormallybackgroundtick = 0, indicating the app is not running normally (eg. due to not being fully onboarded, exposure notifications being turned off in the settings, bluetooth being switched off, network connectivity unavailable, etc). The background task has run at least once (so it is definitely working). Alarm Manager did not run, so a packet was not sent via Alarm Manager. */
                WHEN (aad.latestApplicationVersion NOT IN ('3.0','3.1','3.10','3.11','3.12','3.2','3.3','3.4','3.5','3.6','3.7','3.8','3.9','4.0','4.1','4.2','4.3','4.4','4.5','4.6','4.7','4.8','4.9','29','30','32','35','36','39','41','42','3.0.1','3.1.1','3.1.2','3.10.1','3.10.2','3.12.1','3.12.2','3.3-internal','3.4-internal','3.5-internal','3.6.1','3.6.2','3.6.2-internal','3.6-internal','3.7.1','3.7.2','3.7-internal','3.9-internal','4.0.1','4.1.1','4.2.1','4.6.1','4.7.1')
                AND aad.encounterdetectionpausedbackgroundtick > 0
                AND aad.encounterdetectionpausedbackgroundtick < aad.totalBackgroundTasks
                AND aad.runningnormallybackgroundtick = 0
                AND aad.totalBackgroundTasks > 0
                AND aad.totalAlarmManagerBackgroundTasks = 0)
                
                OR
              
            /* Encounter detection is paused at least once in the day, but not every time a background task is run. runningnormallybackgroundtick = 0, indicating the app is not running normally (eg. due to not being fully onboarded, exposure notifications being turned off in the settings, bluetooth being switched off, network connectivity unavailable, etc). The background task has run at least once (so it is definitely working). */
                (aad.latestApplicationVersion IN ('4.3','4.4','4.5','4.6','4.6.1','4.7','4.7.1','4.9','4.8')
                AND aad.encounterdetectionpausedbackgroundtick > 0
                AND aad.encounterdetectionpausedbackgroundtick < aad.totalBackgroundTasks
                AND aad.runningnormallybackgroundtick = 0
                AND aad.totalBackgroundTasks > 0)
                THEN 'Not Contact Traceable (Scenario 11.2)'
              
            /* ANDROID NON-CONTACT TRACEABLE SCENARIO 12.1 */
                                 
            /* Encounter detection is showing as paused every time a background task is run. runningnormallybackgroundtick = 0, indicating the app is not running normally (eg. due to not being fully onboarded, exposure notifications being turned off in the settings, bluetooth being switched off, network connectivity unavailable, etc). The background task has run at least once (so it is definitely working). Alarm Manager ran, so a packet was sent via Alarm Manager. */
                WHEN aad.latestApplicationVersion NOT IN ('3.0','3.1','3.10','3.11','3.12','3.2','3.3','3.4','3.5','3.6','3.7','3.8','3.9','4.0','4.1','4.2','4.3','4.4','4.5','4.6','4.7','4.8','4.9','29','30','32','35','36','39','41','42','3.0.1','3.1.1','3.1.2','3.10.1','3.10.2','3.12.1','3.12.2','3.3-internal','3.4-internal','3.5-internal','3.6.1','3.6.2','3.6.2-internal','3.6-internal','3.7.1','3.7.2','3.7-internal','3.9-internal','4.0.1','4.1.1','4.2.1','4.6.1','4.7.1')
                AND aad.encounterdetectionpausedbackgroundtick = aad.totalBackgroundTasks
                AND aad.runningnormallybackgroundtick = 0
                AND aad.totalBackgroundTasks > 0
                AND aad.totalAlarmManagerBackgroundTasks > 0
                THEN 'Not Contact Traceable (Scenario 12.1)'		
              
            /* ANDROID NON-CONTACT TRACEABLE SCENARIO 12.2 */
                                 
            /* Encounter detection is paused at least once in the day, but not every time a background task is run. runningnormallybackgroundtick = 0, indicating the app is not running normally (eg. due to not being fully onboarded, exposure notifications being turned off in the settings, bluetooth being switched off, network connectivity unavailable, etc). The background task has run at least once (so it is definitely working). Alarm Manager ran, so a packet was sent via Alarm Manager. */
                WHEN aad.latestApplicationVersion NOT IN ('3.0','3.1','3.10','3.11','3.12','3.2','3.3','3.4','3.5','3.6','3.7','3.8','3.9','4.0','4.1','4.2','4.3','4.4','4.5','4.6','4.7','4.8','4.9','29','30','32','35','36','39','41','42','3.0.1','3.1.1','3.1.2','3.10.1','3.10.2','3.12.1','3.12.2','3.3-internal','3.4-internal','3.5-internal','3.6.1','3.6.2','3.6.2-internal','3.6-internal','3.7.1','3.7.2','3.7-internal','3.9-internal','4.0.1','4.1.1','4.2.1','4.6.1','4.7.1')
                AND aad.encounterdetectionpausedbackgroundtick > 0
                AND aad.encounterdetectionpausedbackgroundtick < aad.totalBackgroundTasks
                AND aad.runningnormallybackgroundtick = 0
                AND aad.totalBackgroundTasks > 0
                AND aad.totalAlarmManagerBackgroundTasks > 0
                THEN 'Not Contact Traceable (Scenario 12.2)' 				
              
            /* ANDROID PARTLY CONTACT TRACEABLE SCENARIO 15.1 */
                                 
            /* Encounter detection is paused at least once in the day, but not every time a background task is run. runningnormallybackgroundtick runs at least once in the day, meaning the app is running normally for part of the day. encounterdetectionpausedbackgroundtick + runningnormallybackgroundtick = totalBackgroundTasks, meaning all backround tasks are accounted for (when encounterdetectionpausedbackgroundtick + runningnormallybackgroundtick is less than totalBackgroundTasks, Bluetooth may be disabled). Alarm Manager did not run, so a packet was not sent via Alarm Manager. */
                WHEN (aad.latestApplicationVersion NOT IN ('3.0','3.1','3.10','3.11','3.12','3.2','3.3','3.4','3.5','3.6','3.7','3.8','3.9','4.0','4.1','4.2','4.3','4.4','4.5','4.6','4.7','4.8','4.9','29','30','32','35','36','39','41','42','3.0.1','3.1.1','3.1.2','3.10.1','3.10.2','3.12.1','3.12.2','3.3-internal','3.4-internal','3.5-internal','3.6.1','3.6.2','3.6.2-internal','3.6-internal','3.7.1','3.7.2','3.7-internal','3.9-internal','4.0.1','4.1.1','4.2.1','4.6.1','4.7.1')
                AND aad.encounterdetectionpausedbackgroundtick > 0
                AND aad.runningnormallybackgroundtick > 0
                AND aad.encounterdetectionpausedbackgroundtick + aad.runningnormallybackgroundtick = aad.totalBackgroundTasks
                AND aad.totalAlarmManagerBackgroundTasks = 0)
                
                OR
              
            /* Encounter detection is paused at least once in the day, but not every time a background task is run. runningnormallybackgroundtick runs at least once in the day, meaning the app is running normally for part of the day. encounterdetectionpausedbackgroundtick + runningnormallybackgroundtick = totalBackgroundTasks, meaning all backround tasks are accounted for (when encounterdetectionpausedbackgroundtick + runningnormallybackgroundtick is less than totalBackgroundTasks, Bluetooth may be disabled). */
                (aad.latestApplicationVersion IN ('4.3','4.4','4.5','4.6','4.6.1','4.7','4.7.1','4.9','4.8')
                AND aad.encounterdetectionpausedbackgroundtick > 0
                AND aad.runningnormallybackgroundtick > 0
                AND aad.encounterdetectionpausedbackgroundtick + aad.runningnormallybackgroundtick = aad.totalBackgroundTasks)
                THEN 'Partly Contact Traceable Due to Pause(Scenario 15.1)'	
              
            /* ANDROID PARTLY CONTACT TRACEABLE DUE TO BOTH SCENARIO 15.2 */
                                 
            /* Encounter detection is paused at least once in the day, but not every time a background task is run. runningnormallybackgroundtick runs at least once in the day, meaning the app is running normally for part of the day. encounterdetectionpausedbackgroundtick + runningnormallybackgroundtick is less than totalBackgroundTasks, meaning Bluetooth may be disabled for part of the day. Alarm Manager did not run, so a packet was not sent via Alarm Manager. */
                WHEN (aad.latestApplicationVersion NOT IN ('3.0','3.1','3.10','3.11','3.12','3.2','3.3','3.4','3.5','3.6','3.7','3.8','3.9','4.0','4.1','4.2','4.3','4.4','4.5','4.6','4.7','4.8','4.9','29','30','32','35','36','39','41','42','3.0.1','3.1.1','3.1.2','3.10.1','3.10.2','3.12.1','3.12.2','3.3-internal','3.4-internal','3.5-internal','3.6.1','3.6.2','3.6.2-internal','3.6-internal','3.7.1','3.7.2','3.7-internal','3.9-internal','4.0.1','4.1.1','4.2.1','4.6.1','4.7.1')
                AND aad.encounterdetectionpausedbackgroundtick > 0
                AND aad.runningnormallybackgroundtick > 0
                AND aad.encounterdetectionpausedbackgroundtick + aad.runningnormallybackgroundtick < aad.totalBackgroundTasks
                AND aad.totalAlarmManagerBackgroundTasks = 0)
                
                OR

            /* Encounter detection is paused at least once in the day, but not every time a background task is run. runningnormallybackgroundtick runs at least once in the day, meaning the app is running normally for part of the day. encounterdetectionpausedbackgroundtick + runningnormallybackgroundtick is less than totalBackgroundTasks, meaning Bluetooth may be disabled for part of the day. */
                (aad.latestApplicationVersion IN ('4.3','4.4','4.5','4.6','4.6.1','4.7','4.7.1','4.9','4.8')
                AND aad.encounterdetectionpausedbackgroundtick > 0
                AND aad.runningnormallybackgroundtick > 0
                AND aad.encounterdetectionpausedbackgroundtick + aad.runningnormallybackgroundtick < aad.totalBackgroundTasks)
                THEN 'Partly Contact Traceable Due to Both(Scenario 15.2)'	
              
            /* ANDROID PARTLY CONTACT TRACEABLE DUE TO PAUSE SCENARIO 16.1 */
                                 
            /* Encounter detection is paused at least once in the day, but not every time a background task is run. runningnormallybackgroundtick runs at least once in the day, meaning the app is running normally for part of the day. encounterdetectionpausedbackgroundtick + runningnormallybackgroundtick = totalBackgroundTasks, meaning all backround tasks are accounted for (when encounterdetectionpausedbackgroundtick + runningnormallybackgroundtick is less than totalBackgroundTasks, Bluetooth may be disabled). Alarm Manager ran, so a packet was sent via Alarm Manager. */
                WHEN aad.latestApplicationVersion NOT IN ('3.0','3.1','3.10','3.11','3.12','3.2','3.3','3.4','3.5','3.6','3.7','3.8','3.9','4.0','4.1','4.2','4.3','4.4','4.5','4.6','4.7','4.8','4.9','29','30','32','35','36','39','41','42','3.0.1','3.1.1','3.1.2','3.10.1','3.10.2','3.12.1','3.12.2','3.3-internal','3.4-internal','3.5-internal','3.6.1','3.6.2','3.6.2-internal','3.6-internal','3.7.1','3.7.2','3.7-internal','3.9-internal','4.0.1','4.1.1','4.2.1','4.6.1','4.7.1')
                AND aad.encounterdetectionpausedbackgroundtick > 0
                AND aad.runningnormallybackgroundtick > 0
                AND aad.encounterdetectionpausedbackgroundtick + aad.runningnormallybackgroundtick = aad.totalBackgroundTasks
                AND aad.totalAlarmManagerBackgroundTasks > 0
                THEN 'Partly Contact Traceable Due to Pause(Scenario 16.1)'       			
              
            /* ANDROID PARTLY CONTACT TRACEABLE DUE TO BOTH SCENARIO 16.2 */
                                 
            /* Encounter detection is paused at least once in the day, but not every time a background task is run. runningnormallybackgroundtick runs at least once in the day, meaning the app is running normally for part of the day. encounterdetectionpausedbackgroundtick + runningnormallybackgroundtick is less than totalBackgroundTasks, meaning Bluetooth may be disabled for part of the day. Alarm Manager ran, so a packet was sent via Alarm Manager. */
                WHEN aad.latestApplicationVersion NOT IN ('3.0','3.1','3.10','3.11','3.12','3.2','3.3','3.4','3.5','3.6','3.7','3.8','3.9','4.0','4.1','4.2','4.3','4.4','4.5','4.6','4.7','4.8','4.9','29','30','32','35','36','39','41','42','3.0.1','3.1.1','3.1.2','3.10.1','3.10.2','3.12.1','3.12.2','3.3-internal','3.4-internal','3.5-internal','3.6.1','3.6.2','3.6.2-internal','3.6-internal','3.7.1','3.7.2','3.7-internal','3.9-internal','4.0.1','4.1.1','4.2.1','4.6.1','4.7.1')
                AND aad.encounterdetectionpausedbackgroundtick > 0
                AND aad.runningnormallybackgroundtick > 0
                AND aad.encounterdetectionpausedbackgroundtick + aad.runningnormallybackgroundtick < aad.totalBackgroundTasks
                AND aad.totalAlarmManagerBackgroundTasks > 0
                THEN 'Partly Contact Traceable Due to Both(Scenario 16.2)'
            				
              
            /* ANDROID PARTLY CONTACT TRACEABLE DUE TO PAUSE OR DISABLED SCENARIO 16.3 */
                                 
            /* Encounter detection is paused at least once in the day, but not every time a background task is run. runningnormallybackgroundtick = totalBackgroundTasks (so every time the app has run in the background (totalBackgroundTasks), the app has been fully functional (runningnormallybackgroundtick). encounterdetectionpausedbackgroundtick + runningnormallybackgroundtick is less than totalBackgroundTasks, meaning Bluetooth may be disabled for part of the day. The background task has run at least once (so it is definitely working). */
                WHEN aad.latestApplicationVersion IN ('4.2.1','4.2','4.1.1','4.1','4.0.1','4.0','3.12.2','3.12.1','3.12','3.10','3.9','3.7.2','3.7.1','3.7','3.6.2','3.6.1','3.6','35','41')
                AND aad.encounterdetectionpausedbackgroundtick > 0
                AND aad.encounterdetectionpausedbackgroundtick < aad.totalBackgroundTasks
                AND aad.runningnormallybackgroundtick = aad.totalBackgroundTasks
                AND aad.totalBackgroundTasks > 0 
                THEN 'Partly Contact Traceable Due to Pause or Disabled(Scenario 16.3)'
            				
              
            /* ANDROID UNEXPECTED OTHER SCENARIOS (FOR PRE-v4.3) */
                                 
            /* runningnormallybackgroundtick is not equal to totalBackgroundTasks (unexpected behaviour in the below app versions). */
                WHEN aad.latestApplicationVersion IN ('4.2.1','4.2','4.1.1','4.1','4.0.1','4.0','3.12.2','3.12.1','3.12','3.10','3.9','3.7.2','3.7.1','3.7','3.6.2','3.6.1','3.6','35','41')
                AND aad.runningnormallybackgroundtick != aad.totalBackgroundTasks
                THEN 'Unexpected Scenario'
            
                ELSE 'Other'
            END

            /* iOS appstate scenarios */
              
            WHEN Upper(devicemodel) LIKE '%IPHONE%'  THEN
                       	

            /* iOS NON-CONTACT TRACEABLE SCENARIO 1 */
                                 
            /* No background tasks are running (eg. due to not being fully onboarded, exposure notifications being turned off in the settings, bluetooth being switched off, network connectivity unavailable, etc). */

            CASE
                WHEN aad.encounterdetectionpausedbackgroundtick = 0
                AND aad.runningnormallybackgroundtick = 0 
                AND aad.totalBackgroundTasks = 0
                THEN 'Not Contact Traceable (Scenario 1)'
            			

            /* iOS NON-CONTACT TRACEABLE SCENARIO 3 */
                    
            /* Encounter detection is not paused but the app is not running normally (eg. due to not being fully onboarded, exposure notifications being turned off in the settings, bluetooth being switched off, network connectivity unavailable, etc). The background task has run at least once (so it is definitely working). */
                WHEN aad.encounterdetectionpausedbackgroundtick = 0
                AND aad.runningnormallybackgroundtick = 0 
                AND aad.totalBackgroundTasks > 0
                THEN 'Not Contact Traceable (Scenario 3)'           			
              
            /* iOS FULLY CONTACT TRACEABLE SCENARIO 7.1 */
                    
            /* Encounter detection is not paused. runningnormallybackgroundtick = totalBackgroundTasks (so every time the app has run in the background (totalBackgroundTasks), the app has been fully functional (runningnormallybackgroundtick). The background task has run at least once (so it is definitely working). */
                WHEN aad.encounterdetectionpausedbackgroundtick = 0
                AND aad.runningnormallybackgroundtick = aad.totalBackgroundTasks
                AND aad.totalBackgroundTasks > 0
                THEN 'Fully Contact Traceable (Scenario 7.1)'
              
            /* iOS PARTLY CONTACT TRACEABLE SCENARIO 7.2*/
                    
            /* Encounter detection is not paused. runningnormallybackgroundtick is less than totalBackgroundTasks (so there have been occasions during the day where the app has not been fully functional) - but runningnormallybackgroundtick is greater than 0 - so the app has been fully functional for part of the day. The background task has run at least once (so it is definitely working). */
                WHEN aad.encounterdetectionpausedbackgroundtick = 0
                AND aad.runningnormallybackgroundtick < aad.totalBackgroundTasks
                AND aad.runningnormallybackgroundtick > 0
                AND aad.totalBackgroundTasks > 0
                THEN 'Partly Contact Traceable Due to Disabled(Scenario 7.2)'	
              
            /* iOS NON-CONTACT TRACEABLE SCENARIO 11.1 */
                    
             /* Encounter detection is showing as paused every time a background task is run. runningnormallybackgroundtick = 0, indicating the app is not running normally (eg. due to not being fully onboarded, exposure notifications being turned off in the settings, bluetooth being switched off, network connectivity unavailable, etc). The background task has run at least once (so it is definitely working). */
                WHEN aad.encounterdetectionpausedbackgroundtick = aad.totalBackgroundTasks
                AND aad.runningnormallybackgroundtick = 0
                AND aad.totalBackgroundTasks > 0
                THEN 'Not Contact Traceable (Scenario 11.1)'		
              
            /* iOS NON-CONTACT TRACEABLE SCENARIO 11.2 */
                    
            /* Encounter detection is paused at least once in the day, but not every time a background task is run. runningnormallybackgroundtick = 0, indicating the app is not running normally (eg. due to not being fully onboarded, exposure notifications being turned off in the settings, bluetooth being switched off, network connectivity unavailable, etc). The background task has run at least once (so it is definitely working). */
                WHEN aad.encounterdetectionpausedbackgroundtick > 0
                AND aad.encounterdetectionpausedbackgroundtick < aad.totalBackgroundTasks
                AND aad.runningnormallybackgroundtick = 0
                AND aad.totalBackgroundTasks > 0
                THEN 'Not Contact Traceable (Scenario 11.2)'           			
              
            /* iOS PARTLY CONTACT TRACEABLE DUE TO PAUSE SCENARIO 18.1 */
                    
            /* encounterdetectionpausedbackgroundtick = totalBackgroundTasks, meaning encounter detection is paused all day. runningnormallybackgroundtick = totalBackgroundTasks (so every time the app has run in the background (totalBackgroundTasks), the app has been fully functional (runningnormallybackgroundtick). The background task has run at least once (so it is definitely working). */
                WHEN aad.encounterdetectionpausedbackgroundtick = aad.totalBackgroundTasks
                AND aad.runningnormallybackgroundtick = aad.totalBackgroundTasks
                AND aad.totalBackgroundTasks > 0 
                THEN 'Not Contact Traceable(Scenario 18.1)'            				
              
            /* iOS PARTLY CONTACT TRACEABLE DUE TO PAUSE SCENARIO 18.2 */
                    
            /* encounterdetectionpausedbackgroundtick = totalBackgroundTasks, meaning encounter detection is paused all day. runningnormallybackgroundtick is less than totalBackgroundTasks (so there have been occasions during the day where the app has not been fully functional) - but runningnormallybackgroundtick is greater than 0 - so the app has been fully functional for part of the day. The background task has run at least once (so it is definitely working). */
                WHEN aad.encounterdetectionpausedbackgroundtick = aad.totalBackgroundTasks
                AND aad.runningnormallybackgroundtick < aad.totalBackgroundTasks
                AND aad.runningnormallybackgroundtick > 0
                AND aad.totalBackgroundTasks > 0 
                THEN 'Not Contact Traceable(Scenario 18.2)'

            /* iOS PARTLY CONTACT TRACEABLE DUE TO PAUSE SCENARIO 19.1 */
                    
            /* Encounter detection is paused at least once in the day, but not every time a background task is run. runningnormallybackgroundtick = totalBackgroundTasks (so every time the app has run in the background (totalBackgroundTasks), the app has been fully functional (runningnormallybackgroundtick). The background task has run at least once (so it is definitely working). */
                WHEN aad.encounterdetectionpausedbackgroundtick > 0
                AND aad.encounterdetectionpausedbackgroundtick < aad.totalBackgroundTasks
                AND aad.runningnormallybackgroundtick = aad.totalBackgroundTasks
                AND aad.totalBackgroundTasks > 0 
                THEN 'Partly Contact Traceable Due to Pause(Scenario 19.1)'         			
              
            /* iOS PARTLY CONTACT TRACEABLE DUE TO PAUSE SCENARIO 19.2 */
                    
            /* Encounter detection is paused at least once in the day, but not every time a background task is run. runningnormallybackgroundtick is less than totalBackgroundTasks (so there have been occasions during the day where the app has not been fully functional) - but runningnormallybackgroundtick is greater than 0 - so the app has been fully functional for part of the day. The background task has run at least once (so it is definitely working). */
                WHEN aad.encounterdetectionpausedbackgroundtick > 0
                AND aad.encounterdetectionpausedbackgroundtick < aad.totalBackgroundTasks
                AND aad.runningnormallybackgroundtick < aad.totalBackgroundTasks
                AND aad.runningnormallybackgroundtick > 0
                AND aad.totalBackgroundTasks > 0 
                THEN 'Partly Contact Traceable Due to Pause and Disabled(Scenario 19.2)'
            
                ELSE 'Other'
            END

            END AS appstate FROM "${workspace}_analytics_db"."${workspace}_${mobileAnalyticsTable}" as aad 
                LEFT JOIN "${workspace}_analytics_db"."${workspace}_analytics_postcode_demographic_geographic_lookup" AS pdgl
                    ON (aad.localauthority <> ''
                        AND aad.postaldistrict = pdgl.postcode
                        AND aad.localauthority = pdgl.lad20cd
                        AND (pdgl.country NOT IN ('Scotland', 'Northern Ireland')
                        OR pdgl.country IS NULL))
                LEFT JOIN "${workspace}_analytics_db"."${workspace}_analytics_postcode_demographic_geographic_lookup" AS pdgl2
                    ON ((aad.localauthority = ''
                        OR aad.localauthority IS NULL)
                        AND aad.postaldistrict = pdgl2.postcode
                        AND pdgl2.lad20cd = ''
                        AND (pdgl2.country NOT IN ('Scotland', 'Northern Ireland')
                        OR pdgl2.country IS NULL)) 
                        
              WHERE
               aad.latestApplicationVersion NOT LIKE '%-internal%'
			AND aad.latestApplicationVersion NOT IN ('30', '32', '36', '39', '42', '29', '3.11', '3.2', '3.3', '3.4', '3.5', '3.8','4.11', '4.8','4.15','4.20') 
                AND  date_parse(substring(aad.startdate, 1, 10), '%Y-%c-%d') <= date_parse(substring(aad.submitteddatehour,1,13), '%Y/%c/%d/%H') 
                AND date_parse(substring(aad.startdate,1,10), '%Y-%c-%d') < current_date
                AND aad.startdate <> aad.enddate
            AND date_parse(substring(aad.startdate,1,10), '%Y-%c-%d') >= date('2020-09-24')
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
                        END)))
                                
                    GROUP BY 		
                    truncatedStartDate,
                    local_authority,
                    country) 
                    WHERE country IS NOT NULL AND local_authority IS NOT NULL AND local_authority NOT IN ('Dumfries and Galloway','Scottish Borders') 
                    
                    GROUP BY firstDayReportingWeek,
                        lastDayReportingWeek,
                        local_authority,
                        country
                    ORDER BY
                        lastDayReportingWeek,
                        local_authority
            )


            /* Output */
            SELECT
            m."Week starting (Wythnos yn dechrau)",
            m."Week ending (Wythnos yn gorffen)",
            m."Country (Wlad)",
            m."Local authority (Awdurdod lleol)",
            m."Average Daily Number of Users With App Installed (Cyfartaledd Nifer o Ddefnyddwyr Dyddiol gyda'r Ap wedi ei Osod)",
            m."Average Daily Number of Users Where App is Contact Traceable (Cyfartaledd Nifer o Ddefnyddwyr Dyddiol ble mae'r Ap yn Caniatáu Olrhain Cyswllt)",
            CAST(m."Average Daily Number of Users With App Installed (Cyfartaledd Nifer o Ddefnyddwyr Dyddiol gyda'r Ap wedi ei Osod)" AS DECIMAL (10,3)) / CAST(p."population" AS DECIMAL (10,3)) AS "Average Daily Number of Users With App Installed as Percentage of Population (Cyfartaledd Nifer o Ddefnyddwyr Dyddiol gyda'r Ap wedi ei Osod fel Canran o'r Boblogaeth)",
            CAST(m."Average Daily Number of Users Where App is Contact Traceable (Cyfartaledd Nifer o Ddefnyddwyr Dyddiol ble mae'r Ap yn Caniatáu Olrhain Cyswllt)" AS DECIMAL (10,3)) / CAST(p."population" AS DECIMAL (10,3)) AS "Average Daily Number of Users Where App is Contact Traceable as Percentage of Population (Cyfartaledd Nifer o Ddefnyddwyr Dyddiol ble mae'r Ap yn Caniatáu Olrhain Cyswllt fel Canran o'r Boblogaeth)"

            FROM
            main m

            LEFT JOIN
            population p
            ON
            m."Country (Wlad)" = p."Country (Wlad)"
            AND
            m."Local authority (Awdurdod lleol)" = p."Local authority (Awdurdod lleol)"
        """
    )

    override fun startAppUsageDataByCountryDatasetQueryAsync(): QueryId = asyncDbClient.submitQuery(
        """
WITH

population

AS

(
SELECT
CASE 
WHEN country = 'England' THEN 'England / Lloegr' 
WHEN country = 'Wales' THEN 'Wales / Cymru'
ELSE NULL
END AS "Country (Wlad)",
SUM (local_authority_population) AS population

FROM "${workspace}_analytics_db"."${workspace}_analytics_Local_Authorities_demographic_geographic_lookup"

GROUP BY
CASE 
WHEN country = 'England' THEN 'England / Lloegr' 
WHEN country = 'Wales' THEN 'Wales / Cymru'
ELSE NULL
END
)
,


main

AS

(
SELECT 
DATE_FORMAT(firstDayReportingWeek, '%Y-%m-%d') AS "Week starting (Wythnos yn dechrau)",
            DATE_FORMAT(lastDayReportingWeek, '%Y-%m-%d') AS "Week ending (Wythnos yn gorffen)",
            CASE country 
			WHEN 'England' THEN 'England / Lloegr' 
			WHEN 'Wales' THEN 'Wales / Cymru'
			END AS "Country (Wlad)",
            
            /* Small number suppression */
            CASE
            WHEN CAST(ROUND(AVG(NumberOfUsersAppInstalledInd)) AS BIGINT) <5
            AND CAST(ROUND(AVG(NumberOfUsersAppInstalledInd)) AS BIGINT) >0
            THEN 5
            ELSE CAST(ROUND(AVG(NumberOfUsersAppInstalledInd)) AS BIGINT)
            END AS "Average Daily Number of Users With App Installed (Cyfartaledd Nifer o Ddefnyddwyr Dyddiol gyda'r Ap wedi ei Osod)",	        
            
            CASE
            WHEN CAST(ROUND(AVG(NumberOfUsersAppIsContactTraceable)) AS BIGINT) <5
            AND CAST(ROUND(AVG(NumberOfUsersAppIsContactTraceable)) AS BIGINT) >0
            THEN 5
            ELSE CAST(ROUND(AVG(NumberOfUsersAppIsContactTraceable)) AS BIGINT)
            END AS "Average Daily Number of Users Where App is Contact Traceable (Cyfartaledd Nifer o Ddefnyddwyr Dyddiol ble mae'r Ap yn Caniatáu Olrhain Cyswllt)"	


FROM(SELECT
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
		
		truncatedStartDate,
		country,

        SUM(AppInstalled) AS NumberOfUsersAppInstalledInd,
        SUM(AppIsContactTraceable) AS NumberOfUsersAppIsContactTraceable

FROM (SELECT
		truncatedStartDate,
		local_authority,
		country,
		
		1 AS AppInstalled, 

		CASE WHEN appstate in (
					'Fully Contact Traceable (Scenario 7.1)',
					'Partly Contact Traceable Due to Disabled(Scenario 7.2)',
					'Partly Contact Traceable Due to Pause or Disabled(Scenario 16.3)',
					'Partly Contact Traceable Due to Pause(Scenario 19.1)',
					'Partly Contact Traceable Due to Pause and Disabled(Scenario 19.2)',
					'Partly Contact Traceable Due to Disabled(Scenario 8.2)',
					'Fully Contact Traceable (Scenario 8.1)',
					'Partly Contact Traceable Due to Pause(Scenario 15.1)',
					'Partly Contact Traceable Due to Both(Scenario 16.2)',
					'Partly Contact Traceable Due to Pause(Scenario 16.1)',
					'Partly Contact Traceable Due to Both(Scenario 15.2)',
					'App Fully Usable and Contact Tracing On',
					'App Partially Usable and Contact Tracing On',
					'App Fully Useable and Contact Tracing Paused',
					'App Partially Usable and Contact Tracing Paused')  THEN 1 ELSE 0 END AS AppIsContactTraceable 

					FROM(

SELECT date_parse(substring(aad.startdate,
         1,
         10),
         '%Y-%c-%d') AS truncatedstartdate,  
		 
		 /*This section contains geographic information about the packet at district and LA level*/
          COALESCE(pdgl.local_authority, pdgl2.local_authority) AS local_authority,
		  COALESCE(pdgl.country, pdgl2.country) as country,


 /* New appstate field scenarios (v4.17+) */
  
CASE
    WHEN aad.latestApplicationVersion NOT IN ('4.5','3.0.1','3.8','4.1.1','3.0','4.9','3.6.2-internal','4.2.1','39','3.10',
	'4.13.1','29','4.0','4.6.1','4.2','3.10.2','3.10.1','4.16','3.6-internal','3.1.2','4.6','4.1','32','42','4.15','3.12.2','3.7.1','3.2','4.13','4.3','3.5',
	'3.5-internal','3.3-internal','3.9','3.3','3.7','3.12','30','3.11','4.7','35','3.12.1','4.12','3.7-internal',
	'4.11','4.14.1','3.4','4.4','3.7.2','3.1','41','3.9-internal','3.6.1','3.1.1','4.7.1','4.18','4.14','3.4-internal','36','3.6.2','4.0.1','3.6','4.10','4.8') THEN
 
CASE
   
/* ANDROID FULLY USABLE AND CONTACT TRACING ON v4.17+ */
  
/* The appisusablebackgroundtick ran every time a background task ran. The appiscontacttraceablebackgroundtick ran every time the appisusablebackgroundtick ran. The background task has run at least once (so it is definitely working). */
    WHEN aad.appisusablebackgroundtick = aad.totalBackgroundTasks 
	AND aad.appiscontacttraceablebackgroundtick = aad.appisusablebackgroundtick
	AND aad.totalBackgroundTasks > 0
	THEN 'App Fully Usable and Contact Tracing On'
  
  
/* ANDROID FULLY USABLE AND CONTACT TRACING OFF v4.17+ */
  
/* The appisusablebackgroundtick ran every time a background task ran. The background task has run at least once (so it is definitely working). The appiscontacttraceablebackgroundtick did not run at all, so contact tracing is assumed to be off. */	    
    WHEN aad.appisusablebackgroundtick = aad.totalBackgroundTasks 
	AND aad.totalBackgroundTasks > 0
	AND aad.appiscontacttraceablebackgroundtick = 0
	THEN 'App Fully Usable and Contact Tracing Off'
  
  
/* ANDROID FULLY USABLE AND CONTACT TRACING PAUSED v4.17+ */
  
/* The appisusablebackgroundtick ran every time a background task ran. The background task has run at least once (so it is definitely working). The appiscontacttraceablebackgroundtick did run at some stage, but not every time the appisusablebackgroundtick happened - so contact tracing was on for some of the time. */	
	WHEN aad.appisusablebackgroundtick = aad.totalBackgroundTasks 
	AND aad.totalBackgroundTasks > 0
	AND aad.appiscontacttraceablebackgroundtick > 0
	AND aad.appiscontacttraceablebackgroundtick < aad.appisusablebackgroundtick
	THEN 'App Fully Useable and Contact Tracing Paused'
  
  
/* ANDROID NOT USABLE v4.17+ */
  
/* The appisusablebackgroundtick did not run. The background task has run at least once (so it is definitely working) The app is therefore assumed to be unusable. */			    
    WHEN aad.appisusablebackgroundtick = 0
	AND aad.totalBackgroundTasks > 0
	THEN 'App Not Usable'
			    

/* ANDROID PARTIALLY USABLE AND CONTACT TRACING ON v4.17+ */
  
/* The appisusablebackgroundtick ran, but not every time a background task ran - so the app was not usable at all times during the day. The background task has run at least once (so it is definitely working). The appiscontacttraceablebackgroundtick ran every time the appisusablebackgroundtick ran. */
    WHEN aad.appisusablebackgroundtick < aad.totalBackgroundTasks 
	AND aad.appisusablebackgroundtick > 0
	AND aad.totalBackgroundTasks > 0
	AND aad.appiscontacttraceablebackgroundtick = aad.appisusablebackgroundtick
	THEN 'App Partially Usable and Contact Tracing On'
			    

/* ANDROID PARTIALLY USABLE AND CONTACT TRACING PAUSED v4.17+ */
  
/* The appisusablebackgroundtick ran, but not every time a background task ran - so the app was not usable at all times during the day. The background task has run at least once (so it is definitely working). The appiscontacttraceablebackgroundtick did run at some stage, but not every time the appisusablebackgroundtick happened - so contact tracing was on for some of the time. */
    WHEN aad.appisusablebackgroundtick < aad.totalBackgroundTasks 
	AND aad.appisusablebackgroundtick > 0
	AND aad.totalBackgroundTasks > 0
	AND  aad.appiscontacttraceablebackgroundtick < aad.appisusablebackgroundtick
	AND  aad.appiscontacttraceablebackgroundtick> 0    
    THEN 'App Partially Usable and Contact Tracing Paused' 
			    

/* ANDROID PARTIALLY USABLE AND CONTACT TRACING OFF v4.17+ */
  
/* The appisusablebackgroundtick ran, but not every time a background task ran - so the app was not usable at all times during the day. The background task has run at least once (so it is definitely working). The appiscontacttraceablebackgroundtick did not run at all, so contact tracing is assumed to be off. */
    WHEN aad.appisusablebackgroundtick < aad.totalBackgroundTasks 
	AND aad.appisusablebackgroundtick > 0
	AND aad.totalBackgroundTasks > 0
	AND  aad.appiscontacttraceablebackgroundtick = 0
    THEN 'App Partially Usable and Contact Tracing Off'
			    

/* ANDROID BACKGROUND TASKS NOT RUNNING v4.17+ */
  
/* No background tasks running */
    WHEN  aad.totalBackgroundTasks = 0
    THEN 'Background Tasks Not Running'
	  
    ELSE 'Other'
END
  

/* Android appstate scenarios */      
  
  WHEN Upper(devicemodel) NOT LIKE '%IPHONE%' THEN
  
CASE 
  

/* ANDROID NON-CONTACT TRACEABLE SCENARIO 1 */
  
/* No background tasks are running (eg. due to not being fully onboarded, exposure notifications being turned off in the settings, bluetooth being switched off, network connectivity unavailable, etc). Alarm Manager did not run, so a packet was not sent via Alarm Manager. */
    WHEN (aad.latestApplicationVersion NOT IN ('3.0','3.1','3.10','3.11','3.12','3.2','3.3','3.4','3.5','3.6','3.7','3.8','3.9','4.0','4.1','4.2','4.3','4.4','4.5','4.6','4.7','4.8','4.9','29','30','32','35','36','39','41','42','3.0.1','3.1.1','3.1.2','3.10.1','3.10.2','3.12.1','3.12.2','3.3-internal','3.4-internal','3.5-internal','3.6.1','3.6.2','3.6.2-internal','3.6-internal','3.7.1','3.7.2','3.7-internal','3.9-internal','4.0.1','4.1.1','4.2.1','4.6.1','4.7.1')
	AND aad.encounterdetectionpausedbackgroundtick = 0
	AND aad.runningnormallybackgroundtick = 0 
	AND aad.totalBackgroundTasks = 0
	AND aad.totalAlarmManagerBackgroundTasks = 0)
	
    OR
  
/* No background tasks are running (eg. due to not being fully onboarded, exposure notifications being turned off in the settings, bluetooth being switched off, network connectivity unavailable, etc). */
		(aad.latestApplicationVersion IN ('4.3','4.4','4.5','4.6','4.6.1','4.7','4.7.1','4.9','4.8')
	AND aad.encounterdetectionpausedbackgroundtick = 0
	AND aad.runningnormallybackgroundtick = 0 
	AND aad.totalBackgroundTasks = 0)
				
    OR
  
/* No background tasks are running (eg. due to not being fully onboarded, exposure notifications being turned off in the settings, bluetooth being switched off, network connectivity unavailable, etc). */
		(aad.latestApplicationVersion IN ('4.2.1','4.2','4.1.1','4.1','4.0.1','4.0','3.12.2','3.12.1','3.12','3.10','3.9','3.7.2','3.7.1','3.7','3.6.2','3.6.1','3.6','35','41')
	AND aad.encounterdetectionpausedbackgroundtick = 0
	AND aad.runningnormallybackgroundtick = 0 
	AND aad.totalBackgroundTasks = 0)
	THEN 'Not Contact Traceable (Scenario 1)'
				

/* ANDROID NON-CONTACT TRACEABLE SCENARIO 2 */
  
/* No background tasks are running (eg. due to not being fully onboarded, exposure notifications being turned off in the settings, bluetooth being switched off, network connectivity unavailable, etc). But Alarm Manager ran, so a packet was sent via Alarm Manager. */
	WHEN aad.latestApplicationVersion NOT IN ('3.0','3.1','3.10','3.11','3.12','3.2','3.3','3.4','3.5','3.6','3.7','3.8','3.9','4.0','4.1','4.2','4.3','4.4','4.5','4.6','4.7','4.8','4.9','29','30','32','35','36','39','41','42','3.0.1','3.1.1','3.1.2','3.10.1','3.10.2','3.12.1','3.12.2','3.3-internal','3.4-internal','3.5-internal','3.6.1','3.6.2','3.6.2-internal','3.6-internal','3.7.1','3.7.2','3.7-internal','3.9-internal','4.0.1','4.1.1','4.2.1','4.6.1','4.7.1')
	AND aad.encounterdetectionpausedbackgroundtick = 0
	AND aad.runningnormallybackgroundtick = 0 
	AND aad.totalBackgroundTasks = 0
	AND aad.totalAlarmManagerBackgroundTasks > 0
	THEN 'Not Contact Traceable (Scenario 2)'
			

/* ANDROID NON-CONTACT TRACEABLE SCENARIO 3 */
  
/* Encounter detection is not paused but the app is not running normally (eg. due to not being fully onboarded, exposure notifications being turned off in the settings, bluetooth being switched off, network connectivity unavailable, etc). The background task has run at least once (so it is definitely working). Alarm Manager did not run, so a packet was not sent via Alarm Manager. */
	WHEN (aad.latestApplicationVersion NOT IN ('3.0','3.1','3.10','3.11','3.12','3.2','3.3','3.4','3.5','3.6','3.7','3.8','3.9','4.0','4.1','4.2','4.3','4.4','4.5','4.6','4.7','4.8','4.9','29','30','32','35','36','39','41','42','3.0.1','3.1.1','3.1.2','3.10.1','3.10.2','3.12.1','3.12.2','3.3-internal','3.4-internal','3.5-internal','3.6.1','3.6.2','3.6.2-internal','3.6-internal','3.7.1','3.7.2','3.7-internal','3.9-internal','4.0.1','4.1.1','4.2.1','4.6.1','4.7.1')
	AND aad.encounterdetectionpausedbackgroundtick = 0
	AND aad.runningnormallybackgroundtick = 0 
	AND aad.totalBackgroundTasks > 0
	AND aad.totalAlarmManagerBackgroundTasks = 0)
	
    OR
  
/* Encounter detection is not paused but the app is not running normally (eg. due to not being fully onboarded, exposure notifications being turned off in the settings, bluetooth being switched off, network connectivity unavailable, etc). The background task has run at least once (so it is definitely working). */
		(aad.latestApplicationVersion IN ('4.3','4.4','4.5','4.6','4.6.1','4.7','4.7.1','4.9','4.8')
	AND aad.encounterdetectionpausedbackgroundtick = 0
	AND aad.runningnormallybackgroundtick = 0 
	AND aad.totalBackgroundTasks > 0)
	THEN 'Not Contact Traceable (Scenario 3)'
			

/* ANDROID NON-CONTACT TRACEABLE SCENARIO 4 */

/* Encounter detection is not paused but the app is not running normally (eg. due to not being fully onboarded, exposure notifications being turned off in the settings, bluetooth being switched off, network connectivity unavailable, etc). runningNormallyBackgroundTick + encounterDetectionPausedBackgroundTick will normally equal totalBackgroundTasks. In this case it does not, suggesting EN is enabled but Bluetooth is disabled. runningnormallybackgroundtick will not tick up as Bluetooth is off, but a background task has still occurred as EN is on. Alarm Manager ran, so a packet was sent via Alarm Manager. */
	WHEN aad.latestApplicationVersion NOT IN ('3.0','3.1','3.10','3.11','3.12','3.2','3.3','3.4','3.5','3.6','3.7','3.8','3.9','4.0','4.1','4.2','4.3','4.4','4.5','4.6','4.7','4.8','4.9','29','30','32','35','36','39','41','42','3.0.1','3.1.1','3.1.2','3.10.1','3.10.2','3.12.1','3.12.2','3.3-internal','3.4-internal','3.5-internal','3.6.1','3.6.2','3.6.2-internal','3.6-internal','3.7.1','3.7.2','3.7-internal','3.9-internal','4.0.1','4.1.1','4.2.1','4.6.1','4.7.1')
	AND aad.encounterdetectionpausedbackgroundtick = 0
	AND aad.runningnormallybackgroundtick = 0 
	AND aad.totalBackgroundTasks > 0
	AND aad.totalAlarmManagerBackgroundTasks > 0
	THEN 'Not Contact Traceable (Scenario 4)'
			

/* ANDROID FULLY CONTACT TRACEABLE SCENARIO 7.1 */
  
/* Encounter detection is not paused. runningnormallybackgroundtick = totalBackgroundTasks (so every time the app has run in the background (totalBackgroundTasks), the app has been fully functional (runningnormallybackgroundtick). The background task has run at least once (so it is definitely working). Alarm Manager did not run, so a packet was not sent via Alarm Manager. */
	WHEN (aad.latestApplicationVersion NOT IN ('3.0','3.1','3.10','3.11','3.12','3.2','3.3','3.4','3.5','3.6','3.7','3.8','3.9','4.0','4.1','4.2','4.3','4.4','4.5','4.6','4.7','4.8','4.9','29','30','32','35','36','39','41','42','3.0.1','3.1.1','3.1.2','3.10.1','3.10.2','3.12.1','3.12.2','3.3-internal','3.4-internal','3.5-internal','3.6.1','3.6.2','3.6.2-internal','3.6-internal','3.7.1','3.7.2','3.7-internal','3.9-internal','4.0.1','4.1.1','4.2.1','4.6.1','4.7.1')
	AND aad.encounterdetectionpausedbackgroundtick = 0
	AND aad.runningnormallybackgroundtick = aad.totalBackgroundTasks
	AND aad.totalBackgroundTasks > 0
	AND aad.totalAlarmManagerBackgroundTasks = 0)
	
    OR
  
/* Encounter detection is not paused. runningnormallybackgroundtick = totalBackgroundTasks (so every time the app has run in the background (totalBackgroundTasks), the app has been fully functional (runningnormallybackgroundtick). The background task has run at least once (so it is definitely working). */
		(aad.latestApplicationVersion IN ('4.3','4.4','4.5','4.6','4.6.1','4.7','4.7.1','4.9','4.8')
	AND aad.encounterdetectionpausedbackgroundtick = 0
	AND aad.runningnormallybackgroundtick = aad.totalBackgroundTasks
	AND aad.totalBackgroundTasks >0)
	
    OR
  
  /* Encounter detection is not paused. runningnormallybackgroundtick = totalBackgroundTasks (so every time the app has run in the background (totalBackgroundTasks), the app has been fully functional (runningnormallybackgroundtick). And the background task has run at least once (so it is definitely working). */
		(aad.latestApplicationVersion IN ('4.2.1','4.2','4.1.1','4.1','4.0.1','4.0','3.12.2','3.12.1','3.12','3.10','3.9','3.7.2','3.7.1','3.7','3.6.2','3.6.1','3.6','35','41')
	AND aad.encounterdetectionpausedbackgroundtick = 0
	AND aad.runningnormallybackgroundtick = aad.totalBackgroundTasks 
	AND aad.totalBackgroundTasks >0)
	THEN 'Fully Contact Traceable (Scenario 7.1)'
			
  
/* ANDROID FULLY CONTACT TRACEABLE SCENARIO 8.1 */
                     
  /* Encounter detection is not paused. runningnormallybackgroundtick = totalBackgroundTasks (so every time the app has run in the background (totalBackgroundTasks), the app has been fully functional (runningnormallybackgroundtick). The background task has run at least once (so it is definitely working). Alarm Manager ran, so a packet was sent via Alarm Manager. */
				WHEN aad.latestApplicationVersion NOT IN ('3.0','3.1','3.10','3.11','3.12','3.2','3.3','3.4','3.5','3.6','3.7','3.8','3.9','4.0','4.1','4.2','4.3','4.4','4.5','4.6','4.7','4.8','4.9','29','30','32','35','36','39','41','42','3.0.1','3.1.1','3.1.2','3.10.1','3.10.2','3.12.1','3.12.2','3.3-internal','3.4-internal','3.5-internal','3.6.1','3.6.2','3.6.2-internal','3.6-internal','3.7.1','3.7.2','3.7-internal','3.9-internal','4.0.1','4.1.1','4.2.1','4.6.1','4.7.1')
				AND aad.encounterdetectionpausedbackgroundtick = 0
				AND aad.runningnormallybackgroundtick = aad.totalBackgroundTasks
				AND aad.totalBackgroundTasks > 0
				AND aad.totalAlarmManagerBackgroundTasks > 0
				THEN 'Fully Contact Traceable (Scenario 8.1)'
			

/* ANDROID PARTLY CONTACT TRACEABLE SCENARIO 7.2 */
                     
/* Encounter detection is not paused. runningnormallybackgroundtick is less than totalBackgroundTasks (so there have been occasions during the day where the app has not been fully functional) - but runningnormallybackgroundtick is greater than 0 - so the app has been fully functional for part of the day. The background task has run at least once (so it is definitely working). Alarm Manager did not run, so a packet was not sent via Alarm Manager. */
				WHEN (aad.latestApplicationVersion NOT IN ('3.0','3.1','3.10','3.11','3.12','3.2','3.3','3.4','3.5','3.6','3.7','3.8','3.9','4.0','4.1','4.2','4.3','4.4','4.5','4.6','4.7','4.8','4.9','29','30','32','35','36','39','41','42','3.0.1','3.1.1','3.1.2','3.10.1','3.10.2','3.12.1','3.12.2','3.3-internal','3.4-internal','3.5-internal','3.6.1','3.6.2','3.6.2-internal','3.6-internal','3.7.1','3.7.2','3.7-internal','3.9-internal','4.0.1','4.1.1','4.2.1','4.6.1','4.7.1')
				AND aad.encounterdetectionpausedbackgroundtick = 0
				AND aad.runningnormallybackgroundtick < aad.totalBackgroundTasks
				AND aad.runningnormallybackgroundtick > 0
				AND aad.totalBackgroundTasks > 0
				AND aad.totalAlarmManagerBackgroundTasks = 0)
				
                OR
  
/* Encounter detection is not paused. runningnormallybackgroundtick is less than totalBackgroundTasks (so there have been occasions during the day where the app has not been fully functional) - but runningnormallybackgroundtick is greater than 0 - so the app has been fully functional for part of the day. The background task has run at least once (so it is definitely working). */
					(aad.latestApplicationVersion IN ('4.3','4.4','4.5','4.6','4.6.1','4.7','4.7.1','4.9','4.8')
				AND aad.encounterdetectionpausedbackgroundtick = 0
				AND aad.runningnormallybackgroundtick < aad.totalBackgroundTasks
				AND aad.runningnormallybackgroundtick > 0
				AND aad.totalBackgroundTasks > 0)
				THEN 'Partly Contact Traceable Due to Disabled(Scenario 7.2)'
			
  
/* ANDROID PARTLY CONTACT TRACEABLE SCENARIO 8.2 */
                     
/* Encounter detection is not paused. runningnormallybackgroundtick is less than totalBackgroundTasks (so there have been occasions during the day where the app has not been fully functional) - but runningnormallybackgroundtick is greater than 0 - so the app has been fully functional for part of the day. The background task has run at least once (so it definitely working). Alarm Manager ran, so a packet was sent via Alarm Manager. */
				WHEN aad.latestApplicationVersion NOT IN ('3.0','3.1','3.10','3.11','3.12','3.2','3.3','3.4','3.5','3.6','3.7','3.8','3.9','4.0','4.1','4.2','4.3','4.4','4.5','4.6','4.7','4.8','4.9','29','30','32','35','36','39','41','42','3.0.1','3.1.1','3.1.2','3.10.1','3.10.2','3.12.1','3.12.2','3.3-internal','3.4-internal','3.5-internal','3.6.1','3.6.2','3.6.2-internal','3.6-internal','3.7.1','3.7.2','3.7-internal','3.9-internal','4.0.1','4.1.1','4.2.1','4.6.1','4.7.1')
				AND aad.encounterdetectionpausedbackgroundtick = 0
				AND aad.runningnormallybackgroundtick < aad.totalBackgroundTasks
				AND aad.runningnormallybackgroundtick > 0
				AND aad.totalBackgroundTasks > 0
				AND aad.totalAlarmManagerBackgroundTasks > 0
				THEN 'Partly Contact Traceable Due to Disabled(Scenario 8.2)'
			
  
/* ANDROID PARTLY CONTACT TRACEABLE SCENARIO 11.1 */
                     
/* Encounter detection is showing as paused every time a background task is run. runningnormallybackgroundtick = 0, indicating the app is not running normally (eg. due to not being fully onboarded, exposure notifications being turned off in the settings, bluetooth being switched off, network connectivity unavailable, etc). The background task has run at least once (so it is definitely working). Alarm Manager did not run, so a packet was not sent via Alarm Manager. */
				WHEN (aad.latestApplicationVersion NOT IN ('3.0','3.1','3.10','3.11','3.12','3.2','3.3','3.4','3.5','3.6','3.7','3.8','3.9','4.0','4.1','4.2','4.3','4.4','4.5','4.6','4.7','4.8','4.9','29','30','32','35','36','39','41','42','3.0.1','3.1.1','3.1.2','3.10.1','3.10.2','3.12.1','3.12.2','3.3-internal','3.4-internal','3.5-internal','3.6.1','3.6.2','3.6.2-internal','3.6-internal','3.7.1','3.7.2','3.7-internal','3.9-internal','4.0.1','4.1.1','4.2.1','4.6.1','4.7.1')
				AND aad.encounterdetectionpausedbackgroundtick = aad.totalBackgroundTasks
				AND aad.runningnormallybackgroundtick = 0
				AND aad.totalBackgroundTasks > 0
				AND aad.totalAlarmManagerBackgroundTasks = 0)
  
                OR
  
/* Encounter detection is showing as paused every time a background task is run. runningnormallybackgroundtick = 0, indicating the app is not running normally (eg. due to not being fully onboarded, exposure notifications being turned off in the settings, bluetooth being switched off, network connectivity unavailable, etc). The background task has run at least once (so it is definitely working). */
					(aad.latestApplicationVersion IN ('4.3','4.4','4.5','4.6','4.6.1','4.7','4.7.1','4.9','4.8')
				AND aad.encounterdetectionpausedbackgroundtick = aad.totalBackgroundTasks
				AND aad.runningnormallybackgroundtick = 0
				AND aad.totalBackgroundTasks > 0)
				
                OR
  
/* Encounter detection is showing as paused every time a background task is run. runningnormallybackgroundtick = 0, indicating the app is not running normally (eg. due to not being fully onboarded, exposure notifications being turned off in the settings, bluetooth being switched off, network connectivity unavailable, etc). The background task has run at least once (so it is definitely working). */
					(aad.latestApplicationVersion IN ('4.2.1','4.2','4.1.1','4.1','4.0.1','4.0','3.12.2','3.12.1','3.12','3.10','3.9','3.7.2','3.7.1','3.7','3.6.2','3.6.1','3.6','35','41')
				AND aad.encounterdetectionpausedbackgroundtick = aad.totalBackgroundTasks
				AND aad.runningnormallybackgroundtick = aad.totalBackgroundTasks 
				AND aad.totalBackgroundTasks >0)
				THEN 'Not Contact Traceable (Scenario 11.1)'
			
 
/* ANDROID NON-CONTACT TRACEABLE SCENARIO 11.2*/
                     
/* Encounter detection is paused at least once in the day, but not every time a background task is run. runningnormallybackgroundtick = 0, indicating the app is not running normally (eg. due to not being fully onboarded, exposure notifications being turned off in the settings, bluetooth being switched off, network connectivity unavailable, etc). The background task has run at least once (so it is definitely working). Alarm Manager did not run, so a packet was not sent via Alarm Manager. */
				WHEN (aad.latestApplicationVersion NOT IN ('3.0','3.1','3.10','3.11','3.12','3.2','3.3','3.4','3.5','3.6','3.7','3.8','3.9','4.0','4.1','4.2','4.3','4.4','4.5','4.6','4.7','4.8','4.9','29','30','32','35','36','39','41','42','3.0.1','3.1.1','3.1.2','3.10.1','3.10.2','3.12.1','3.12.2','3.3-internal','3.4-internal','3.5-internal','3.6.1','3.6.2','3.6.2-internal','3.6-internal','3.7.1','3.7.2','3.7-internal','3.9-internal','4.0.1','4.1.1','4.2.1','4.6.1','4.7.1')
				AND aad.encounterdetectionpausedbackgroundtick > 0
				AND aad.encounterdetectionpausedbackgroundtick < aad.totalBackgroundTasks
				AND aad.runningnormallybackgroundtick = 0
				AND aad.totalBackgroundTasks > 0
				AND aad.totalAlarmManagerBackgroundTasks = 0)
				
                OR
  
/* Encounter detection is paused at least once in the day, but not every time a background task is run. runningnormallybackgroundtick = 0, indicating the app is not running normally (eg. due to not being fully onboarded, exposure notifications being turned off in the settings, bluetooth being switched off, network connectivity unavailable, etc). The background task has run at least once (so it is definitely working). */
					(aad.latestApplicationVersion IN ('4.3','4.4','4.5','4.6','4.6.1','4.7','4.7.1','4.9','4.8')
				AND aad.encounterdetectionpausedbackgroundtick > 0
				AND aad.encounterdetectionpausedbackgroundtick < aad.totalBackgroundTasks
				AND aad.runningnormallybackgroundtick = 0
				AND aad.totalBackgroundTasks > 0)
				THEN 'Not Contact Traceable (Scenario 11.2)'
			
  
/* ANDROID NON-CONTACT TRACEABLE SCENARIO 12.1 */
                     
/* Encounter detection is showing as paused every time a background task is run. runningnormallybackgroundtick = 0, indicating the app is not running normally (eg. due to not being fully onboarded, exposure notifications being turned off in the settings, bluetooth being switched off, network connectivity unavailable, etc). The background task has run at least once (so it is definitely working). Alarm Manager ran, so a packet was sent via Alarm Manager. */
				WHEN aad.latestApplicationVersion NOT IN ('3.0','3.1','3.10','3.11','3.12','3.2','3.3','3.4','3.5','3.6','3.7','3.8','3.9','4.0','4.1','4.2','4.3','4.4','4.5','4.6','4.7','4.8','4.9','29','30','32','35','36','39','41','42','3.0.1','3.1.1','3.1.2','3.10.1','3.10.2','3.12.1','3.12.2','3.3-internal','3.4-internal','3.5-internal','3.6.1','3.6.2','3.6.2-internal','3.6-internal','3.7.1','3.7.2','3.7-internal','3.9-internal','4.0.1','4.1.1','4.2.1','4.6.1','4.7.1')
				AND aad.encounterdetectionpausedbackgroundtick = aad.totalBackgroundTasks
				AND aad.runningnormallybackgroundtick = 0
				AND aad.totalBackgroundTasks > 0
				AND aad.totalAlarmManagerBackgroundTasks > 0
				THEN 'Not Contact Traceable (Scenario 12.1)'
			
  
/* ANDROID NON-CONTACT TRACEABLE SCENARIO 12.2 */
                     
/* Encounter detection is paused at least once in the day, but not every time a background task is run. runningnormallybackgroundtick = 0, indicating the app is not running normally (eg. due to not being fully onboarded, exposure notifications being turned off in the settings, bluetooth being switched off, network connectivity unavailable, etc). The background task has run at least once (so it is definitely working). Alarm Manager ran, so a packet was sent via Alarm Manager. */
				WHEN aad.latestApplicationVersion NOT IN ('3.0','3.1','3.10','3.11','3.12','3.2','3.3','3.4','3.5','3.6','3.7','3.8','3.9','4.0','4.1','4.2','4.3','4.4','4.5','4.6','4.7','4.8','4.9','29','30','32','35','36','39','41','42','3.0.1','3.1.1','3.1.2','3.10.1','3.10.2','3.12.1','3.12.2','3.3-internal','3.4-internal','3.5-internal','3.6.1','3.6.2','3.6.2-internal','3.6-internal','3.7.1','3.7.2','3.7-internal','3.9-internal','4.0.1','4.1.1','4.2.1','4.6.1','4.7.1')
				AND aad.encounterdetectionpausedbackgroundtick > 0
				AND aad.encounterdetectionpausedbackgroundtick < aad.totalBackgroundTasks
				AND aad.runningnormallybackgroundtick = 0
				AND aad.totalBackgroundTasks > 0
				AND aad.totalAlarmManagerBackgroundTasks > 0
				THEN 'Not Contact Traceable (Scenario 12.2)'
				
  
/* ANDROID PARTLY CONTACT TRACEABLE SCENARIO 15.1 */
                     
/* Encounter detection is paused at least once in the day, but not every time a background task is run. runningnormallybackgroundtick runs at least once in the day, meaning the app is running normally for part of the day. encounterdetectionpausedbackgroundtick + runningnormallybackgroundtick = totalBackgroundTasks, meaning all backround tasks are accounted for (when encounterdetectionpausedbackgroundtick + runningnormallybackgroundtick is less than totalBackgroundTasks, Bluetooth may be disabled). Alarm Manager did not run, so a packet was not sent via Alarm Manager. */
				WHEN (aad.latestApplicationVersion NOT IN ('3.0','3.1','3.10','3.11','3.12','3.2','3.3','3.4','3.5','3.6','3.7','3.8','3.9','4.0','4.1','4.2','4.3','4.4','4.5','4.6','4.7','4.8','4.9','29','30','32','35','36','39','41','42','3.0.1','3.1.1','3.1.2','3.10.1','3.10.2','3.12.1','3.12.2','3.3-internal','3.4-internal','3.5-internal','3.6.1','3.6.2','3.6.2-internal','3.6-internal','3.7.1','3.7.2','3.7-internal','3.9-internal','4.0.1','4.1.1','4.2.1','4.6.1','4.7.1')
				AND aad.encounterdetectionpausedbackgroundtick > 0
				AND aad.runningnormallybackgroundtick > 0
				AND aad.encounterdetectionpausedbackgroundtick + aad.runningnormallybackgroundtick = aad.totalBackgroundTasks
				AND aad.totalAlarmManagerBackgroundTasks = 0)
				
                OR
  
/* Encounter detection is paused at least once in the day, but not every time a background task is run. runningnormallybackgroundtick runs at least once in the day, meaning the app is running normally for part of the day. encounterdetectionpausedbackgroundtick + runningnormallybackgroundtick = totalBackgroundTasks, meaning all backround tasks are accounted for (when encounterdetectionpausedbackgroundtick + runningnormallybackgroundtick is less than totalBackgroundTasks, Bluetooth may be disabled). */
					(aad.latestApplicationVersion IN ('4.3','4.4','4.5','4.6','4.6.1','4.7','4.7.1','4.9','4.8')
				AND aad.encounterdetectionpausedbackgroundtick > 0
				AND aad.runningnormallybackgroundtick > 0
				AND aad.encounterdetectionpausedbackgroundtick + aad.runningnormallybackgroundtick = aad.totalBackgroundTasks)
				THEN 'Partly Contact Traceable Due to Pause(Scenario 15.1)'
			
  
/* ANDROID PARTLY CONTACT TRACEABLE DUE TO BOTH SCENARIO 15.2 */
                     
/* Encounter detection is paused at least once in the day, but not every time a background task is run. runningnormallybackgroundtick runs at least once in the day, meaning the app is running normally for part of the day. encounterdetectionpausedbackgroundtick + runningnormallybackgroundtick is less than totalBackgroundTasks, meaning Bluetooth may be disabled for part of the day. Alarm Manager did not run, so a packet was not sent via Alarm Manager. */
				WHEN (aad.latestApplicationVersion NOT IN ('3.0','3.1','3.10','3.11','3.12','3.2','3.3','3.4','3.5','3.6','3.7','3.8','3.9','4.0','4.1','4.2','4.3','4.4','4.5','4.6','4.7','4.8','4.9','29','30','32','35','36','39','41','42','3.0.1','3.1.1','3.1.2','3.10.1','3.10.2','3.12.1','3.12.2','3.3-internal','3.4-internal','3.5-internal','3.6.1','3.6.2','3.6.2-internal','3.6-internal','3.7.1','3.7.2','3.7-internal','3.9-internal','4.0.1','4.1.1','4.2.1','4.6.1','4.7.1')
				AND aad.encounterdetectionpausedbackgroundtick > 0
				AND aad.runningnormallybackgroundtick > 0
				AND aad.encounterdetectionpausedbackgroundtick + aad.runningnormallybackgroundtick < aad.totalBackgroundTasks
				AND aad.totalAlarmManagerBackgroundTasks = 0)
				
                OR

/* Encounter detection is paused at least once in the day, but not every time a background task is run. runningnormallybackgroundtick runs at least once in the day, meaning the app is running normally for part of the day. encounterdetectionpausedbackgroundtick + runningnormallybackgroundtick is less than totalBackgroundTasks, meaning Bluetooth may be disabled for part of the day. */
					(aad.latestApplicationVersion IN ('4.3','4.4','4.5','4.6','4.6.1','4.7','4.7.1','4.9','4.8')
				AND aad.encounterdetectionpausedbackgroundtick > 0
				AND aad.runningnormallybackgroundtick > 0
				AND aad.encounterdetectionpausedbackgroundtick + aad.runningnormallybackgroundtick < aad.totalBackgroundTasks)
				THEN 'Partly Contact Traceable Due to Both(Scenario 15.2)'
			
  
/* ANDROID PARTLY CONTACT TRACEABLE DUE TO PAUSE SCENARIO 16.1 */
                     
/* Encounter detection is paused at least once in the day, but not every time a background task is run. runningnormallybackgroundtick runs at least once in the day, meaning the app is running normally for part of the day. encounterdetectionpausedbackgroundtick + runningnormallybackgroundtick = totalBackgroundTasks, meaning all backround tasks are accounted for (when encounterdetectionpausedbackgroundtick + runningnormallybackgroundtick is less than totalBackgroundTasks, Bluetooth may be disabled). Alarm Manager ran, so a packet was sent via Alarm Manager. */
				WHEN aad.latestApplicationVersion NOT IN ('3.0','3.1','3.10','3.11','3.12','3.2','3.3','3.4','3.5','3.6','3.7','3.8','3.9','4.0','4.1','4.2','4.3','4.4','4.5','4.6','4.7','4.8','4.9','29','30','32','35','36','39','41','42','3.0.1','3.1.1','3.1.2','3.10.1','3.10.2','3.12.1','3.12.2','3.3-internal','3.4-internal','3.5-internal','3.6.1','3.6.2','3.6.2-internal','3.6-internal','3.7.1','3.7.2','3.7-internal','3.9-internal','4.0.1','4.1.1','4.2.1','4.6.1','4.7.1')
				AND aad.encounterdetectionpausedbackgroundtick > 0
				AND aad.runningnormallybackgroundtick > 0
				AND aad.encounterdetectionpausedbackgroundtick + aad.runningnormallybackgroundtick = aad.totalBackgroundTasks
				AND aad.totalAlarmManagerBackgroundTasks > 0
				THEN 'Partly Contact Traceable Due to Pause(Scenario 16.1)'
			
  
/* ANDROID PARTLY CONTACT TRACEABLE DUE TO BOTH SCENARIO 16.2 */
                     
/* Encounter detection is paused at least once in the day, but not every time a background task is run. runningnormallybackgroundtick runs at least once in the day, meaning the app is running normally for part of the day. encounterdetectionpausedbackgroundtick + runningnormallybackgroundtick is less than totalBackgroundTasks, meaning Bluetooth may be disabled for part of the day. Alarm Manager ran, so a packet was sent via Alarm Manager. */
				WHEN aad.latestApplicationVersion NOT IN ('3.0','3.1','3.10','3.11','3.12','3.2','3.3','3.4','3.5','3.6','3.7','3.8','3.9','4.0','4.1','4.2','4.3','4.4','4.5','4.6','4.7','4.8','4.9','29','30','32','35','36','39','41','42','3.0.1','3.1.1','3.1.2','3.10.1','3.10.2','3.12.1','3.12.2','3.3-internal','3.4-internal','3.5-internal','3.6.1','3.6.2','3.6.2-internal','3.6-internal','3.7.1','3.7.2','3.7-internal','3.9-internal','4.0.1','4.1.1','4.2.1','4.6.1','4.7.1')
				AND aad.encounterdetectionpausedbackgroundtick > 0
				AND aad.runningnormallybackgroundtick > 0
				AND aad.encounterdetectionpausedbackgroundtick + aad.runningnormallybackgroundtick < aad.totalBackgroundTasks
				AND aad.totalAlarmManagerBackgroundTasks > 0
				THEN 'Partly Contact Traceable Due to Both(Scenario 16.2)'
				
  
/* ANDROID PARTLY CONTACT TRACEABLE DUE TO PAUSE OR DISABLED SCENARIO 16.3 */
                     
/* Encounter detection is paused at least once in the day, but not every time a background task is run. runningnormallybackgroundtick = totalBackgroundTasks (so every time the app has run in the background (totalBackgroundTasks), the app has been fully functional (runningnormallybackgroundtick). encounterdetectionpausedbackgroundtick + runningnormallybackgroundtick is less than totalBackgroundTasks, meaning Bluetooth may be disabled for part of the day. The background task has run at least once (so it is definitely working). */
				WHEN aad.latestApplicationVersion IN ('4.2.1','4.2','4.1.1','4.1','4.0.1','4.0','3.12.2','3.12.1','3.12','3.10','3.9','3.7.2','3.7.1','3.7','3.6.2','3.6.1','3.6','35','41')
				AND aad.encounterdetectionpausedbackgroundtick > 0
				AND aad.encounterdetectionpausedbackgroundtick < aad.totalBackgroundTasks
				AND aad.runningnormallybackgroundtick = aad.totalBackgroundTasks
				AND aad.totalBackgroundTasks > 0 
				THEN 'Partly Contact Traceable Due to Pause or Disabled(Scenario 16.3)'
				
  
/* ANDROID UNEXPECTED OTHER SCENARIOS (FOR PRE-v4.3) */
                     
/* runningnormallybackgroundtick is not equal to totalBackgroundTasks (unexpected behaviour in the below app versions). */
				WHEN aad.latestApplicationVersion IN ('4.2.1','4.2','4.1.1','4.1','4.0.1','4.0','3.12.2','3.12.1','3.12','3.10','3.9','3.7.2','3.7.1','3.7','3.6.2','3.6.1','3.6','35','41')
				AND aad.runningnormallybackgroundtick != aad.totalBackgroundTasks
				THEN 'Unexpected Scenario'
			
				ELSE 'Other'
END
        


/* iOS appstate scenarios */
  
                WHEN Upper(devicemodel) LIKE '%IPHONE%'  THEN
           	

/* iOS NON-CONTACT TRACEABLE SCENARIO 1 */
                     
/* No background tasks are running (eg. due to not being fully onboarded, exposure notifications being turned off in the settings, bluetooth being switched off, network connectivity unavailable, etc). */

CASE
                WHEN aad.encounterdetectionpausedbackgroundtick = 0
				AND aad.runningnormallybackgroundtick = 0 
				AND aad.totalBackgroundTasks = 0
				THEN 'Not Contact Traceable (Scenario 1)'
			

/* iOS NON-CONTACT TRACEABLE SCENARIO 3 */
        
/* Encounter detection is not paused but the app is not running normally (eg. due to not being fully onboarded, exposure notifications being turned off in the settings, bluetooth being switched off, network connectivity unavailable, etc). The background task has run at least once (so it is definitely working). */
				WHEN aad.encounterdetectionpausedbackgroundtick = 0
				AND aad.runningnormallybackgroundtick = 0 
				AND aad.totalBackgroundTasks > 0
				THEN 'Not Contact Traceable (Scenario 3)'
			
  
/* iOS FULLY CONTACT TRACEABLE SCENARIO 7.1 */
        
/* Encounter detection is not paused. runningnormallybackgroundtick = totalBackgroundTasks (so every time the app has run in the background (totalBackgroundTasks), the app has been fully functional (runningnormallybackgroundtick). The background task has run at least once (so it is definitely working). */
				WHEN aad.encounterdetectionpausedbackgroundtick = 0
				AND aad.runningnormallybackgroundtick = aad.totalBackgroundTasks
				AND aad.totalBackgroundTasks > 0
				THEN 'Fully Contact Traceable (Scenario 7.1)'

  
/* iOS PARTLY CONTACT TRACEABLE SCENARIO 7.2*/
        
/* Encounter detection is not paused. runningnormallybackgroundtick is less than totalBackgroundTasks (so there have been occasions during the day where the app has not been fully functional) - but runningnormallybackgroundtick is greater than 0 - so the app has been fully functional for part of the day. The background task has run at least once (so it is definitely working). */
				WHEN aad.encounterdetectionpausedbackgroundtick = 0
				AND aad.runningnormallybackgroundtick < aad.totalBackgroundTasks
				AND aad.runningnormallybackgroundtick > 0
				AND aad.totalBackgroundTasks > 0
				THEN 'Partly Contact Traceable Due to Disabled(Scenario 7.2)'
			
  
/* iOS NON-CONTACT TRACEABLE SCENARIO 11.1 */
        
 /* Encounter detection is showing as paused every time a background task is run. runningnormallybackgroundtick = 0, indicating the app is not running normally (eg. due to not being fully onboarded, exposure notifications being turned off in the settings, bluetooth being switched off, network connectivity unavailable, etc). The background task has run at least once (so it is definitely working). */
				WHEN aad.encounterdetectionpausedbackgroundtick = aad.totalBackgroundTasks
				AND aad.runningnormallybackgroundtick = 0
				AND aad.totalBackgroundTasks > 0
				THEN 'Not Contact Traceable (Scenario 11.1)'
			
  
/* iOS NON-CONTACT TRACEABLE SCENARIO 11.2 */
        
  /* Encounter detection is paused at least once in the day, but not every time a background task is run. runningnormallybackgroundtick = 0, indicating the app is not running normally (eg. due to not being fully onboarded, exposure notifications being turned off in the settings, bluetooth being switched off, network connectivity unavailable, etc). The background task has run at least once (so it is definitely working). */
				WHEN aad.encounterdetectionpausedbackgroundtick > 0
				AND aad.encounterdetectionpausedbackgroundtick < aad.totalBackgroundTasks
				AND aad.runningnormallybackgroundtick = 0
				AND aad.totalBackgroundTasks > 0
				THEN 'Not Contact Traceable (Scenario 11.2)'
			
  
/* iOS PARTLY CONTACT TRACEABLE DUE TO PAUSE SCENARIO 18.1 */
        
/* encounterdetectionpausedbackgroundtick = totalBackgroundTasks, meaning encounter detection is paused all day. runningnormallybackgroundtick = totalBackgroundTasks (so every time the app has run in the background (totalBackgroundTasks), the app has been fully functional (runningnormallybackgroundtick). The background task has run at least once (so it is definitely working). */
				WHEN aad.encounterdetectionpausedbackgroundtick = aad.totalBackgroundTasks
				AND aad.runningnormallybackgroundtick = aad.totalBackgroundTasks
				AND aad.totalBackgroundTasks > 0 
				THEN 'Not Contact Traceable(Scenario 18.1)'
				
  
/* iOS PARTLY CONTACT TRACEABLE DUE TO PAUSE SCENARIO 18.2 */
        
/* encounterdetectionpausedbackgroundtick = totalBackgroundTasks, meaning encounter detection is paused all day. runningnormallybackgroundtick is less than totalBackgroundTasks (so there have been occasions during the day where the app has not been fully functional) - but runningnormallybackgroundtick is greater than 0 - so the app has been fully functional for part of the day. The background task has run at least once (so it is definitely working). */
				WHEN aad.encounterdetectionpausedbackgroundtick = aad.totalBackgroundTasks
				AND aad.runningnormallybackgroundtick < aad.totalBackgroundTasks
				AND aad.runningnormallybackgroundtick > 0
				AND aad.totalBackgroundTasks > 0 
				THEN 'Not Contact Traceable(Scenario 18.2)'
			

/* iOS PARTLY CONTACT TRACEABLE DUE TO PAUSE SCENARIO 19.1 */
        
/* Encounter detection is paused at least once in the day, but not every time a background task is run. runningnormallybackgroundtick = totalBackgroundTasks (so every time the app has run in the background (totalBackgroundTasks), the app has been fully functional (runningnormallybackgroundtick). The background task has run at least once (so it is definitely working). */
				WHEN aad.encounterdetectionpausedbackgroundtick > 0
				AND aad.encounterdetectionpausedbackgroundtick < aad.totalBackgroundTasks
				AND aad.runningnormallybackgroundtick = aad.totalBackgroundTasks
				AND aad.totalBackgroundTasks > 0 
				THEN 'Partly Contact Traceable Due to Pause(Scenario 19.1)'
			
  
/* iOS PARTLY CONTACT TRACEABLE DUE TO PAUSE SCENARIO 19.2 */
        
/* Encounter detection is paused at least once in the day, but not every time a background task is run. runningnormallybackgroundtick is less than totalBackgroundTasks (so there have been occasions during the day where the app has not been fully functional) - but runningnormallybackgroundtick is greater than 0 - so the app has been fully functional for part of the day. The background task has run at least once (so it is definitely working). */
				WHEN aad.encounterdetectionpausedbackgroundtick > 0
				AND aad.encounterdetectionpausedbackgroundtick < aad.totalBackgroundTasks
				AND aad.runningnormallybackgroundtick < aad.totalBackgroundTasks
				AND aad.runningnormallybackgroundtick > 0
				AND aad.totalBackgroundTasks > 0 
				THEN 'Partly Contact Traceable Due to Pause and Disabled(Scenario 19.2)'
			
				ELSE 'Other'
END

END AS appstate FROM "${workspace}_analytics_db"."${workspace}_${mobileAnalyticsTable}" as aad 
            LEFT JOIN "${workspace}_analytics_db"."${workspace}_analytics_postcode_demographic_geographic_lookup" AS pdgl
                ON (aad.localauthority <> ''
                    AND aad.postaldistrict = pdgl.postcode
                    AND aad.localauthority = pdgl.lad20cd
                    AND (pdgl.country NOT IN ('Scotland', 'Northern Ireland')
                    OR pdgl.country IS NULL))
            LEFT JOIN "${workspace}_analytics_db"."${workspace}_analytics_postcode_demographic_geographic_lookup" AS pdgl2
                ON ((aad.localauthority = ''
                    OR aad.localauthority IS NULL)
                    AND aad.postaldistrict = pdgl2.postcode
                    AND pdgl2.lad20cd = ''
                    AND (pdgl2.country NOT IN ('Scotland', 'Northern Ireland')
                    OR pdgl2.country IS NULL)) 
                    
          WHERE
           aad.latestApplicationVersion NOT LIKE '%-internal%'
			AND aad.latestApplicationVersion NOT IN ('30', '32', '36', '39', '42', '29', '3.11', '3.2', '3.3', '3.4', '3.5', '3.8','4.11', '4.8','4.15','4.20') 
            AND date_parse(substring(aad.startdate, 1, 10), '%Y-%c-%d') <= date_parse(substring(aad.submitteddatehour,1,13), '%Y/%c/%d/%H') 
            AND date_parse(substring(aad.startdate,1,10), '%Y-%c-%d') < current_date
            AND aad.startdate <> aad.enddate
            AND date_parse(substring(aad.startdate,1,10), '%Y-%c-%d') >= date('2020-09-24')
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
                            END)))
							
				GROUP BY 		
				truncatedStartDate,
				country) 
				WHERE country IS NOT NULL
				
				GROUP BY firstDayReportingWeek,
            lastDayReportingWeek,
            country
			        ORDER BY
            lastDayReportingWeek,
            country
)



/* Output */
SELECT
m."Week starting (Wythnos yn dechrau)",
m."Week ending (Wythnos yn gorffen)",
m."Country (Wlad)",
m."Average Daily Number of Users With App Installed (Cyfartaledd Nifer o Ddefnyddwyr Dyddiol gyda'r Ap wedi ei Osod)",
m."Average Daily Number of Users Where App is Contact Traceable (Cyfartaledd Nifer o Ddefnyddwyr Dyddiol ble mae'r Ap yn Caniatáu Olrhain Cyswllt)",
CAST(m."Average Daily Number of Users With App Installed (Cyfartaledd Nifer o Ddefnyddwyr Dyddiol gyda'r Ap wedi ei Osod)" AS DECIMAL (10,2)) / CAST(p."population" AS DECIMAL (10,2)) AS "Average Daily Number of Users With App Installed as Percentage of Population (Cyfartaledd Nifer o Ddefnyddwyr Dyddiol gyda'r Ap wedi ei Osod fel Canran o'r Boblogaeth)",
CAST(m."Average Daily Number of Users Where App is Contact Traceable (Cyfartaledd Nifer o Ddefnyddwyr Dyddiol ble mae'r Ap yn Caniatáu Olrhain Cyswllt)" AS DECIMAL (10,2)) / CAST(p."population" AS DECIMAL (10,2)) AS "Average Daily Number of Users Where App is Contact Traceable as Percentage of Population (Cyfartaledd Nifer o Ddefnyddwyr Dyddiol ble mae'r Ap yn Caniatáu Olrhain Cyswllt fel Canran o'r Boblogaeth)"

FROM
main m

LEFT JOIN
population p
ON
m."Country (Wlad)" = p."Country (Wlad)"
        """
    )

    override fun checkQueryState(queryId: QueryId): QueryResult<Unit> = asyncDbClient.queryResults(queryId)

}
