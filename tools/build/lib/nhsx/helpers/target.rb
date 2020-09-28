require_relative "terraform"

module NHSx
  # Methods to query and manipulate the target environment
  module TargetEnvironment
    include NHSx::Terraform
    include NHSx::AWS
    # All the fixed (named) target environments per account: {"account"=>[target_environments]}
    TARGET_ENVIRONMENTS = {
      "dev" => ["ci", "test", "qa", "fnctnl", "demo", "load-test", "extdev", "branch"],
      "staging" => ["staging"],
      "prod" => ["prod"],
    }.freeze
    # All the fixed (named) Analytics target environments per account: {"account"=>[target_environments]}
    ANALYTICS_TARGET_ENVIRONMENTS = {
        "dev" => ["load-test", "branch"],
        "staging" => ["staging"],
        "prod" => ["prod"],
    }.freeze
    # All the fixed (named) DoReTo target environments per account: {"account"=>[target_environments]}
    DORETO_TARGET_ENVIRONMENTS = {
      "dev" => ["test", "branch"]
    }.freeze
    # The parameter name that contains the ARN of the signing key in the SSM parameter store
    SIGNING_KEY_PARAMETER = "/app/kms/SigningKeyArn".freeze
    # The parameter name that contains the ARN of the content signing key in the SSM parameter store
    CONTENT_SIGNING_KEY_PARAMETER = "/app/kms/ContentSigningKeyArn".freeze
    # The name of the secret that contains the API authentication headers used by the tests
    TEST_API_KEY_HEADERS_SECRET = "AuthenticationHeadersForTests".freeze
    # Retrieves the target environment configuration
    def target_environment_configuration(environment_name, account_name, system_config)
      terraform_configuration = File.join(system_config.base, NHSx::Terraform::APP_SYSTEM_ACCOUNTS, account_name)
      target_config = parse_terraform_output(terraform_output(environment_name, terraform_configuration, system_config))
      target_config["auth_headers"] = authentication_headers_for_test(system_config)

      return target_config
    end
    # Retrieves the target environment configuration for the Document Reporting Tool subsystem
    def doreto_target_environment_configuration(environment_name, account_name, system_config)
      terraform_configuration = File.join(system_config.base, NHSx::Terraform::DORETO_ACCOUNTS, account_name)
      target_config = parse_terraform_output(terraform_output(environment_name, terraform_configuration, system_config))

      return target_config
    end

    def signing_key_id(system_config)
      ssm_parameter(SIGNING_KEY_PARAMETER, system_config)
    end

    def content_signing_key_id(system_config)
      ssm_parameter(CONTENT_SIGNING_KEY_PARAMETER, system_config)
    end

    # Retrieves the bearer tokens used for API access to the tests
    #
    # By default it will look in the secrets manager for the AuthenticationHeadersForTests entry.
    #
    # The expected format of the secret's entries is
    #
    # { "mobile" => "authentication header", "testResultUpload"=>"authentication header",
    #   "highRiskVenuesCodeUpload" => "authentication header", "highRiskPostCodeUpload" => "authentication header"}
    def authentication_headers_for_test(system_config)
      scrt = secrets_entry(TEST_API_KEY_HEADERS_SECRET, system_config)
      return {} if scrt == "0"

      JSON.parse(scrt)
    end

    # Returns the list of the names of all secrets in the SystemsManager that match used_for_tests*
    def test_secrets(system_config)
      all_scts = all_secrets(system_config)
      all_scts.select { |item| /used_for_tests/ =~ item }
    end

    def delete_test_secret(secret_name, system_config)
      raise GaudiError, "Will not delete #{secret_name}, it does not match used_for_tests" unless /used_for_tests/ =~ secret_name

      run_command("Schedule #{secret_name} for deletion", NHSx::AWS::Commandlines.delete_secret(secret_name), system_config)
    end

    def zero_test_authentication_headers(system_config)
      run_command("Zero out #{TEST_API_KEY_HEADERS_SECRET}", NHSx::AWS::Commandlines.update_secret(TEST_API_KEY_HEADERS_SECRET, "0"), system_config)
    end
  end
end
