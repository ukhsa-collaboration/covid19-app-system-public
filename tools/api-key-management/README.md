# API Key Management

API keys are used by third party systems to identify themselves when using one of our APIs. We use GPG for key generation, out-of-band verification of public keys for non-dev environments, and encryption of the API key for distributing to third parties.

This README provides step by step instructions and tools to use in the process.

## Third party public key generation

1. Third party generates GPG key pair with name pattern `{3rd party system}-{env}-public-key.txt`, where `env = (dev | integration | prod)` and third party system is e.g. `risk-venue` or `npex-cta`
1. They should generate the key like this:
```
gpg --quick-generate-key "{3rd party system}-{env}-key" rsa4096 sign,auth,encr 2022-01-01
gpg --list-secret-keys "{3rd party system}-{env}-key"
gpg --export --armor "{3rd party system}-{env}-key" > {3rd party system}-{env}-public-key.txt
```
3. Third party public key is send to App System Admin
4. Admin verifies the received public key by reading back (phone call) the fingerprint, using e.g.
```
gpg --import --import-options show-only {3rd party system}-{env}-public-key.txt
```
4. Note: Verification for `dev` keys can be done without phone call (e.g. chat channel)

## App System Admin uses script to generate ephemeral key and encrypted API key

1. Copy third party public key to `/tools/api-key-management/public-keys/{env}`
1. Use `/tools/api-key-management/bin/create-api-key.py --api {api-type-name} --party {3rd party system} --environment {env}`, where `api-type-name = <API type name in system architecture>`, e.g. "testResultUpload" (names are in [ApiName.java](../../src/aws/lambdas/incremental_distribution/src/main/java/uk/nhs/nhsx/core/auth/ApiName.java)), this
   1. generates ephemeral Zuhlke admin PK pair, 
   1. generates API Key and encrypts using the third-parties public key, 
   1. signs it using ephemeral admin private key and
   1. outputs a string to be used to add the new secret to the App System's AWS SecretManager:
````
aws secretsmanager create-secret --name {secret_manager_location} --secret-string '{hashed_value}'
````
3. Then send generated zip file to third party - it contains ephemeral public key and encrypted api key
4. Third party should verify our key by reading back (phone call) the received public key fingerprint given by:
```
gpg --import --import-options show-only tt-api-distribution-2020-07-23-wQjh-public-key.txt
```
4. Note: Verification for `dev` keys can be done without phone call (e.g. chat channel)
5. Then import the ephemeral key
```
gpg --import tt-api-distribution-2020-07-23-wQjh-public-key.txt
```
6. Then decrypt
```
gpg --decrypt {3rd party system}-{env}-20210119.gpg.asc
```

7. Third party should now be able to access the API as specified in the particular API contract.
