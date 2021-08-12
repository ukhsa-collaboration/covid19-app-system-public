data "terraform_remote_state" "core_infra" {
  backend = "s3"

  config = {
    bucket = "tf-state-****-****-****-****-***********"
    key    = "core-infra"
    region = "eu-west-2"
  }
}
