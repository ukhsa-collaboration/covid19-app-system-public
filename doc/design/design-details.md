# Design details of the API services

## Tokens and signatures

- [Testkit order, Testresult upload, Testresult polling & Diagnosis Keys submission](details/testkit-order-test-result-key-upload.md)
- [Upload pattern, API keys, Signatures & Distribute pattern](details/upload-pattern-api-keys-signatures-distribute-pattern.md)

## Processing of data

Data processing units (e.g. batch jobs) are not exposed to mobile or 3rd party clients.

They are implemented as AWS Lambda functions that process data from an input store (usually updated by a submission API) and persist the processed output in an output store.

Both stores are implemented as S3 buckets. The processing lambda function is triggered periodically.

## Authorization Secrets

API clients authenticate themselves to the application using the HTTP `Authorization` header.
The value of the header is `Bearer XYZ`, where XYZ is an authentication token encoded as a
[Base64](https://developer.mozilla.org/en-US/docs/Glossary/Base64) UTF8 string.

The authentication token is composed of a key name and a secret, separated by a `:`.

- The key name can be anything.
  - The established convention is to use a path-like name that contains the API "type" (i.e. mobile, health etc.) followed by a name for the key - see
  [Managing Auth Tokens](../howto/ManageTestAuthTokens.md) for details
- The secret is a random string
  - The build system tasks use the `uuid` function of the Ruby
  [SecureRandom](https://docs.ruby-lang.org/en/master/SecureRandom.html) module.

To validate the authentication token, the lambda function

- decodes it from Base64
- breaks it into the key name and secret
- constructs an AWS secret key composed of the API category name and the key name:
  e.g. `/mobile/used_for_tests_abcdef`
- retrieves that secret from the AWS secret manager
  - it will be a password hash of the secret random string
- hashes the secret obtained from the auth token and compares this to the password hash

## Storage of Authorization Secrets

Ordinarily, bearer tokens are not preserved when generated - and for staging and production accounts there is a process of identity verification via a separate channel when communicating the secrets.

An exception is made for tests. The bearer tokens are stored in the AWS secrets manager for retrieval by the test harness.
The test automation ensures that secrets used by tests in staging and prod are removed upon completion of the tests.
