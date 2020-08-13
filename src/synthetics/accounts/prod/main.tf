module "agapi-syn" {
  source      = "../../"
  base_domain = "prod.svc-test-trace.nhs.uk"
  burst_limit = 5000
  rate_limit  = 10000
}
