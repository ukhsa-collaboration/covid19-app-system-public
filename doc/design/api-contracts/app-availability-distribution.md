# App availability Distribution

Version V3.0, 2020-08-08

API group: [Distribution](../api-patterns.md#Distribution)

- Endpoint schema: ```https://<FQDN>/distribution/availability-<Platform>```
  - FQDN: One (CDN-) hostname for all distribute APIs
  - Platform: `ios` and `android`.
- Payload content-type: application/json
- Signature (ECDSA_SHA_256) of response body: ```x-amz-meta-signature: keyId="(AWS ACM CMK key id)",signature="(base64 encoded signature)"```

## Scenario
- Client downloads the app availability json periodically to check if the app is available.
- Client uses the `minimumOSVersion` (iOS) or `minimumSDKVersion` (Android) to determine if the app should disable itself and ask for an upgrade.
- Client uses the `minimumAppVersion` to determine if the app should disable itself. If app determines a new version is available through other means, it may offer an app upgrade. **For Android that must be a single integer**.
 
## Payload Example

### Initial values

Initially, these endpoints return our intended configuration for the launch.

#### `/availability/ios`

```json
{
  "minimumOSVersion": {
    "value": "13.5.0",
    "description": {
      "en-GB": ""
    }
  },
  "minimumAppVersion": {
    "value": "3.0.0",
    "description": {
      "en-GB": ""
    }
  },
}
```

#### `/availability/android`

```json
{
  "minimumSDKVersion": {
    "value": 23,
    "description": {
      "en-GB": ""
    }
  },
  "minimumAppVersion": {
    "value": 8,
    "description": {
      "en-GB": ""
    }
  },
}
```

### Example: Responding to an incident

When responding to an incident in version `3.1.3`, we decide that the app must be disabled immediately. We can do it like so:

```json
{
  ...
  "minimumAppVersion": {
    "value": "3.2.0",
    "description": {
      "en-GB": "To protect your privacy, you can no longer use this version of the app."
    }
  },
}
```

The clients will initially show this message as a complete blocker as there is nothing the user can do (no updates are available yet).

When we publish the `3.2.0` to the stores, the app will detect this version, and may show additional UI to make it easier for the user to update the app.

### Example: Demising older versions of the OS

In the future, we may decide that it is necessary to reduce the minimum supported OS version. We can indicate it like so:

```json
{
  "minimumOSVersion": {
    "value": "14",
    "description": {
      "en-GB": "Your operating system version is no longer supported. Please update to the latest version of the operating system."
    }
  },
  "minimumAppVersion": {
    "value": "5.0.0",
    "description": {
      "en-GB": "To protect your privacy, you can no longer use this version of the app."
    }
  },
}
```

Note that indicating the minimum OS version is necessary to avoid confusing user messages: If the user can not install `5.0.0` on their phone, it would be incorrect for us to show them a message stating that they must upgrade to `5.0.0`.

### Example: Demising the app

App demise is supported just like security and policy force upgrades. We set the minimum version to a non-existant version, along with the appropriate message.

```json
{
  "minimumAppVersion": {
    "value": "99",
    "description": {
      "en-GB": "This app is no longer in use. Thanks for helping you community."
    }
  },
}
```
