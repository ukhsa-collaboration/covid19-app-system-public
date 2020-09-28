# Risky Venue Circuit Breaker

API group: [Submission]((../ag-architecture-guidebook#System-APIs-and-Interfaces)

## Scenario

We assume the circuit breaker counts the number of identified risk venues over some time period (e.g. 2h) and hence relies only on getting the venueIds.

### Initial request

- Endpoint schema: ```[POST] https://<FQDN>/circuit-breaker/venue/request```
    - FQDN: Hostname can be different per API
- Authorization: ```Authorization: Bearer <API KEY>```
    - One API KEY for all mobile phone-facing APIs
- Request payload content-type: application/json
- Response payload content-type: application/json

### Response Headers
- Signature (ECDSA_SHA_256) of response body: ```x-amz-meta-signature: keyId="(AWS ACM CMK key id)",signature="(base64 encoded signature)"```

#### Payload Example

```json
{
  "venueId": "MAX8CHR1"
}
```

#### Response Payload Example

```json
{
  "approval_token": "QkFDQzlBREUtN0ZBMC00RTFELUE3NUMtRTZBMUFGNkMyRjNECg",
  "approval": "yes"|"no"|"pending"  
}
```

### Poll for resolution status

- Endpoint schema: ```[GET] https://<FQDN>/circuit-breaker/venue/resolution/<approval_token>```
    - FQDN: Hostname can be different per API
- Authorization: ```Authorization: Bearer <API KEY>```
    - One API KEY for all mobile phone-facing APIs
- Response payload content-type: application/json

#### Response Payload Example

```json
{
  "approval": "yes"|"no"|"pending"
}
```
