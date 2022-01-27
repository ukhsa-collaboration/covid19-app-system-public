## API Security

### Overview

The system is secured via HTTPS and requires an authentication token, referred to as an API key,  in order to access the Submission and Upload APIs.  Distribution APIs do not require a token.

In addition, all distribution APIs, and most submission APIs, have a digital signature included in the header of the response.  An asymmetric private key is used to generate the signature. The mobile App has the corresponding public key to verify the payload has not been tampered with.

### Summary

A summary of which API provides authentication and digital signing is below:

| API Group | API Key Required | Digitally Signed Response |
|-----------|-------------------------|-----------------------------|
| Distribution | No | Yes |
| Submission | Yes | Yes (except analytics) |
| Upload | Yes | No |

### Authentication

To access an API requiring authentication, the request must be sent via HTTPS with an API key in the `Authorization` header.

| Header | Value |
|--------|---------|
|`Authorization`|Bearer `<api key>`

Where `<api key>` is the base64 of encoding of `<key name>`:`<key value>`

For example:

```bash
curl -X POST --header 'Authorization: Bearer amJjOjEzZGU2ZTVjLWYyNTMtNGY3Ni05MWRiLWQxMjljMTlkNzI5YQ==' https://<FQDN>/submission
```

#### API Keys

Clients (mobile App, 3rd party) pass the API key provided to them. The mobile App stores the embedded API key in obfuscated form.

API keys are stored in the AWS Secrets Manager using the following convention:

| Key | Value |
|--------|---------|
|/`<api name>`/`<key name>` | bcrypt.hash(`<key value>`, 4096) |

The `<key name>` can be anything.
The `<key value>` is a random string secret.

Ordinarily, api keys are not preserved when generated - and for staging and production accounts there is a process of identity verification via a separate channel when communicating the secrets.

An exception is made for tests. The api keys are stored in the AWS secrets manager for retrieval by the test harness. The test automation ensures that secrets used by tests in staging and prod are removed upon completion of the tests.

##### Submission API

- A single token can access all APIs available in the Submission API group.

##### Upload API

- A single token may only access one API type in the Upload API group.

### Digitally Signed Response

Where a signature is provided in the response, values for the two headers _x-amz-meta-signature_ and _x-amz-meta-signature-date_ are returned. Both are used in the signature verification process.

For example:

| Header | Value |
|--------|---------|
|`x-amz-meta-signature`|keyId="`<AWS ACM CMK key id>`", signature="`<base64 encoded signature>`"
|`x-amz-meta-signature-date`|`Fri, 27 Nov 2020 14:40:14 UTC`|
