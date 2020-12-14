# Risky Post Districts Distribution

API group: [Distribution](../guidebook.md#system-apis-and-interfaces)

## HTTP request and response

- Risky Post Districts v2: ```GET https://<FQDN>/distribution/risky-post-districts-v2```

### Parameters
- FQDN: One (CDN-) hostname for all distribute APIs
- Authorization NOT required and signatures provided - see [API security](./security.md)
- Payload content-type: application/json

## Response Payload Example

```json
{
  "postDistricts": {
    "<post district code 1>": "<risk indicator 1>",
    "<post district code 2>": "<risk indicator 2>",
    "<post district code 3>": "<risk indicator 3>",
    …
  },
  "localAuthorities": {
    "<local authority code 1>": "<risk indicator 1>",
    "<local authority code 2>": "<risk indicator 2>",
    "<local authority code 3>": "<risk indicator 3>",
    …
  },
  "riskLevels": {
    "<risk indicator 1>": {
      "colorScheme": "yellow",
      "name": {
        "en": "[postcode] is in…",
        …
      },
      "heading": {
        "en": "Data from NHS shows…",
        …
      },
      "content": {
        "en": "Your local authority…",
        …
      },
      "linkTitle": {
        "en": "Restrictions in your area",
        …
      },
      "linkUrl": {
        "en": "https://gov.uk/somewhere",
        …
      },
      "policyData": {
        "localAuthorityRiskTitle": {
          "en": "[local authority] ([postcode]) is in",
          …
        },
        "heading": {
          "en": "Your area has coronavirus…",
          …
        },
        "content": {
          "en": "Your area is inline…",
          …
        },
        "footer": {
          "en": "Find out more…",
           …
        },
        "policies": [
          {
            "policyIcon": "meetingPeople",
            "policyHeading": {
              "en": "Meeting people",
              …
            },
            "policyContent": {
              "en": "Meeting people",
              …
            },
          }, {
            "policyIcon": "bars",
            "policyHeading": {
              "en": "Bars and pubs",
              …
            },
            "policyContent": {
              "en": "Venues must close…",
              …
            }
        }],
      }
    },
    …
  }
}
```

## Validation

- "policyData" field is optional. All other fields are required.
- Acceptable values for the `<risk indicator>` are defined by the API itself. All `<risk indicator>` values used must provide their definition as part of the `riskLevelIndicators` object.
- Acceptable values for `colorScheme` are `green`, `yellow`, `red`, `amber`, `neutral`.
- Acceptable values for `icon` are `default-icon`, `meeting-people`, `bars-and-pubs`, `worship`, `overnight-stays`, `education`, `travelling`, `exercise`, `weddings-and-funerals`.
  - For any unrecognised values the mobile app will fall back to using `default-icon`
- `heading`, `content`, `name`, `title`, `linkTitle`, and `linkUrl`, `footer`, `policyHeading`, and `policyContent` are localised.
- `heading`, `content`, and `footer` may contain multiple paragraphs of text; they may also be left empty (but must not be null)
- If the string `[postcode]` appears as part of the field `name`; this is a hint for clients to replace it with an actual postcode (e.g. `NR21`).
  - (_Note: usage of the field `name` changed since it was introduced. It is more appropriate to think of it as a “title” now_)
- If the string `[postcode]` appears as part of the field `localAuthorityRiskTitle`; this is a hint for clients to replace it with an actual postcode (e.g. `NR21`).
- If the string `[local authority]` appears as part of the field `localAuthorityRiskTitle`; this is a hint for clients to replace it with an local authority name (e.g. `King’s Lynn and West Norfolk`).
- `linkTitle` should be a few words. The clients may use this as part of a button.

## Notes

- [Post Districts](https://en.wikipedia.org/wiki/List_of_postcode_districts_in_the_United_Kingdom)
