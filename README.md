# NHS COVID-19 App System

This is the repository for the NHS COVID-19 App System, a contact tracing solution that leverages the [Google/Apple Exposure Notification (GAEN)](doc/architecture/gaen.md) API.

The repository includes:

* The [architecture guide](doc/architecture/guidebook.md) for the complete COVID-19 App System
* The [API contracts](doc/architecture/api-contracts/README.md) for all exposed service endpoints, based on a [small number of patterns](doc/architecture/api-patterns.md)
* The [development environment scripts](tools/provisioning/dev/README.md) that set up the development environment used to perform all automated tasks, including the deployment of the target environments
* The [target environment scripts](src/aws/README.md) that set up the target environments and deploy the application code
* The [build scripts](tools/build) and [pipelines](tools/ci) for the [build system](./doc/BUILDSYSTEM.md) that automates the building, deploying, testing and releasing of the services 
* The [implementation](src/aws/lambdas/incremental_distribution/cta/src/main/java/uk/nhs/nhsx) of the services, including [unit](src/aws/lambdas/incremental_distribution/cta/src/test/java/uk/nhs/nhsx), [contract](src/aws/lambdas/incremental_distribution/cta/src/test/java/contract) and [smoke](src/aws/lambdas/incremental_distribution/cta/src/test/java/smoke) tests


