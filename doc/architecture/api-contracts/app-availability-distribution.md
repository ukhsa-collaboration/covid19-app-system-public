# App availability Distribution

API group: [Distribution](../guidebook.md#system-apis-and-interfaces)

## HTTP request and response

- Availability iOS: ```GET https://<FQDN>/distribution/availability-ios```
- Availability Android: ```GET https://<FQDN>/distribution/availability-android```

### Parameters

- FQDN: One (CDN-) hostname for all distribute APIs
- Authorization NOT required and signatures provided - see [API security](./security.md)
- Payload content-type: application/json

## Scenario
- Client downloads the app availability json periodically to check if the app is available.
- Client uses the `minimumOSVersion` (iOS) or `minimumSDKVersion` (Android) to determine if the app should disable itself and ask for an upgrade.
- Client uses the `minimumAppVersion` to determine if the app should disable itself. If app determines a new version is available through other means, it may offer an app upgrade. **For Android that must be a single integer**.
- [Only iOS] Client uses the `recommendedOSVersion` to determine whether a recommended OS update is available or not. If it is the case, it asks the user to go to the Phone settings to upgrade their OS.
- Client uses the `recommendedAppVersion` to determine whether a recommended App update is available or not. If it is the case, it asks the user regularly to upgrade their App in the App store. **For Android that must be a single integer**
 
## Example: Availability iOS
`GET https://<FQDN>/distribution/availability-ios`

### Response Example (structure)
```json
{
   "minimumOSVersion": {
      "value": "13.5.0",
      "description": {
         "en-GB": "In order to run this app on your device, you need to update your device software. Please go to settings and update it from there."
      }
   },
   "recommendedOSVersion": {
      "value": "14.0.0",
      "title": {
         "en-GB": "Recommended device software update."
      },
      "description": {
         "en-GB": "To improve the exposure's accuracy to COVID-19, you need to update your device software. Please go to settings and update it from there."
      }
   },
   "minimumAppVersion": {
      "value": "3.0.0",
      "description": {
         "en-GB":"In order to run this app on your device, you need to update it to the latest version. Please go to the app store and update it from there."
      }
   },
   "recommendedAppVersion": {
      "value": "3.8.0",
      "title": {
         "en-GB": "Recommended App update."
      },
      "description": {
         "en-GB": "In order to profit from the latest App features, we recommend you to update it to the latest version. Please go to the app store and update it from there."
      }
   }
}

```

## Example: Availability Android
`GET https://<FQDN>/distribution/availability-android`

### Response Example (structure)
```json
{
  "minimumSDKVersion": {
    "value": 23,
    "description": {
      "en-GB": "In order to run this app on your device, you need to update your device software. Please go to settings and update it from there."
    }
  },
  "minimumAppVersion": {
    "value": 8,
    "description": {
      "en-GB": "In order to run this app on your device, you need to update it to the latest version. Please go to the app store and update it from there."
    }
  },
  "recommendedAppVersion": {
      "value": 9,
      "title": {
         "en-GB":"Recommended App update."
      },
      "description": {
         "en-GB":"In order to profit from the latest App features, we recommend you to update it to the latest version. Please go to the app store and update it from there."
      }
   }
}
```
