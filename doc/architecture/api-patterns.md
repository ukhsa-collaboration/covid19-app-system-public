
## API Patterns

There are several general patterns of APIs in use:
* [Submission](#Submission)
* [Distribution](#Distribution)
* [Upload](#Upload)
* [Circuit Breaker](#Circuit-Breaker)
* [Connector and Exporter](#Connector-and-Exporter)

Details of specific APIs can be found in [API contract documentation](./api-contracts).

### Submission

Submission APIs are used by the App to submit data to the backend.

* Endpoint schema: ```https://<FQDN>/submission/<payload type>```
* Payload content-type: application/json
* Authorisation: ```Authorization: Bearer <API KEY>```
* One API KEY for all mobile-facing APIs

| API Name | API Group | API Contract | User/Client impact |
| --- | --- | --- | --- |
| Crash Report | Submission | [crash-report-submission.md](api-contracts/mobile-facing/submission/crash-report-submission.md) | Used to send crash reports. |
| Diagnosis Key | Submission | [diagnosis-key-submission.md](api-contracts/mobile-facing/submission/diagnosis-key-submission.md) | Used to send anonymous diagnosis keys in the event of a positive diagnosis, and user consent to share. |
| Empty Endpoint | Submission | [empty-endpoint-submission.md](api-contracts/mobile-facing/submission/empty-endpoint-submission.md) | Used for time-based traffic obfuscation. |
| Isolation Payment | Submission | [isolation-payment-mobile.md](api-contracts/mobile-facing/submission/isolation-payment-submission.md) | Used to create and consume an IPC token, for isolation payment claims. |
| Mobile Analytics  | Submission | [analytics-submission-mobile.md](api-contracts/mobile-facing/submission/analytics-submission.md) | Used to send analytics data, daily. |
| Mobile Analytics Events | Submission | [mobile-analytics-submission.md](api-contracts/mobile-facing/submission/analytics-submission-events.md) | Used to send anonymous epidemiological data. |
| Virology Testing | Submission | [virology-testing-api.md ](api-contracts/mobile-facing/submission/virology-testing-api.md) | Used to book a test and poll for results, using the generated CTA token.  |
### Distribution

Distribution APIs are used by the App to ensure datasets and configurations are kept up-to-date, by polling periodically.

* Endpoint schema: ```https://<FQDN>/distribution/<payload specific>```
* `FQDN`: One (CDN-) hostname for all distribute APIs
* HTTP verb: GET
* Payload content-type: payload specific

| API Name | API Group | API Contract | User/Client impact |
| --- | --- | --- | --- |
| App Availability | Distribution | [app-availability-distribution.md](api-contracts/mobile-facing/distribution/app-availability-distribution.md) | Gets the latest minimum and recommended OS and App versions, for checking on App launch. |
| Diagnosis Key | Distribution | [diagnosis-key-distribution.md](api-contracts/mobile-facing/distribution/diagnosis-key-distribution.md) | Gets the latest diagnosis keys, valid for 14 days (as per EN API), for matching against the user's recent contacts. |
| Identified Risk Venues | Distribution | [risky-venue-distribution.md](api-contracts/mobile-facing/distribution/risky-venue-distribution.md) | Gets the latest list of venues identified as risky, for matching against the user's venue check-ins. |
| Local Messages | Distribution | [local-messages-distribution.md](api-contracts/mobile-facing/distribution/local-messages-distribution.md) | Gets the latest messages, specific to local authorities, for matching against the user's local authority. |
| Postal District Risk Levels | Distribution | [risky-post-district-distribution.md](api-contracts/mobile-facing/distribution/risky-post-district-distribution.md) | Gets the latest risk indicator for postal districts, for matching against the user's postal district. |
| Symptoms Questionnaire | Distribution | [symptoms-questionnaire-distribution.md](api-contracts/mobile-facing/distribution/symptoms-questionnaire-distribution.md) | Gets the latest symptomatic questionnaire and advice, set by the NHS Medical Policy team, for use by users reporting symptoms. |
| |  |  |  |
| Exposure Risk Configuration | Distribution | [exposure-risk-configuration.md](api-contracts/mobile-facing/configuration/exposure-risk-configuration.md) | Gets the latest configuration for exposure risk analysis. |
| Identified Risk Venues Configuration | Distribution | [risky-venue-configuration.md](api-contracts/mobile-facing/configuration/risky-venue-configuration.md) | Gets the latest configuration for venue risk notification e.g. how long a user can book a test after visiting a risky venue. |
| Self Isolation Configuration | Distribution | [self-isolation-configuration.md](api-contracts/mobile-facing/configuration/self-isolation-configuration.md) | Gets the latest configuration for self isolation e.g. how long a user needs to isolate for. |

### Upload

Upload APIs are used by external systems to submit data (files, json) to the backend, usually for subsequent distribution.

* Endpoint schema: ```https://<FQDN>/upload/<payload type>```
* Payload content type (HTTP header): application/json or text/csv
* Payload size restriction: < 6MB
* All-or-nothing: No partial processing (no row-by-row processing)
* Fast-fail: stop processing after first validation exception
* API GW Rate limit (can be periodically adjusted): 100-150 RPS, max concurrency of 10
* Security for external system upload

| API Name | API Group | API Contract | User/Client impact |
| --- | --- | --- | --- |
| Identified Risk Venues | Upload | [risky-venue-upload.md](api-contracts/service-facing/risky-venue-upload.md) | Source of data for the Risky Venue distribution API. |
| Isolation Payment | Upload | [isolation-payment-upload.md](api-contracts/service-facing/isolation-payment-upload.md) | Used by the Isolation Payment Gateway to verify and consume the IPC token during isolation payment claims. |
| Postal District Risk Levels | Upload | [risky-post-district-upload.md](api-contracts/service-facing/risky-post-district-upload.md) | Source of data for the Postal District Risk Levels distribution API. |
| Test Lab Results | Upload | [test-lab-api.md](api-contracts/service-facing/test-lab-api.md) | Source of data for the Virology Testing API to satisfy it's polling for a test result booked within the App. |
| Token API  | Upload | [token-api.md](api-contracts/service-facing/token-api.md) | Used by the notification service (BSA for England, NWIS for Wales) to create a CTA token for sending with a test result, when a test is booked outside the App.|

### Circuit Breaker

Circuit breaker APIs delegate the decision for a risk-based action e.g. to advise self-isolation on exposure notification. 

* Endpoint schema: ```https://<FQDN>/circuit-breaker/<risk type specific>```
* HTTP verb: POST
* Payload content-type: application/json
* Payload: related context information (a simple JSON dictionary, i.e. key-value pairs)
* Authorisation: ```Authorization: Bearer <API KEY>```
* One API KEY for all mobile phone-facing APIs

After receiving the token the mobile client polls the backend until it receives a resolution result from the backend.

| API Name | API Group | API Contract | User/Client impact |
| --- | --- | --- | --- |
| Exposure Notification Circuit Breaker | Circuit Breaker | [exposure-notification-circuit-breaker.md](api-contracts/mobile-facing/circuit-breaker/exposure-notification-circuit-breaker.md) | Manual circuit breaker to stop exposure notification alerts in mobile clients following a recent contact match with distributed diagnosis keys. |
| Risk Venues Circuit Breaker | Circuit Breaker | [risky-venue-circuit-breaker.md](api-contracts/mobile-facing/circuit-breaker/exposure-notification-circuit-breaker.md) | Manual circuit breaker to stop user notification alerts in mobile clients, following a check-in match on the mobile client with a venue identified as risky. |

### Connector and Exporter

Connectors and Exporters are integrations with third-parties.

In this case, the third parties are:
NearForm Federation API - sends and receives Diagnosis Keys to/from operators of other GAEN-compatible systems.
AAE - Advanced Analytics Environment.

| API Name | API Group | API Contract | User/Client impact |
| --- | --- | --- | --- |
| AAE Exporter (Analytics) | Exporter | [analytics-submission-aae-quicksight.md](api-contracts/aae/analytics-submission-aae.md) | Export of analytics data to AAE. |
| AAE Exporter (Epidemiological) | Exporter | [mobile-analytics-events-aae.md](api-contracts/aae/mobile-analytics-events-aae.md) | Export epidemiological data to AAE. |
| Federated Server Connector (Exposure Notification) | Connector | [diagnosis-key-federation.md](api-contracts/diagnosis-key-federation.md) | Upload/download of federated diagnosis keys shared by nations to/from Nearform API. |

