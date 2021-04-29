# Troubleshooting

## Authentication headers are already configured

Occurs after trying to generate test secrets (for running sanity-checks).

Means a previous release did not clean up at the end.

Optionally you can clean the secrets and recreate them, but you can continue and the system will use the existing test secrets.

## Currently Selected Workspace Does not Exist

Occurs after an orphan .terraform directory has been left behind in `src/aws/accounts/staging/.terraform/`

```bash
Reading main configuration from
	/workspace/tools/build/system.cfg
terraform init
Terraform initialisation completed in 8.92
rake aborted!
GaudiError: terraform init
Initializing modules...

Initializing the backend...

The currently selected workspace (te-staging) does not exist.
  This is expected behavior when the selected workspace did not have an
  existing non-empty state. Please enter a number to select a workspace:

  1. default

  Enter a value:

Error: Failed to select workspace: input not a valid number

/workspace/tools/build/lib/zuehlke/helpers/execution.rb:29:in `run_command'
```

This means that a deployment is attempted on a workspace that has not been prepared properly:

```bash
rake prepare:deploy:prod
```
