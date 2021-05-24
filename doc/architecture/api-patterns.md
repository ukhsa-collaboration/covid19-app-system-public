
## API Patterns

Here are the general patterns, specific APIs are described in [API contract documentation](./api-contracts)

### Submission

Submission APIs are usually used by the app to submit data to the backend.

* Endpoint schema: ```https://<FQDN>/submission/<payload type>```
* Payload content-type: application/json
* Authorisation: ```Authorization: Bearer <API KEY>```
* One API KEY for all mobile-facing APIs

Note, the port name in the system architecture is defined by ```API Group\API Name```, e.g. ```Submission\Diagnosis Key```.

| API Name | API Group | API Contract | User/Client impact |
| --- | --- | --- | --- |
| Diagnosis Key | Submission | [diagnosis-key-submission.md](api-contracts/mobile-facing/submission/diagnosis-key-submission.md) | In event of positive diagnosis, and user consent to share, the app uploads anonymous exposure keys to the server |
| Virology Testing | Submission | [virology-testing-api.md ](api-contracts/mobile-facing/submission/virology-testing-api.md) | Allows clients to book a coronavirus test using a CTA Token that is passed into the test booking website. Clients can also periodically poll for test results using the CTA Token. New for v3.3 - clients can request a result for a test that was not booked via the app, they will input a CTA token into the app. To support test types other than PCR tests, we have introduced a non-backward compatible version 2 of the API (V1 is now deprecated). |
| Mobile Analytics  | Submission | [analytics-submission-mobile.md](api-contracts/mobile-facing/submission/analytics-submission.md) | Allows clients to submit analytics data daily. Not testable from mobile application. |
| Mobile Analytics Events | Submission | [mobile-analytics-submission.md](api-contracts/mobile-facing/submission/analytics-submission-events.md) | Allows clients to send anonymous epidemiological data to the backend. |
| Isolation Payment | Submission | [isolation-payment-mobile.md](api-contracts/mobile-facing/submission/isolation-payment-submission.md) | Allows clients to request isolation payment using a IPC Token that is passed to the isolation payment website. |


### Distribution

* Endpoint schema: ```https://<FQDN>/distribution/<payload specific>```
* `FQDN`: One (CDN-) hostname for all distribute APIs
* HTTP verb: GET
* Payload content-type: payload specific

| API Name | API Group | API Contract | User/Client impact |
| --- | --- | --- | --- |
| Diagnosis Key | Distribution | [diagnosis-key-distribution.md](api-contracts/mobile-facing/distribution/diagnosis-key-distribution.md) | Clients download exposure keys everyday, valid for 14 days (as per EN API). |
| Exposure Risk Configuration | Distribution | [exposure-risk-configuration.md](api-contracts/mobile-facing/configuration/exposure-risk-configuration.md) | N/A not testable. |
| Postal District Risk Levels | Distribution | [risky-post-district-distribution.md](api-contracts/mobile-facing/distribution/risky-post-district-distribution.md) | List of post districts with risk indicators, used by mobiles to match against the user specified postal district. |
| Identified Risk Venues | Distribution | [risky-venue-distribution.md](api-contracts/mobile-facing/distribution/risky-venue-distribution.md) | List of venues marked as risky which mobile clients poll for daily. If the client has been in a risky venue within the risk period (defined in risky venue upload) an isolation message is displayed. |
| Symptoms Questionnaire | Distribution | [symptoms-questionnaire-distribution.md](api-contracts/mobile-facing/distribution/symptoms-questionnaire-distribution.md) | Symptomatic questionnaire used in the mobile clients. This is set by the NHS Medical Policy team. |
| Self Isolation Configuration | Distribution | [self-isolation-configuration.md](api-contracts/mobile-facing/configuration/self-isolation-configuration.md) | Configuration data used by mobile clients to inform users how long to isolate for and how far back they can select symptom onset. |
| App Availability | Distribution | [app-availability-distribution.md](api-contracts/mobile-facing/distribution/app-availability-distribution.md) | Distribute required OS and app versions (req version > existing => deactivates app) |

