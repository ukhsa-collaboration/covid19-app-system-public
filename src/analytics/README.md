# Analytics

## Terraform scripts for deployment of Analytics target environments

We refer to the Analytics module in the context of deployment as a target environment.

A target environment contains all component implementation necessary to support the analysis of the analytic metrics the App System makes available via the mobile applications.

A target environment requires a correctly configured AWS account. Issues with access, security etc. are handled at the AWS account level. Speak to the Ops&Infra team for further support.

## Accounts and target envrinoments in terraform

AWS accounts are mapped to terraform root modules and are stored in [src/analytics/accounts](./analytics/accounts), one directory per account.

The information required to create a new account setup is the ID of the S3 bucket to use for maintaining the Terraform state.

Target environments are mapped to [Terraform workspaces](https://www.terraform.io/docs/cloud/workspaces/index.html)

An account can contain multiple target environments - but the build system will ensure that only the ```dev``` account is allowed to host multiple target environments.

## Features, patterns and re-usable implementations

In [src/analytics](./analytics/) each TF file is used to group the different design pattern instantiations together (e.g. quicksight, etc.). 
Outputs are placed at the bottom of the file (and need to be replicated in every root module to be visible).

The [patterns identified in the design](../../doc/design/api-patterns.md) are implemented as Terraform modules in [src/analytics/modules](./modules/).

AWS resources are also instantiated and managed in Terraform modules. The technical building block modules are in [src/analytics/libraries](./libraries/).
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
