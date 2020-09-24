# Venue API: Upload identified risk venues

API group: [Upload](../ag-architecture-guidebook#System-APIs-and-Interfaces)

An identified risky venue is a venue that has been tagged by a UK public health authority (e.g. CTAS, manual contact tracers, PHE) as a potential CV-19 hotspot.
 
For the V3 launch we provide a basic https endpoint for CSV file upload which is then processed and distributed as a json file to our api clients.

The responsibility of when updates are fed into the system is completely up to the upload approver, e.g. to PHE or to PHW. This also allows for system indpendent coordination with public comms if needed at the same time.


## Scenario

We expect to receive a new updated list of risky venues between `00:00am and 02:00am UTC` (but also accept late uploads) so distribution to UK users can ideally be finished until early morning (lead time <4h). During summer, UK is one hour ahead, which leads to an acceptable UK time update window `01:00am and 03:00am DST`, so the API client may safely assume always UTC.

After a successful upload the new json file is going to replace the existing one (if it exists) and it is going to be the one that all the clients will see (no merging is done and no old versions are stored). If another file is received the same day it will be distributed to mobile clients with the same latency like earlier uploads: min 2h, best we expect 4h, worst case 6 to 24 hours.


## Endpoints

- Upload a csv file with a full list of identified risk venues
```
POST https://<FQDN>/upload/identified-risk-venues
```
- Payload content-type: ```text/csv```

## Payload Example

```csv
# venue_id, start_time, end_time, message_type, optional_parameter
"8CHARS-Y", "2019-07-04T13:33:03Z", "2019-07-14T23:33:03Z", "M1", ""
"8CHARS-Y", "2019-07-05T13:33:03Z", "2019-07-15T23:33:03Z", "M2", ""
"8CHARS-Y", "2019-07-06T13:33:03Z", "2019-07-16T23:33:03Z", "M3", "07911 123456"
```

## Backwards Compatibility Mode (can be enabled/disabled via feature flag)

If `message_type` or `optional_parameter` are missing (in header row or data rows)
- Fallback for `message_type`: "M1"
- Fallback for `optional_parameter`: "" (empty string)

## Validation

- `venue_id` is the globally unique ID (= poster ID). No duplicates in the file
- `start_time` of considering this venue as a risk venue, valid UTC date (sample: `2019-07-04T23:33:03Z`)
  - `start_time` is yesterday or before, e.g. if we receive on `2019-07-04T00:15:03Z` then the file must not 
  contain entries with `start_time == 2019-07-04T...`
  - before `end_time`  
- `end_time` after which this venue is no longer a risk venue, valid UTC date (sample: `2019-07-04T23:33:03Z`), after `start_time`
- `message_type` valid message type - after applying compatibility mode rules (see "Risky Venue Messages Configuration-Download API") 
- `optional_parameter` string, 0..32 chars - after applying compatibility mode rules

## Response Codes

Default -> [Upload Response Codes](../api-patterns.md#Upload)

## Notes

- Delete/Corrections: the current design allows for **deletion by uploading a subset of venues of a previous upload**. If needed, we may consider more explicit correction/delete mechanisms for V4.

- **TBD** Estimations on expected numbers of risk venues for different contexts like "second wave", "R-value below 1" etc. These contexts and numbers probably come from PHE and/or the epidemiological research.
