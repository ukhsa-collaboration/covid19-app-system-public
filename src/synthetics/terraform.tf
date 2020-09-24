provider "aws" {
  alias = "synth"
  # We should not set the region explicitly and instead, follow the pattern documented at https://www.terraform.io/docs/configuration/modules.html#passing-providers-explicitly. However, the pattern doesn't work correctly in TF v0.12.29
  region = var.canary_deploy_region
}
