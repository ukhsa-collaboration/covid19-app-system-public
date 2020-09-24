# Exposure Notification Circuit Breaker

API group: [Submission](../ag-architecture-guidebook#System-APIs-and-Interfaces)

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
  "maximumRiskScore" : 150
}
```

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

```json
{
  "approval": "yes"|"no"|"pending"
}
```
