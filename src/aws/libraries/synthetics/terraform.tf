provider "aws" {
  alias  = "synth" # do not name as synthetics since it causes conflict with the synthetics provider
  region = var.region
}
