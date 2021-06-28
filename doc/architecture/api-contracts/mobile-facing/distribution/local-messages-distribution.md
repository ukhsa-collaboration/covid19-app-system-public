# Local Messages Distribution

> API Pattern: [Distribution](../../../api-patterns.md#distribution)

## HTTP Request and Response

- Content module: ```GET https://<FQDN>/distribution/local-messages```

### Parameters

- FQDN: One (CDN) hostname for all distributed APIs
- Authorization NOT required and signatures provided - see [API security](../../../api-security.md)
- Payload content-type: `application/json`

## Scenario
- Client downloads the local messages json periodically
- Client checks if there is a message that needs to be displayed for a specific local authority
- Client uses:
    - `las.<la-code>` to get the first message from the array (mobile app versions backwards compatibility)
    - `<message>.type` to trigger corresponding ui journey displaying the content provided
        1. `type` possible values are: `notification`
    - `updated` value to check when the content was generated and published
    - `<message>.contentVersion` to check updates to the content of a certain message (e.g. typo in the content that was fixed and published again)
    - `<message>.translations.<iso-639-1>` e.g. "en" to find the correct language translation
    - `<message>.translations.<iso-639-1>.head` content title
        - Text replacements apply
    - `<message>.translations.<iso-639-1>.body` content description
        - Text replacements apply
    - `<message>.translations.<iso-639-1>.content` content elements:
        - `type` type of element to be rendered:
            1. `para` means that the following elements are wrapped in a paragraph
        - `text` block of text:
            - Text replacements apply
        - `link` url
            - Text replacements DO NOT apply
        - `linkText` block of text that describes the url
            - Text replacements DO NOT apply
- Client performs placeholder substitution on the message content
    - replaces `[postcode]` with device selected postcode
    - replaces `[local authority]` with device selected local authority

    
### Get local messages 
`GET https://<FQDN>/distribution/local-messages`

#### Response Payload Example (structure)
```json
{
    "las": {
        "ABCD1234": ["message1"]
    },
    "messages": {
        "message1": {
            "type": "notification",
            "updated": "2021-05-19T14:59:13Z",
            "contentVersion": 1,
            "translations": {
                "en": {
                    "head": "[postcode]: New Coronavirus Information",
                    "body": "Here is a message applicable to [local authority]",
                    "content": [
                        {
                            "type": "para",
                            "text": "We have information for people in [postcode]. Here are some key pieces of information to help you stay safe"
                        },
                        {
                            "type": "para",
                            "text": "We have information for people in  [postcode]. Here are some key pieces of information to help you stay safe",
                            "link": "http://example.com",
                            "linkText": "Click me for more information"
                        }
                    ]
                }
            }
        }
    }
}
```


