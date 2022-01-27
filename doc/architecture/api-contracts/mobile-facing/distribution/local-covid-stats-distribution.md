# Local COVID-19 Statistics Distribution

> API Pattern: [Distribution](../../../api-patterns.md#distribution)

## HTTP Request and Response

- Content module: ```GET https://<FQDN>/distribution/v1/local-covid-stats-daily```

### Parameters

- FQDN: One (CDN) hostname for all distributed APIs
- Authorization NOT required and signatures provided - see [API security](../../../api-security.md)
- Payload content-type: `application/json`
- Payload content-encoding: `gzip`

## Scenario

### Get daily local COVID-19 statistics

`GET https://<FQDN>/distribution/v1/local-covid-stats-daily`

#### Response Payload Example (structure)

```json
{
    "lastFetch": "2021-11-15T21:59:00Z",
    "metadata": {
        "england": {
            "newCasesBySpecimenDateRollingRate": {
                "lastUpdate": "2021-11-13"
            }
        },
        "wales": {
            "newCasesBySpecimenDateRollingRate": {
                "lastUpdate": "2021-11-13"
            }
        },
        "lowerTierLocalAuthorities": {
            "newCasesByPublishDate": {
                "lastUpdate": "2021-11-18"
            },
            "newCasesByPublishDateChangePercentage": {
                "lastUpdate": "2021-11-18"
            },
            "newCasesByPublishDateChange": {
                "lastUpdate": "2021-11-18"
            },
            "newCasesByPublishDateRollingSum": {
                "lastUpdate": "2021-11-18"
            },
            "newCasesByPublishDateDirection": {
                "lastUpdate": "2021-11-18"
            },
            "newCasesBySpecimenDateRollingRate": {
                "lastUpdate": "2021-11-13"
            }
        }
    },
    "england": {
        "newCasesBySpecimenDateRollingRate": 510.8
    },
    "wales": {
        "newCasesBySpecimenDateRollingRate": null
    },
    "lowerTierLocalAuthorities": {
        "E06000037": {
            "name": "West Berkshire",
            "newCasesByPublishDateRollingSum": -771,
            "newCasesByPublishDateChange": 207,
            "newCasesByPublishDateDirection": "UP",
            "newCasesByPublishDate": 105,
            "newCasesByPublishDateChangePercentage": 36.7,
            "newCasesBySpecimenDateRollingRate": 289.5
        },
        "E08000035": {
            "name": "Leeds",
            "newCasesByPublishDateRollingSum": null,
            "newCasesByPublishDateChange": null,
            "newCasesByPublishDateDirection": null,
            "newCasesByPublishDate": null,
            "newCasesByPublishDateChangePercentage": null,
            "newCasesBySpecimenDateRollingRate": null
        }
    }
}
```

- `lastFetch`: ISO-8601 timestamp in `YYYY-MM-DD'T'hh:mm:ssZ` format
    - Global update timestamp (when the backend fetches the data from the external api)

- `metadata`: Contains the date when the metrics were last updated

- `<areaType>.<areaCode>`:
    - `name`: area name
    - `newCasesByPublishDate` nullable number
    - `newCasesByPublishDateChangePercentage` nullable number
    - `newCasesByPublishDateChange` nullable number
    - `newCasesByPublishDateRollingSum` nullable number
    - `newCasesByPublishDateDirection` nullable string, possible values `[SAME, UP, DOWN]`
    - `newCasesBySpecimenDateRollingRate` nullable number
