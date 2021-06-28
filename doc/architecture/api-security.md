## API Security

The system is secured via HTTPS and requires an authentication token in order to access the submission and upload APIs.  Distribution APIs do not require a token. In addition, in some APIs, a digital signature is included in the header of the response using an asymmetric private key. The mobile client has the corresponding public key to verify the payload. 

A summary of which API provides authentication and digital signing is below:


| API Group | Authentication Required | Signed Response Provided |
|-----------|-------------------------|-----------------------------|
| Distribution | No | Yes |
| Submission | Yes | Yes (except analytics) |
| Upload | Yes | No |

### Authentication

To access an API requiring authentication, the request must be sent via HTTPS with an API key in the `Authorization` header.

| Header | Value |
|--------|---------|
|`Authorization`|`Bearer amJjOjEzZGU2ZTVjLWYyNTMtNGY3Ni05MWRiLWQxMjljMTlkNzI5YQ==`

For example:
```bash
curl -X POST --header 'Authorization: Bearer amJjOjEzZGU2ZTVjLWYyNTMtNGY3Ni05MWRiLWQxMjljMTlkNzI5YQ==' https://<FQDN>/submission
```

#### Submission API
- A single token can access all APIs available in the Submission API group.

#### Upload API
- A single token may only access one API type in the Upload API group.

### Signed Response

Where a signature is provided in the response, values for the two headers _x-amz-meta-signature_ and _x-amz-meta-signature-date_ are returned. Both are used in the signature verification process.

For example:

| Header | Value |
|--------|---------|
|`x-amz-meta-signature`|`keyId="(AWS ACM CMK key id)",signature="(base64 encoded signature)"`
|`x-amz-meta-signature-date`|`Fri, 27 Nov 2020 14:40:14 UTC`|
