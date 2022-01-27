## Foundations and Security

All APIs adhere to the following **structure and foundational features**

- Basic endpoint schema: `https://<FQDN>/<api-group>`
- `FQDN`: Hostname is different per environment. It is prefixed by the API group, e.g. for `submission-` you'll get `https://submission-<host>/<api-group>`
- We provide API endpoints in different environments from test to prod, to support joint integration testing
- APIs have rate limits, see general information on [AWS API GW rate limits](https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-request-throttling.html)

### Security and authentication for external system access via Upload or Submission

- Cloudfront presents SSL certificate with our host name in it, pinned on the root certificate(s) of the chain for our mobile app
- TLS 1.2 (tls1.2_2018 policy) is used for connection encryption
- [Authorisation with API Key](api-security.md#authentication) specially issued for each API: `Authorization: Bearer <API KEY>`
- Secure process for generating and distributing API Keys relies on out-of-band identity authentication:
  1. We generate and exchange GPG public keys and establish a trust relationship (e.g. phone call) with third party (ext system responsible)
  1. We generate the API key using the third-party's public key, encrypt and send it via mail
- IP range restrictions: API access is restricted to a single IP or a range of given IP addresses
- Our process for IP range restrictions requires exchange of to be used IP addresses/ranges with our Operations & Infrastructure team
- Authentication secrets are not stored anywhere except in the opaque auth header, which is distributed to the respective client application with end-to-end encryption.
- Note that details of the particular security implementation may differ from dev to prod

### Signature of responses via Submission and Distribution

The majority of APIs in the submission and distribution API groups will include a HTTP header containing a signature to support verification of payloads.

- [Signature (ECDSA_SHA_256) of response body](api-security.md#response-signature): `x-amz-meta-signature: keyId="(AWS ACM CMK key id)",signature="(base64 encoded signature)"`
- One Signing Key #1 for all APIs
- Public Key #1 embedded into mobile apps (in obfuscated form)
- Clients (mobile apps) must verify signature

There are some exceptions (such as analytics events) where a signature is not provided because the consumption of the body is not required (e.g. because it is empty).

No response signatures are provided for external facing APIs (not consumed by mobile).

### Signature of diagnosis keys distribution

In addition to the signature header, the payload for diagnosis keys distribution also contains a signature that follows the Apple and Google specification and is required for the exposure notification API to function correctly on the mobile.

- Signed "by design"
- One Signing Key #2 for Diagnosis Key distributions
- Public Key #2 sent to Apple and Google

### Generic HTTP response codes

If not stated differently all APIs use the following **default HTTP response codes**

- `202` if uploaded file successfully processed, response text similar to `successfully processed`
- `403` forbidden (API key invalid), response text similar to `authentication error: <summary, no details>`
- `422` file validation errors, response text similar to `validation error: <details>`
- `429` API GW rate limit, response text similar to `too many requests: <summary, no details>`
- `500` internal errors, response text similar to `internal error: <summary, no details>`
- `503` service in maintenance mode (point-in-time data recovery in progress)

Be prepared for the unpredictable: the cloud services we use (CloudFront, API Gateway, etc.) can return other HTTP error codes in certain error situations. API clients must therefore be prepared for unexpected HTTP error codes in their error handling strategy.

We generally recommend that all API clients have a retry strategy with exponential backoff.
