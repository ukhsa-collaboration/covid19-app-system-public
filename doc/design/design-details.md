# Design details of the API services

## Tokens and signatures

- [Testkit order, Testresult upload, Testresult polling & Diagnosis Keys submission](details/testkit-order-test-result-key-upload.md)
- [Upload pattern, API keys, Signatures & Distribute pattern](details/upload-pattern-api-keys-signatures-distribute-pattern.md)

## Processing of data

Data processing units (e.g. batch jobs) are not exposed to mobile or 3rd party clients.

They are implemented as AWS Lambda functions that process data from an input store (usually updated by a submission API) and persist the processed output in an output store.

Both stores are implemented as S3 buckets. The processing lambda function is triggered periodically.
