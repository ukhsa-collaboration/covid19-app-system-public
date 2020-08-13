mkio

# Client Controlled Activation

Version V3.0, 2020-08-08

API group: [Submission](../api-patterns.md#Submission)

- Endpoint schema: ```[POST] https://<FQDN>/activation/request```
  - FQDN: Hostname can be different per API
- Authorization: ```Authorization: Bearer <API KEY>```
  - One API KEY for all mobile phone-facing APIs

## Payload Example

Content-Type: application/json

HTTP Method: POST

```json
{
  "activationCode": "1234abcd" 
}
```

Note that the way that the code is *displayed* may be "1234-abcd", but the submission should be "1234abcd"

## Response Example

The API returns no content

## Responses

- The server *may* delay replying for up to 5 seconds, so the client should take this into account when setting timeouts

## Response Codes

200 - OK - Code accepted

400 - Rejected
  - It will not be possible to determine why, but may be
  - Code invalid
  - Code doesn't exist
  - Code expired
  - Unexpected content type

429 - Rate Limited: Code may be valid, retry

500 - Server Issue: Code may be valid, retry

404 - Specifically not used


## Retrying

In case of transient network errors, a code will remain valid for a short period of time, so an interrupted 
network call can be retried safely.
