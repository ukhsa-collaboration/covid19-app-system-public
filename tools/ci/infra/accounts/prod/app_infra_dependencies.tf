data "terraform_remote_state" "core_infra" {
  backend = "s3"

  config = {
    bucket = "tf-state-****-****-****-****-**********"
    key    = "account"
    region = "eu-west-2"
  }
}
