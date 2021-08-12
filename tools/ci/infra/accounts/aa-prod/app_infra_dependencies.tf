data "terraform_remote_state" "core_infra" {
  backend = "s3"

  config = {
    bucket = "tf-state-3531-8916-5293-rand-7427027392"
    key    = "core-infra"
    region = "eu-west-2"
  }
}
