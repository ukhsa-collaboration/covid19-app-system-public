# Exposure Notification Circuit Breaker

API group: [Submission](../../../guidebook.md#system-apis-and-interfaces)

## HTTP request and response

- Circuit Breaker Request: ```POST https://<FQDN>/circuit-breaker/exposure-notification/request```
- Circuit Breaker Resolution: ```GET https://<FQDN>/circuit-breaker/exposure-notification/resolution/<approval_token>```

### Parameters
- FQDN: Hostname can be different per API
- Authorization required and signatures provided - see [API security](../../security.md)
- Request payload content-type: `application/json`
- Response payload content-type: `application/json`

## Scenario

### Example: Initial request
```POST https://<FQDN>/circuit-breaker/exposure-notification/request```

#### Payload Example

```json
{
  "matchedKeyCount" : 2,
  "daysSinceLastExposure": 3,
  "maximumRiskScore" : 150.0,
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

### Example: Poll for resolution status

```GET https://<FQDN>/circuit-breaker/exposure-notification/resolution/<approval_token>```

#### Response Payload Example

```json
{
  "approval": "yes"|"no"|"pending"
}
```
