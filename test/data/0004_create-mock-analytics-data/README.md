# Background

This directory contains some mock data for analytics, as of COV-732. This is just a temporary solution, which will need to get adjusted (or removed) once the proper data structure is known.

In the real world situation this data will be provided by the mobile backend but for the testing and developing purposes we mocked that data.

More about what each field in json file represents can be found here: 
Confluence - Data Dictionary

# Usage

To upload all JSON files from the `json` directory to an S3 bucket, run:
```
rake publish:data:analytics_data:branch TEST_DATA=test/data/0004_create-mock-analytics-data/json
```

# Data structure example
```$xslt
{
  "startDate": "YYYY-MM-DDThh:mm:ssZ",
  "endDate": "YYYY-MM-DDThh:mm:ssZ",
  "postalDistrict": "string",
  "deviceModel": "string",
  "operatingSystemVersion": "string",
  "onboardingCompletedToday": boolean,
  "backgroundTasksPerformed": int,
  "dataDownloadUsageBytes": int,
  "qrCodeCheckInCounts": int,
  "symptomaticQuestionnaireResults": "positive OR negative OR <emptystring>",
  "isolationStatus": boolean,
  "isolationReason": "self-diagnosis OR exposure-notification OR <emptystring>"
}
```