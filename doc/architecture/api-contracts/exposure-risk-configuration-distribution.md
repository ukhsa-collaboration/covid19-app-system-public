# Exposure Notification & Risk Calculation Configuration Distribution

API group: [Distribution](../guidebook.md#system-apis-and-interfaces)

- Endpoint schema: ```https://<FQDN>/distribution/exposure-configuration```
  - FQDN: One (CDN-) hostname for all distribute APIs
- Payload content-type: application/json
- Signature (ECDSA_SHA_256) of response body: ```x-amz-meta-signature: keyId="(AWS ACM CMK key id)",signature="(base64 encoded signature)"```
- Mobile app downloads following configuration json and:
    - The `exposureNotification` is used to do the initial setup of the exposure api (based on the German configuration values)
    - The `riskCalculation` is used to then calculate the risk (based on government policy on acceptable true positive and false positive rates for alerting users)

[Read more about the German risk score calculation](https://github.com/corona-warn-app/cwa-documentation/blob/master/solution_architecture.md#risk-score-calculation)

[Read more about the German exposure & risk configuration](https://github.com/corona-warn-app/cwa-documentation/blob/master/cwa-risk-assessment.md#current-configuration)

[Read more about Exposure Notification Configuration](https://static.googleusercontent.com/media/www.google.com/en//covid19/exposurenotifications/pdfs/Android-Exposure-Notification-API-documentation-v1.3.2.pdf)

## Payload Example

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