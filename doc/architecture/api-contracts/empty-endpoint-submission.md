# Empty Submission Endpoint

API group: [Submission](../guidebook.md#system-apis-and-interfaces)

This API is used for time-based traffic obfuscation.

## HTTP request and response

- Empty Submission: ```POST https://<FQDN>/submission/empty-submission```

### Parameters

- FQDN: Target-environment specific CNAME of the Mobile Submission CloudFront distribution 
- Authorization required and signatures provided - see [API security](./security.md)

## Example: Empty Submission

Payload is ignored, so any payload will be accepted.

### Responses
The API always returns OK 200.
