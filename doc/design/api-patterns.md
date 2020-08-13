# API Services: Solution patterns and foundational design

Version V3.0, 2020-08-08

The NHS COVID-19 App System implements three groups of API Services:

- Submission of data from mobile or external systems to the backend
- Upload of data from external system to the backend
- Distribution of submitted or uploaded data to mobile clients

The solution patterns take characteristics of these groups into account. The patterns are applied in [specific API contracts](./api-contracts), provided by the backend and consumed by mobile or external systems. We use an API specification by example approach using .md files.

General information on [AWS API GW rate limits](https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-request-throttling.html)

### URLs

- Basic endpoint schema: ```https://<FQDN>/<api-group>```
- `FQDN`: Hostname is different per environment. It is prefixed by the API group, e.g. for `submission-` you'll get ```https://submission-<host>/<api-group>```
- We provide API endpoints in different environments from test to prod, to support joint integration testing

## Submission

- Endpoint schema: ```https://<FQDN>/submission/<payload type>```
- Payload content-type: application/json
- Authorisation: ```Authorization: Bearer <API KEY>```
- One API KEY for all mobile-facing APIs

## Distribution

- Endpoint schema: ```https://<FQDN>/distribution/<payload specific>```
- `FQDN`: One (CDN-) hostname for all distribute APIs
- HTTP verb: GET
- Payload content-type: payload specific
- Key Distribution
  - Signed "by design"
  - One Signing Key #1 for Diagnosis Key distributions
  - Public Key #1 sent to appgle/google
- All except Diagnosis Key Distribution
  - Signature (ECDSA_SHA_256) of response body: ```x-amz-meta-signature: keyId="(AWS ACM CMK key id)",signature="(base64 encoded signature)"```
  - One Signing Key #2 for all other distributions
  - Public Key #2 embedded into mobile apps (in obfuscated form)
  - Clients (mobile apps) must verify signature

## Circuit breakers

Circuit breaker APIs delegate the decision for a risk-based action (e.g. recommend self-isolation etc.). The are provided in the group of Submission APIs

The mobile client indicates to the corresponding service that a risk action is to be taken and receives a randomly generated token.

- Endpoint schema: ```https://<FQDN>/circuit-breaker/<risk type specific>```
- HTTP verb: POST
- Payload content-type: application/json
- Payload: related context information (a simple JSON dictionary, i.e. key-value pairs)
- Authorisation: ```Authorization: Bearer <API KEY>```
- One API KEY for all mobile phone-facing APIs

After receiving the token the mobile client polls the backend until it receives a resolution result from the backend.

- Endpoint schema: ```https://<FQDN>/circuit-breaker/<risk type specific>/resolution/<token>```
- HTTP verb: GET
- Payload content-type: application/json
- Authorization: ```Authorization: Bearer <API KEY>```
- One API KEY for all mobile phone-facing APIs

## Upload

External systems will upload files periodically

### Pattern

- Endpoint schema: ```https://<FQDN>/upload/<payload type>```
- Payload content-type (HTTP header): application/json or text/csv
- Payload size restriction: < 6MB
- All-or-nothing: No partial processing (no row-by-row processing)
- Fast-fail: stop processing after first validation exception
- API GW Rate limit: 100 RPS, max concurrency of 10

### Security

Based on [ADR-022 Security for external system integration](../../architecture/decisions/ADR022-security-ext-system-integration.md)

- Cloudfront presents SSL certificate with our host name in it, pinned on the root certificate(s) of the chain for our mobile app
- TLS 1.2 (tls1.2_2018 policy) is used for connection encryption
- Authorisation with API Key specially issued for each API: ```Authorization: Bearer <API KEY>```
- Secure process for generating and distributing API Keys relies on out-of-band identity authentication:
  1. We generate and exchange GPG public keys and establish a trust relationship (e.g. phone call) with third party (ext system responsible)
  1. We generate the API key using the third-parties's public key, encrypt and send it via mail
- IP range restrictions: API access is restricted to a single IP or a range of given IP addresses
- Our process for IP range restrictions requires exchange of to be used API Adressess/ranges with our Operations & Infrastructure team
- Note that details of the particular security implementation may differ from dev to prod


### Default HTTP response codes

- `202` iff uploaded file successfully processed, response text similar to `successfully processed`
- `403` forbidden (API key invalid), response text similar to `authentication error: <summary, no details>`
- `422` file validation errors, response text similar to `validation error: <details>`
- `429` API GW rate limit, response text similar to `too many requests: <summary, no details>`
- `500` internal errors, response text similar to `internal error: <summary, no details>` 
