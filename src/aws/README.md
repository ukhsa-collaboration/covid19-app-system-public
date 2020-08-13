# Terraform scripts for deployment of AGAPI target environments

We refer to the AGAPI system in the context of deployment as a target environment.

A target environment contains all API implementation necessary to support the mobile applications, the services that integrate with third party systems (e.g. QR code data etc.) and any supporting services for operation (e.g. analytics stores, control panel etc.).

A target environment requires a correctly configured AWS account. Issues with access, security etc. are handled at the AWS account level.

## Accounts and target envrinoments in terraform

AWS accounts are mapped to terraform root modules and are stored in [src/aws/accounts](./aws/accounts), one directory per account.

The information required to create a new account setup is the ID of the S3 bucket to use for maintaining the Terraform state.

Target environments are mapped to [Terraform workspaces](https://www.terraform.io/docs/cloud/workspaces/index.html)

An account can contain multiple target environments - but the build system will ensure that only the ```dev``` account is allowed to host multiple target environments.

## Features, patterns and re-usable implementations

In [src/aws](./aws/) each TF file is used to group the different design pattern instantiations together (e.g. distributions, submissions etc.). 
Outputs are placed at the bottom of the file (and need to be replicated in every root module to be visible).

The [patterns identified in the design](../../doc/design/api-patterns.md) are implemented as Terraform modules in [src/aws/modules](./modules/).

AWS resources are also instantiated and managed in Terraform modules. The technical building block modules are in [src/aws/libraries](./libraries/).
This allows us to maintain them centrally following the "single source of truth" principle.

## Resource identifiers and naming

Always prefix a resource identifier with the terraform workspace.

As an example, the modules implementing the design patterns use the following way internally (here an  example from the processing module).

```hcl
locals {
  identifier_prefix = "${terraform.workspace}-${var.name}-processing"
}
```

All modules should handle prefixing of identifiers internally - do not pass values to a module's variables that include the workspace.

### Lambda functions

The lambda terraform library requires a function name that is unique within the account.

Use the terraform workspace as a prefix when parametrizing the lambda function names.
