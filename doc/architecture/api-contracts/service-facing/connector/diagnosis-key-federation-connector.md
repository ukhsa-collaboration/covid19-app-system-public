# Diagnosis Key Federation Connector

> API Pattern: [Connector](../../../api-patterns.md#connector)

## Lambda Invocation
Federation upload and download Lambda 

- Scheduled to run periodically (~every 30 minutes)

## Scenario
### Download of diagnosis keys (or Import)

- The lambda downloads batches of diagnosis keys using NearForm Federation API, the Diagnosis Keys are then validated and uploaded to the Diagnosis Keys Submission (JSON) bucket.
- The keys downloaded using nearform API are stored in the following format
  ```<federated_key_prefix>/<origin>/<downloadDate>/<filename>.json```
- The last downloaded batchtag is stored in a table ```<workspace-name>-federation-key-proc-history```

#### Response Payload Example

```json
{
    "batchTag": "1110614f-6ff0-4e95-a79f-570744b55c63",
    "exposures": [
        {
            "keyData": "Ns4/fV5RF+8GS/WJFmJUdw==",
            "rollingStartNumber": 2671920,
            "transmissionRiskLevel": 0,
            "rollingPeriod": 144,
            "origin": "JE",
            "regions": [
                "JE"
            ]
        }
    ]
}
```

#### Validation rules used to accept the keys from Federation server

- Keys should not be older than 14 days or have a future date.
- Keys should be Base64 encoded and 32 bytes in length.
- Transmission risk level should be ```>=0``` and ```<=7```.
- Keys should be from a valid origin.

### Upload of diagnosis keys (or Export) 

- Lambda uploads the diagnosis keys from the submission bucket to the Nearform server using Nearform Federation API.
- The Submission date of the uploaded keys is stored in a table ```<workspace-name>-federation-key-proc-history```
- We can configure the origins/S3 key prefixes (Diagnosis Keys Submission bucket) to control which Diagnosis Keys will be uploaded

#### Request Payload Example

Request payload (unsigned):

```json
  [
    {
      "transmissionRiskLevel": 4,
      "rollingStartNumber": 2670912,
      "keyData": "gi+hAyRA3vavaYhRoPhCrg==",
      "rollingPeriod": 144,
      "regions": [
        "GB-EAW"
      ]
    }
  ]
```

HTTP request body with signed payload (JWS compact format):

```json
  {
    "batchTag": "04b323bc",
    "payload": "eyJhbGciOiJFUzI1NiJ9.W3sidHJhbnNtaXNzaW9uUmlza0xldmVsIjo0LCJyb2xsaW5nU3RhcnROdW1iZXIiOjI2NzA5MTIsImtleURhdGEiOiJnaStoQXlSQTN2YXZhWWhSb1BoQ3JnPT0iLCJyb2xsaW5nUGVyaW9kIjoxNDQsInJlZ2lvbnMiOlsiQ0giXX1d.j1PlrHTA6BD7mAauPfV-Q41eJsA6tuOzMZ-EqOtmTWJfpHAwh_r-X8pqpTlKRtcPUgmraGoaX4ztrI_eqV91gg"
  }
```
