# Security

The system is secured via HTTPS and require an authentication token in order to access the submission and upload APIs whereas distribution APIs do not require a token. In addition, in some APIs, a signature is included in the header of the response using an asymmetric private key. The client (mobile) should use the corresponding public key to verify the payload. A summary of which API provides authentication and signatures is below.


| API Group | Authentication Required | Response Signature Provided |
|-----------|-------------------------|-----------------------------|
| Distribution | No | Yes |
| Submission | Yes | Yes (except analytics) |
| Upload | Yes | No |

## Authentication

To access an API requiring authentication, the request must be sent via HTTPS with an API key in the `Authorization` header.

| Header | Value |
|--------|---------|
|`Authorization`|`Bearer amJjOjEzZGU2ZTVjLWYyNTMtNGY3Ni05MWRiLWQxMjljMTlkNzI5YQ==`

e.g.
```bash
curl -X POST --header 'Authorization: Bearer amJjOjEzZGU2ZTVjLWYyNTMtNGY3Ni05MWRiLWQxMjljMTlkNzI5YQ==' https://<FQDN>/submission
```

### Submission API
- A single token can access all APIs available in the Submission API group.

### Upload API
- A single token may only access one API type in the Upload API group.

## Response Signature

Where a signature is provided in the response, two header values are returned. Both are used in the signature verification process.

| Header | Value |
|--------|---------|
|`x-amz-meta-signature`|`keyId="(AWS ACM CMK key id)",signature="(base64 encoded signature)"`
|`x-amz-meta-signature-date`|`Fri, 27 Nov 2020 14:40:14 UTC`|