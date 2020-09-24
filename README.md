# NHS COVID-19 App System

This is the system repository for the NHS COVID-19 App System that uses the Google/Apple exposure notification API.

It includes:

* The [architecture guidebook](doc/architecture/ag-architecture-guidebook.md) for the complete CV-19 App System
* The [API contracts](doc/architecture/api-contracts) for all exposed endpoints based on a small number of [patterns](doc/architecture/ag-architecture-guidebook.md).
* The [provisioning scripts](tools/provisioning/dev) for the development environment
* The scripts that setup the target environment and deploy the application code
* The implementation of all services required to collect data and interact with the mobile devices and external systems
* The code to automate build, deployment and test of the services

### Environment provisioning

#### Infrastructure

The code to target environment provisioning is put under the [src/aws](src/aws) directory.

#### Development

The development environment is available as a docker container image. This should be used to perform all automated
tasks, including deployment of target environments. [Instructions on usage](tools/provisioning/dev/README.md)