### Upload

Upload APIs are usually used by external systems to submit data (files, json) to the backend.

* Endpoint schema: ```https://<FQDN>/upload/<payload type>```
* Payload content type (HTTP header): application/json or text/csv
* Payload size restriction: < 6MB
* All-or-nothing: No partial processing (no row-by-row processing)
* Fast-fail: stop processing after first validation exception
* API GW Rate limit (can be periodically adjusted): 100-150 RPS, max concurrency of 10
* Security for external system upload

| API Name | API Group | API Contract | User/Client impact |
| --- | --- | --- | --- |
| Postal District Risk Levels | Upload | [risky-post-district-upload.md](api-contracts/gateway-facing/risky-post-district-upload.md) | Distribution to mobile. |
| Identified Risk Venues | Upload | [risky-venue-upload.md](api-contracts/gateway-facing/risky-venue-upload.md) | Data source for Risky Venue distribution API. |
| Test Lab Results | Upload | [test-lab-api.md](api-contracts/gateway-facing/test-lab-api.md) | Data source for Virology Testing API allowing mobile to poll for test result. To support test types other than PCR tests, we have introduced a non-backward compatible version 2 of the API (V1 is now deprecated). |
| Token API  | Upload | [token-api.md](api-contracts/gateway-facing/token-api.md) | Data source for CTA token when test outside of the app has been undertaken. Mobile app allows entry of CTA token to confirm receipt of the test outcome. To support test types other than PCR tests, we have introduced a non-backward compatible version 2 of the API (V1 is now deprecated). |
| Isolation Payment | Upload | [isolation-payment-upload.md](api-contracts/gateway-facing/isolation-payment-upload.md) | Used to verify and consume IPC Token |

### Circuit Breaker

Circuit breaker APIs delegate the decision for a risk-based action (e.g. advice self-isolation on exposure notification). The mobile client indicates to the corresponding service that a risk action is to be taken and receives a randomly generated token.

* Endpoint schema: ```https://<FQDN>/circuit-breaker/<risk type specific>```
* HTTP verb: POST
* Payload content-type: application/json
* Payload: related context information (a simple JSON dictionary, i.e. key-value pairs)
* Authorisation: ```Authorization: Bearer <API KEY>```
* One API KEY for all mobile phone-facing APIs

After receiving the token the mobile client polls the backend until it receives a resolution result from the backend.

| API Name | API Group | API Contract | User/Client impact |
| --- | --- | --- | --- |
| Exposure Notification Circuit Breaker | Circuit Breaker | [exposure-notification-circuit-breaker.md](api-contracts/mobile-facing/circuit-breaker/exposure-notification-circuit-breaker.md) | Manual circuit breaker to stop exposure notification alerts in mobile clients on positive diagnosis after client uploads keys. |
| Risk Venues Circuit Breaker | Circuit Breaker | [risky-venue-circuit-breaker.md](api-contracts/mobile-facing/circuit-breaker/exposure-notification-circuit-breaker.md) | Manual circuit breaker to stop exposure notification alerts in mobile clients after a venue is marked as risky from the upload API. |

### Connectors and Exporters

Federation sends and receives Diagnosis Keys to/from operators of other GAEN-compatible systems.

AAE - Advanced Analytics Environment

| API Name | API Group | API Contract | User/Client impact |
| --- | --- | --- | --- |
| Federated Server Connector | Connector | [diagnosis-key-federation.md](api-contracts/key-federation/diagnosis-key-federation.md) | Up/Download federated diagnosis keys. |
| AAE Exporter (Analytics) | Exporter | [analytics-submission-aae-quicksight.md](api-contracts/aae/analytics-submission-aae.md) | Export analytics data. |
| AAE Exporter (Epidemiological) | Exporter | [mobile-analytics-events-aae.md](api-contracts/aae/mobile-analytics-events-aae.md) | Export epidemiological data. |

