# Analytics export to Advanced Analytics Environment (AAE)

API group: Export (interface exposed by AAE and consumed by the England/Wales National Backend)

## HTTP request and response

- Endpoint: HTTPS PUT ```https://<FQDN>/c19appdata/<filename>.parquet```

### Parameters
- FQDN: Target-Environment specific CNAME of the Advanced Analytics Environment
- Authorization:
    - Mutual TLS authentication (TLS client cert & keystore credentials in SecretsManager)
    - Subscription key (subscription key in SecretsManger)
- Request Headers:
    - Content-Type: `application/json`
    - Ocp-Apim-Subscription-Key: ```SUBSCRIPTION_KEY```

## Scenario

Metrics submitted to the analytics submission API will be stored and then exported to AAE. 

### AAE Export Payload

The request payload is a binary parquet file which needs to follow these rules:
- We MUST NOT remove columns from future parquet files to ensure compatibility with AAE
- We MUST APPEND new columns to future parquet files (last entry in the GlueCatalog) to ensure compatibility with AAE
- Additional columns MUST be optional (column can contain NULL value - to ensure backwards compatibility with old app versions)
