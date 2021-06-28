# Diagnosis Key Distribution

> API Pattern: [Distribution](../../../api-patterns.md#distribution)

## HTTP Request and Response

- Daily ZIP of diagnosis keys: ```GET https://<FQDN>/distribution/daily/yyyyMMdd00.zip```
- Two-hourly ZIP of diagnosis keys: ```GET https://<FQDN>/distribution/two-hourly/yyyyMMddhh.zip```

### Parameters

- FQDN: One (CDN) hostname for all distributed APIs
- Authorization NOT required and signatures provided - see [API security](../../../api-security.md)
- ```yyyyMMdd00``` (formatted UTC timestamp): 14 valid values (the last 14 days < today)
- ```yyyyMMddhh``` (formatted UTC timestamp, ```hh``` = ```hour of day - hour of day % 2```): ```14*12``` valid values (the last ```14*12``` two-hour periods < current two-hour period)

### Response
- Content-Type: `application/zip`
- The file format ("the ZIPs" in the scenario below) is described here: https://developers.google.com/android/exposure-notifications/exposure-key-file-format

## Scenario

- The client downloads diagnosis keys periodically (newly available keys since the last successful download)
- If the downloads fails, the client will try again in the next download cycle

### First app launch after install

- Current time (mobile): 2020-06-26 09:12:43 UTC
- Last download timestamp: n/a

Sequential downloads (assumption: full history is not required):

    GET https://distribution-<host>/distribution/two-hourly/2020062608.zip

After successful download of all ZIPs, the mobile app will store 2020062608 as "last download timestamp"

### First incremental download after 1h

- Current time (mobile): 2020-06-26 10:02:00 UTC
- Last download timestamp: 2020062608

Sequential downloads:

    GET https://distribution-<host>/distribution/two-hourly/2020062610.zip

After successful download of the ZIP, the mobile app will store 2020062610 as "last download timestamp"

### Second incremental download after 4h (no internet connectivity for a while)

- Current time (mobile): 2020-06-26 14:07:30 UTC
- Last download timestamp: 2020062610

Sequential downloads:

    GET https://distribution-<host>/distribution/two-hourly/2020062612.zip
    GET https://distribution-<host>/distribution/two-hourly/2020062614.zip

Mobile will store 2020062614 as "last downloaded" after successful download of the ZIP
