# Empty Submission Endpoint

API group: [Submission](../guidebook.md#system-apis-and-interfaces)

This API is used for time-based traffic obfuscation.

## HTTP request and response

- Empty Submission (V1)
  - ```POST https://<FQDN>/submission/empty-submission```
- Empty Submission (V2)
  - ```GET https://<FQDN>/submission/empty-submission-v2```

### Parameters

- FQDN: Target-environment specific CNAME of the Mobile Submission CloudFront distribution 
- V1
  - Authorization required and signatures provided - see [API security](./security.md)
- V2
  - No authorization required, no signature provided in response.

## Example: Empty Submission

- V1
  - Payload is ignored, so any payload will be accepted.
- V2
  - Payload is not accepted.

### Responses
The API always returns OK 200.
