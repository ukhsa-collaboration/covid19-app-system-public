require_relative "terraform"
require_relative "aws"

module NHSx
  # Methods to query and manipulate the target environment
  module TargetEnvironment
    include NHSx::Terraform
    include NHSx::AWS

    # The location for the account used by a component of the system for targeting a temporary deployment environment
    # relative to the root of the repository
    APP_SYSTEM_ACCOUNTS = "src/aws/accounts".freeze
    ANALYTICS_ACCOUNTS = "src/analytics/accounts".freeze
    DORETO_ACCOUNTS = "src/documentation_reporting_tool/infrastructure/accounts".freeze
    PUBDASH_ACCOUNTS = "src/pubdash/infrastructure/accounts".freeze

    # The location for the terraform configuration of the account used for hosting temporary deployment environments
    # relative to the root of the repository
    CTA_DEV_ACCOUNT = "src/aws/accounts/dev".freeze
    ANALYTICS_DEV_ACCOUNT = "src/analytics/accounts/dev".freeze
    PUBDASH_DEV_ACCOUNT = "src/pubdash/infrastructure/accounts/dev".freeze
    DORETO_DEV_ACCOUNT = "src/documentation_reporting_tool/infrastructure/accounts/dev".freeze

    # Define the API endpoint category names and corresponding rake targets - see uk.nhs.nhsx.core.auth.ApiName
    API_NAMES = {
      "mobile" => "mobile",
      "testResultUpload" => "test_result",
      "highRiskVenuesCodeUpload" => "venues",
      "highRiskPostCodeUpload" => "post_districts",
      "health" => "health",
      "isolationPayment" => "isolation_payment",
    }.freeze

    # All the fixed mappings between AWS and the AAE per account: {"account"=>[aae_account]}
    AAE_TARGET_ACCOUNTS = {
      "dev" => "test",
      "staging" => "test",
      "prod" => "prod",
    }.freeze

    # All the fixed (named) target environments per account: {"account"=>[target_environments]}
    CTA_TARGET_ENVIRONMENTS = {
      "dev" => ["ci", "test", "qa", "fnctnl", "demo", "load-test", "extdev", "sit", "pentest", "branch"],
      "staging" => ["staging"],
      "prod" => ["prod"],
    }.freeze
    CI_TARGET_ENVIRONMENTS = {
      "dev" => ["ci", "test", "qa", "fnctnl", "demo", "load-test", "extdev", "sit", "pentest", "branch"],
      "staging" => ["staging"],
      "prod" => ["prod"],
      "aa-dev" => ["aa-dev", "aa-ci"],
      "aa-staging" => ["aa-staging"],
      "aa-prod" => ["aa-prod"],
    }.freeze
    # All the fixed (named) Analytics target environments per account: {"account"=>[target_environments]}
    ANALYTICS_TARGET_ENVIRONMENTS = {
      "dev" => ["load-test", "ci", "fnctnl", "qa", "branch", "extdev", "test", "demo"],
      "staging" => ["staging"],
      "prod" => ["prod"],
      "aa-dev" => ["aa-dev", "aa-ci"],
      "aa-staging" => ["aa-staging"],
      "aa-prod" => ["aa-prod"],
    }.freeze
    # All the fixed (named) DoReTo target environments per account: {"account"=>[target_environments]}
    DORETO_TARGET_ENVIRONMENTS = {
      "dev" => ["test", "branch"],
    }.freeze
    # All the fixed (named) public dashboard target environments per account: {"account"=>[target_environments]}
    PUBDASH_TARGET_ENVIRONMENTS = {
      "dev" => ["ci", "test", "qa", "fnctnl", "demo", "load-test", "extdev", "sit", "pentest", "branch"],
      "staging" => ["staging"],
      "prod" => ["prod"],
      "aa-dev" => ["aa-ci"],
      "aa-staging" => ["aa-staging"],
      "aa-prod" => ["aa-prod"],
    }.freeze
    # The parameter name that contains the ARN of the signing key in the SSM paramater store
    SIGNING_KEY_PARAMETER = "/app/kms/SigningKeyArn".freeze
    # The parameter name that contains the ARN of the content signing key in the SSM paramater store
    CONTENT_SIGNING_KEY_PARAMETER = "/app/kms/ContentSigningKeyArn".freeze
    # The name of the secret that contains the API authentication headers used by the tests
    TEST_API_KEY_HEADERS_SECRET = "AuthenticationHeadersForTests".freeze

    # Retrieves the target environment configuration
    def target_environment_configuration(environment_name, account_name, system_config)
      terraform_configuration = File.join(system_config.base, APP_SYSTEM_ACCOUNTS, account_name)
      target_config = parse_terraform_output(terraform_output(environment_name, terraform_configuration, system_config))
      target_config["auth_headers"] = authentication_headers_for_test(system_config)
      target_config["target_environment_name"] = environment_name
      return target_config
    end

    # Retrieves the target environment configuration for the Document Reporting Tool subsystem
    def doreto_target_environment_configuration(environment_name, account_name, system_config)
      terraform_configuration = File.join(system_config.base, DORETO_ACCOUNTS, account_name)
      parse_terraform_output(terraform_output(environment_name, terraform_configuration, system_config))
    end

    # Retrieves the target environment configuration for public dashboard
    def pubdash_target_environment_configuration(environment_name, account_name, system_config)
      terraform_configuration = File.join(system_config.base, PUBDASH_ACCOUNTS, account_name)
      parse_terraform_output(terraform_output(environment_name, terraform_configuration, system_config))
    end

    # Retrieves the target environment configuration
    def analytics_target_environment_configuration(environment_name, account_name, system_config)
      terraform_configuration = File.join(system_config.base, ANALYTICS_ACCOUNTS, account_name)
      parse_terraform_output(terraform_output(environment_name, terraform_configuration, system_config))
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
    def testing_secrets(system_config)
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

    def get_aae_mapping_uuid(target_env_config, format)
      identifier = "_events" if format == "json"
      lambda_arn = target_env_config["aae#{identifier}_export_function_arn"]
      event_source_arn = target_env_config["aae#{identifier}_export_event_source_arn"]
      function_name = target_env_config["aae#{identifier}_export_function_name"]
      uuid = get_event_source_mapping_uuid(function_name, event_source_arn, lambda_arn, $configuration)
      raise GaudiError, "Could not find event source mapping uuid for #{function_name}" if uuid.nil?

      return uuid
    end
  end
end
