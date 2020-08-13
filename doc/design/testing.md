# Backend system and integration testing

## Test organisation

Tests are grouped together in test suites corresponding to the features the backend services offer.

The test suites are directories under test/robot/suites.

One test suite per feature, e.g. Exposure Notification tests are grouped in [test/robot/suites/exposure_notification](../../test/robot/suites/exposure_notification).

## Target environment specific parameters

All information required in the tests that is target environment specific is generated (currently using ```terraform output```)  by the build system in JSON format and passed to the test suites via command line definition of the TEST_CONFIGURATION_FILE robot variable.
