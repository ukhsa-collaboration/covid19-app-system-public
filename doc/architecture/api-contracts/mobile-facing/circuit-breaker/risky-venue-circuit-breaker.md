# Risky Venue Circuit Breaker

> API Pattern: [Circuit Breaker](../../../api-patterns.md#circuit-breaker)

## HTTP Request and Response

- Circuit Breaker Request: ```POST https://<FQDN>/circuit-breaker/venue/request```
- Circuit Breaker Resolution: ```GET https://<FQDN>/circuit-breaker/venue/resolution/<approval_token>```

### Parameters

- FQDN: Hostname can be different per API
- Authorization required and signatures provided - see [API security](../../../api-security.md)
- Response payload content-type: application/json

## Scenario

Used as a pre-requisite to check if a notification should be sent to the mobile app making the request.

### Initial request
```POST https://<FQDN>/circuit-breaker/venue/request```

#### Response Payload Example

```json
{
  "approval_token": "QkFDQzlBREUtN0ZBMC00RTFELUE3NUMtRTZBMUFGNkMyRjNECg",
  "approval": "yes"|"no"|"pending"  
}
```

### Poll for resolution status

```GET https://<FQDN>/circuit-breaker/venue/resolution/<approval_token>```

#### Response Payload Example

```json
{
  "approval": "yes"|"no"|"pending"
}
```
