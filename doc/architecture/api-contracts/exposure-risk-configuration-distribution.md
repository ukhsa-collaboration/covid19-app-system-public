# Exposure Notification & Risk Calculation Configuration Distribution

API group: [Distribution](../guidebook.md#system-apis-and-interfaces)

## HTTP request and response

- Exposure Configuration: ```GET https://<FQDN>/distribution/exposure-configuration```

### Parameters

- FQDN: One (CDN-) hostname for all distribute APIs
- Authorization NOT required and signatures provided - see [API security](./security.md)
- Payload content-type: application/json

## Scenario
- Mobile app downloads following configuration json and:
    - The `exposureNotification` is used to do the initial setup of the exposure api 
    - The `riskCalculation` is used to then calculate the risk (based on government policy on acceptable true positive and false positive rates for alerting users)

[Read more about our algorithm and configuration](https://www.turing.ac.uk/blog/technical-roadmap-uks-contract-tracing-app-functionality)

[Read more about the API for Exposure Notification Configuration](https://static.googleusercontent.com/media/www.google.com/en//covid19/exposurenotifications/pdfs/Android-Exposure-Notification-API-documentation-v1.3.2.pdf)

## Example: Exposure Configuration
```GET https://<FQDN>/distribution/exposure-configuration```

### Response Example (structure)
```json
    {
      "exposureNotification": {
        "minimumRiskScore": 11,
        "attenuationDurationThresholds": [55, 63],
        "attenuationLevelValues": [0, 1, 1, 1, 1, 1, 1, 1],
        "daysSinceLastExposureLevelValues": [5, 5, 5, 5, 5, 5, 5, 5],
        "durationLevelValues": [0, 0, 0, 1, 1, 1, 1, 1],
        "transmissionRiskLevelValues": [1, 2, 3, 4, 5, 6, 7, 8],
        "attenuationWeight": 50.0,
        "daysSinceLastExposureWeight": 20,
        "durationWeight": 50.0,
        "transmissionRiskWeight": 50.0
      },
      "riskCalculation": {
        "durationBucketWeights": [1.0, 0.5, 0.0],
        "riskThreshold": 180
      },
      "riskScore": {
        "sampleResolution": 1.0,
        "expectedDistance": 0.1,
        "minimumDistance": 1.0,
        "rssiParameters": {
          "weightCoefficient": 0.1270547531082051,
          "intercept": 4.2309333657856945,
          "covariance": 0.4947614361027773
        },
        "powerLossParameters": {
          "wavelength": 0.125,
          "pathLossFactor": 20.0,
          "refDeviceLoss": 0.0
        },
        "observationType": "log",
        "initialData": {
          "mean": 2.0,
          "covariance": 10.0
        },
        "smootherParameters": {
          "alpha": 1.0,
          "beta": 0.0,
          "kappa": 0.0
        }
      },  
      "v2RiskCalculation": {
        "daysSinceOnsetToInfectiousness":[0,0,0,0,0,0,0,0,0,1,1,1,2,2,2,2,2,2,1,1,1,1,1,1,0,0,0,0,0],
        "infectiousnessWeights": [0.0, 0.4, 1.0],
        "reportTypeWhenMissing": 1,
        "riskThreshold": 120
      }
    }  
```

For the live version check [src/static/exposure-configuration.json](../../../src/static/exposure-configuration.json)

* The following fields are currently unused and EN-API documentation states that they are reserved for future use: 
`attenuationWeight`,`daysSinceLastExposureWeight`,`transmissionRiskWeight`,`durationWeight`. 

* We will use a threshold on the weighted average of the attenuationDuration buckets.
* The `durationBucketWeights` and `riskThreshold` are TBD.  
