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
* The key name is essentially anything
  * For standard API clients, it is the value of the environment variable `API_KEY_NAME`
  at the time that the command `rake secret:nnn` was run,
  where `nnn` is one of the API category names (`mobile`, `testResultUpload` etc.) - see
  [Managing Auth Tokens](../howto/ManageTestAuthTokens.md) for details
  * For Robot and sanity tests, it is `used_for_tests_` followed by a six-digit hex digest of
  the epoch time in seconds at the moment the secret was generated
  * For synthetic canaries, it is simply `synthetic_canary`
* The secret is a random string
  * In Rake, the secret is created using the `uuid` function of the Ruby
  [SecureRandom](https://docs.ruby-lang.org/en/master/SecureRandom.html) module.

To validate the authentication token, the lambda function
* decodes it from Base64
* breaks it into the key name and secret
* constructs an AWS secret key composed of the API category name and the key name:
  e.g. `/mobile/used_for_tests_abcdef`
* retrieves that secret from the AWS secret manager
  * it will be a password hash of the secret random string,
  created using the `rake` command shown above
* hashes the secret obtained from the auth token and compares this to the password hash

## Storage of Authorization Secrets
Ordinarily, the authentication secret is not stored anywhere except in the opaque auth header,
which is distributed to the respective client application with end-to-end encryption.

An exception is made for the test authentication tokens, which are lodged in the
AWS secrets manager for retrieval by the sanity and Robot test clients.
They must be deleted (cleaned up) at the earliest opportunity.

Another exception is made for the synthetic canaries' authentication tokens.
Managing the authentication secrets for synthetic canaries should be
a different role than deploying synthetic canaries, so these are created
and updated using a separate set of `rake` commands
(see [Synthetic Canaries](../howto/SyntheticCanaries.md)).
Whenever one of these secrets is generated and stored in AWS Secret Manager,
the corresponding authorization header is also stored in AWS Secret Manager
in the region where the synthetic canaries will be deployed.
The synthetic canary will retrieve the header from the secret manager
using the secret name `/aaa/synthetic_canary_auth`, where `aaa` is the API category name.
In Staging and Production environments, only the `mobile` API is used by
synthetic canaries - the upload APIs are protected by source IP restriction,
so that synthetic canaries are blocked by the Web Application Firewall (WAF) rules
linked to CloudFront.
