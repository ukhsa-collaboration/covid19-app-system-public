# NHS COVID-19 App System

This is the repository for the NHS COVID-19 App System, a contact tracing solution that leverages the [Google/Apple Exposure Notification (GAEN)](doc/architecture/gaen.md) API.

This repository includes:

* The [architecture guide](doc/architecture/guidebook.md) for the complete COVID-19 App System
* The [API contracts](doc/architecture/api-contracts/README.md) for all exposed service endpoints, based on a [small number of patterns](doc/architecture/api-patterns.md)
* The [development environment scripts](tools/provisioning/dev/#readme) that set up the development environment which is used to perform all automated tasks, including deployment of target environments
* The [target environment scripts](src/aws/#readme) that set up the target environments and deploy the application code
* The code to automate the building, deploying and testing of the services 
* The implementation of the services

