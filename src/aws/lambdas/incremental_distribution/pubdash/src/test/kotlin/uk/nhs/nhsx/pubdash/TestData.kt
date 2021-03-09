package uk.nhs.nhsx.pubdash

object TestData {
    const val countryAgnosticDatasetCsvHeader =
        """"Week ending (Wythnos yn gorffen)","Number of app downloads (Nifer o lawrlwythiadau ap)","Number of NHS QR posters created (Nifer o bosteri cod QR y GIG a grëwyd)","Number of 'at risk' venues triggering venue alerts (Nifer o leoliadau 'dan risg')""""

    const val countrySpecificDatasetCsvHeader =
        """"Week ending (Wythnos yn gorffen)","Country","Wlad","Contact tracing alert (Hysbysiadau olrhain cyswllt)","Check-ins (Cofrestriadau)","Positive test results linked to app (Canlyniadau prawf positif)","Negative test results linked to app (Canlyniadau prawf negatif)","Symptoms reported (Symptomau a adroddwyd)""""

    const val localAuthorityDatasetCsvHeader =
        """"Week ending (Wythnos yn gorffen)","Local authority (Awdurdod lleol)","Contact tracing alert (Hysbysiadau olrhain cyswllt)","Check-ins (Cofrestriadau)","Positive test results linked to app (Canlyniadau prawf positif)","Negative test results linked to app (Canlyniadau prawf negatif)","Symptoms reported (Symptomau a adroddwyd)""""
}
