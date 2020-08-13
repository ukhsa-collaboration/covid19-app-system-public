# Venue API (DEPRECATED / Out of scope for V3/F&F release)

*DEFERED* For V3 we use our default integration pattern for ext system facing endpoints, a simple upload API. However, we may consider implementing it as an internal API to validate the CSV received via the upload API.

An "identified risk venue" is a venue which has been tagged by CTAS (manual contact tracers) as a potential CV-19 hotspot. This API is currently designed as an optional internal ARestful PI. For V3 we will first implement a basic simple upload endpoint according to our integration pattern for data providers.

## Endpoints

- Authorization: ```Authorization: Bearer <API KEY>```
- Endpoints and supported HTTP verbs
  - Submit and query list of identified risk venues: ```(POST | GET) https://<FQDN>/venue/identifiedRiskVenues```
  - Query, update or delete a single identified risk venue: ```(GET | PUT | DELETE) https://<FQDN>/venue/identifiedRiskVenues/<venueId>```
- Payload content-type: ```application/json```

## Payload Examples

### Submit a new list of identified risk venues

```json
POST https://<FQDN>/venue/identifiedRiskVenues
{
  "identifiedRiskVenues" : [
        {
            "venueId": "MAX8CHR1",
            "startTime": "utc-start-date-time",
            "endTime": "utc-end-date-time"
        },
        {
            "venueId": "MAX8CHR2",
            "startTime": "utc-start-date-time",
            "endTime": "utc-end-date-time"
        }]
}
```

Validation

- `identifiedRiskVenues` required array of risk venues
- `venueCategory` required enum to be defined by a national health autority
- `venueId` required string, globally unique (~poster ID)
- `startTime` required risk start time in UTC
- `endTime` required risk end time, not before startTime

Responses

- `Created (201)` if validation OK

### Change the time interval of an existing identified

```json
PUT https://<FQDN>/venue/identifiedRiskVenues/MAX8CHR1
{
  "startTime": "new-utc-date-time",
  "endTime": "new-utc-date-time"
}
```

Validation

- `startTime` not before the existing startTime
- `endTime` required, not before startTime, (?more than 1h?)

Responses

- `OK (200)` if validation OK

### Delete an existing identification

```json
DELETE https://<FQDN>/venue/identifiedRiskVenues/MAX8CHR1
{}
```
