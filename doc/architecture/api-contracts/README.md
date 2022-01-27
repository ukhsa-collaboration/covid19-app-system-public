## API Contracts

> See also  [API Patterns](../api-patterns.md),  [API Foundations](../api-foundation.md) and [API Security](../api-security.md)

The following API contacts are in in use within the COVID-19 App System:

 - __mobile-facing__
   - __circuit-breaker__
     - [exposure-notification-circuit-breaker.md](mobile-facing/circuit-breaker/exposure-notification-circuit-breaker.md)
     - [risky-venue-circuit-breaker.md](mobile-facing/circuit-breaker/risky-venue-circuit-breaker.md)
   - __configuration__
     - [exposure-risk-configuration.md](mobile-facing/configuration/exposure-risk-configuration.md)
     - [risky-venue-configuration.md](mobile-facing/configuration/risky-venue-configuration.md)
     - [self-isolation-configuration.md](mobile-facing/configuration/self-isolation-configuration.md)
   - __distribution__
     - [app-availability-distribution.md](mobile-facing/distribution/app-availability-distribution.md)
     - [diagnosis-key-distribution.md](mobile-facing/distribution/diagnosis-key-distribution.md)
     - [local-messages-distribution.md](mobile-facing/distribution/local-messages-distribution.md)
     - [postal-district-risk-level-distribution.md](mobile-facing/distribution/postal-district-risk-level-distribution.md)
     - [risky-venue-distribution.md](mobile-facing/distribution/risky-venue-distribution.md)
     - [symptoms-questionnaire-distribution.md](mobile-facing/distribution/symptoms-questionnaire-distribution.md)
     - [local-covid-stats-distribution.md](mobile-facing/distribution/local-covid-stats-distribution.md)
   - __submission__
     - [analytics-event-submission.md](mobile-facing/submission/analytics-event-submission.md)
     - [analytics-submission.md](mobile-facing/submission/analytics-submission.md)
     - [crash-report-submission.md](mobile-facing/submission/crash-report-submission.md)
     - [diagnosis-key-submission.md](mobile-facing/submission/diagnosis-key-submission.md)
     - [empty-submission.md](mobile-facing/submission/empty-submission.md)
     - [isolation-payment-claim-token-submission.md](mobile-facing/submission/isolation-payment-claim-token-submission.md)
     - [virology-test-order-submission.md](mobile-facing/submission/virology-test-order-submission.md)
     - [virology-test-result-token-submission.md](mobile-facing/submission/virology-test-result-token-submission.md)
 - __service-facing__
   - __connector__
     - [diagnosis-key-federation-connector.md](service-facing/connector/diagnosis-key-federation-connector.md)
   - __exporter__
     - [analytics-aae-exporter.md](service-facing/exporter/analytics-aae-exporter.md)
     - [analytics-event-aae-exporter.md](service-facing/exporter/analytics-event-aae-exporter.md)
   - __upload__
     - [isolation-payment-claim-token-upload.md](service-facing/upload/isolation-payment-claim-token-upload.md)
     - [postal-district-risk-level-upload.md](service-facing/upload/postal-district-risk-level-upload.md)
     - [risky-venue-upload.md](service-facing/upload/risky-venue-upload.md)
     - [virology-test-result-token-upload.md](service-facing/upload/virology-test-result-token-upload.md)
     - [virology-test-result-upload.md](service-facing/upload/virology-test-result-upload.md)
