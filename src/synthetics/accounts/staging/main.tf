module "agapi-syn" {
  source      = "../../"
  base_domain = "staging.svc-test-trace.nhs.uk"
  burst_limit = 5000
  rate_limit  = 10000
}
