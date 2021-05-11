# Mobile crash reports submission

API group: [Submission](../../../guidebook.md#system-apis-and-interfaces)

## HTTP request and response

- Submit mobile crash reports: ```POST https://<FQDN>/submission/crash-reports```

### Parameters

- FQDN: Target-environment specific CNAME of the Mobile Submission CloudFront distribution 
- Authorization required and signatures NOT provided - see [API security](../../security.md)
- Extensibility: new Key-Value Pairs can be added to the request without breaking request processing

## Scenario

Mobile app sends crash reports as part of its background task.

The backend receives the payload (see example below) from the mobile client and stores it.

This data is preserved for a limited amount of time (7 days) to allow further investigation of the exception.

## Example: Submit 

### Android Request Payload Example
```json
{
  "exception": "android.app.RemoteServiceException",
  "threadName": "MainThread",
  "stackTrace": "android.app.RemoteServiceException: Here is the elusive exception message that we really need to capture,
    at android.app.ActivityThread$H.handleMessage (ActivityThread.java:2211)
    at android.os.Handler.dispatchMessage (Handler.java:106)
    at android.os.Looper.loop (Looper.java:246)
    at android.app.ActivityThread.main (ActivityThread.java:8419)
    at java.lang.reflect.Method.invoke (Native Method)
    at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run (RuntimeInit.java:596)
    at com.android.internal.os.ZygoteInit.main (ZygoteInit.java:1130)"
}
```

#### Validation
The exception value will be validated, the supported values are:
 * "android.app.RemoteServiceException"

All payload values will be sanitised to avoid XSS malicious urls (remove http:// and https://)

#### Feature flag
Logging the payload data can be switched off, e.g. api is under attack or this endpoint is no longer needed

### Responses
| Status Code | Description |
| --- | --- |
| 200 | Submission processed | Exception field is not recognised by the backend (prevent api abuse)
| 400 | Bad request could not process payload
