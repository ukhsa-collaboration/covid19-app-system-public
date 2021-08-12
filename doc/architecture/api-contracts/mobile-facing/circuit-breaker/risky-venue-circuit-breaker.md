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

The circuit breaker is polled following a risky venue notification to get a decision on whether to proceed to notify the user of the venue risk.

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
