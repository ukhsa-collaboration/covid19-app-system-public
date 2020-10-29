# Exposure Notification Circuit Breaker

API group: [Submission](../guidebook.md#system-apis-and-interfaces)

## Scenario

### Initial request

- Endpoint schema: ```[POST] https://<FQDN>/circuit-breaker/exposure-notification/request```
    - FQDN: Hostname can be different per API
- Authorisation: ```Authorization: Bearer <API KEY>```
    - One API KEY for all mobile phone-facing APIs
- Request payload content-type: application/json
- Response payload content-type: application/json

### Response Headers
- Signature (ECDSA_SHA_256) of response body: ```x-amz-meta-signature: keyId="(AWS ACM CMK key id)",signature="(base64 encoded signature)"```

#### Payload Example

```json
{
  "matchedKeyCount" : 2,
  "daysSinceLastExposure": 3,
  "maximumRiskScore" : 150,
  "riskCalculationVersion": 2
}
```
Notes: `riskCalculationVersion` should be an optional field and default to `1` if not sent by a client. (Allows supporting older clients which do not send that field)

##### Risk calculation version
Currently there are two risk calculation versions with the following mapping:

*Version 1*
- Apps or devices using EN API v1.5/1 (App version < 3.9, iOS devices < iOS 13.7)

*Version 2*
- Apps or devices using EN API v1.6/2 or higher (App version >= 3.9, iOS devices >= iOS 13.7)


#### Response Payload Example

```json
{
  "approvalToken": "QkFDQzlBREUtN0ZBMC00RTFELUE3NUMtRTZBMUFGNkMyRjNECg",
  "approval": "yes"|"no"|"pending"
}
```

### Poll for resolution status

- Endpoint schema: ```[GET] https://<FQDN>/circuit-breaker/exposure-notification/resolution/<approval_token>```
    - FQDN: Hostname can be different per API
- Authorisation: ```Authorization: Bearer <API KEY>```
    - One API KEY for all mobile phone-facing APIs    
- Response payload content-type: application/json

#### Response Payload Example

```json| 
{
  "approval": "yes"|"no"|"pending"
}
```
