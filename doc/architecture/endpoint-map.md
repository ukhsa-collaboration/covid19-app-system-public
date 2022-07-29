# Endpoint Map

This document is for support engineers. It maps which API endpoints are exposed by which Lambdas. Knowing which Lambda
exposes an endpoint makes it easier to find the logs for a given endpoint. This is because logs are grouped by the
Lambda they originated from.

# Lambdas

The following is a list of lambdas and the endpoints they control.
All lambda names start with a workspace name. For Production that is always `te-prod`.

## Analytics / Puddash

### \<workspace\>-analytics-events-sub

Endpoints provided by `uk.nhs.nhsx.analyticsevents.AnalyticsEventsHandler`

* `/submission/mobile-analytics-events`
* `/submission/mobile-analytics-events/health`

### \<workspace\>-analytics-ingest-proc

Endpoints provided by `uk.nhs.nhsx.analyticssubmission.AnalyticsSubmissionQueuedHandler`

* `/submission/mobile-analytics`
* `/submission/mobile-analytics/health`

## Backend

### \<workspace\>-crash-reports-sub

Endpoints provided by `uk.nhs.nhsx.crashreports.CrashReportsHandler`

* `/submission/crash-reports`

### \<workspace\>-diagnosis-keys-sub

Endpoints provided by `uk.nhs.nhsx.diagnosiskeyssubmission.DiagnosisKeySubmissionHandler`

* `/submission/diagnosis-keys`
* `/submission/diagnosis-keys/health`

### \<workspace\>-empty-submission-sub

Endpoints provided by `uk.nhs.nhsx.emptysubmission.EmptySubmissionHandler`

* `/submission/empty-submission`

### \<workspace\>-exposure-notification-circuit-breaker

Endpoints provided by `uk.nhs.nhsx.circuitbreakers.ExposureNotificationHandler`

* `/circuit-breaker/exposure-notification/request`
* `/circuit-breaker/exposure-notification/resolution`
* `/circuit-breaker/exposure-notification/health`

### \<workspace\>-ipc-token-api

Endpoints provided by `uk.nhs.nhsx.isolationpayment.IsolationPaymentUploadHandler`

* `/isolation-payment/ipc-token/consume-token`
* `/isolation-payment/ipc-token/verify-token`
* `/isolation-payment/health`

### \<workspace\>-ipc-token-order

Endpoints provided by `uk.nhs.nhsx.isolationpayment.IsolationPaymentOrderHandler`

* `/isolation-payment/ipc-token/create`
* `/isolation-payment/ipc-token/update`
* `/isolation-payment/health`

### \<workspace\>-risky-post-districts-upload

Endpoints provided by `uk.nhs.nhsx.highriskpostcodesupload.HighRiskPostcodesUploadHandler`

* `/upload/high-risk-postal-districts`
* `/upload/high-risk-postal-districts/health`

### \<workspace\>-risky-venues-circuit-breaker

Endpoints provided by `uk.nhs.nhsx.circuitbreakers.RiskyVenueHandler`

* `/circuit-breaker/venue/request`
* `/circuit-breaker/venue/resolution`
* `/circuit-breaker/venue/health`

### \<workspace\>-risky-venues-upload

Endpoints provided by `uk.nhs.nhsx.highriskvenuesupload.HighRiskVenuesUploadHandler`

* `/upload/identified-risk-venues`
* `/upload/identified-risk-venues/health`

### \<workspace\>-virology-sub

Endpoints provided by `uk.nhs.nhsx.virology.VirologySubmissionHandler`

* `/virology-test/health`
* `/virology-test/home-kit/order`
* `/virology-test/home-kit/register`
* `/virology-test/results`
* `/virology-test/cta-exchange`
* `/virology-test/v2/order`
* `/virology-test/v2/results`
* `/virology-test/v2/cta-exchange`

### \<workspace\>-virology-upload

Endpoints provided by `uk.nhs.nhsx.virology.VirologyUploadHandler`

* `/upload/virology-test/health`
* `/upload/virology-test/npex-result`
* `/upload/virology-test/fiorano-result`
* `/upload/virology-test/eng-result-tokengen`
* `/upload/virology-test/wls-result-tokengen`
* `/upload/virology-test/v2/npex-result`
* `/upload/virology-test/v2/fiorano-result`
* `/upload/virology-test/v2/eng-result-tokengen`
* `/upload/virology-test/v2/wls-result-tokengen`
* `/upload/virology-test/v2/eng-result-tokenstatus`
* `/upload/virology-test/v2/wls-result-tokenstatus`
