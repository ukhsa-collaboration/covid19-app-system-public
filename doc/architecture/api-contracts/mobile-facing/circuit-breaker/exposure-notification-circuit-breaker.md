# Exposure Notification Circuit Breaker

> API Pattern: [Circuit Breaker](../../../api-patterns.md#circuit-breaker)

## HTTP Request and Response

- Circuit Breaker Request: ```POST https://<FQDN>/circuit-breaker/exposure-notification/request```
- Circuit Breaker Resolution: ```GET https://<FQDN>/circuit-breaker/exposure-notification/resolution/<approval_token>```

### Parameters
- FQDN: Hostname can be different per API
- Authorization required and signatures provided - see [API security](../../../api-security.md)
- Request payload content-type: `application/json`
- Response payload content-type: `application/json`

## Scenario

### Initial request
```POST https://<FQDN>/circuit-breaker/exposure-notification/request```

#### Request Payload Example

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

Risk Calculation *V1*
- Apps or devices using EN API v1.5/1 (App version < 3.9, iOS devices < iOS 13.7)

Risk Calculation *V2*
- Apps or devices using EN API v1.6/2 or higher (App version >= 3.9, iOS devices >= iOS 13.7)

#### Response Payload Example

```json
{
  "approvalToken": "QkFDQzlBREUtN0ZBMC00RTFELUE3NUMtRTZBMUFGNkMyRjNECg",
  "approval": "yes"|"no"|"pending"
}
```

### Poll for resolution status

```GET https://<FQDN>/circuit-breaker/exposure-notification/resolution/<approval_token>```

#### Response Payload Example

```json
{
  "approval": "yes"|"no"|"pending"
}
```
