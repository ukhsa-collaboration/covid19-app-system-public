require "inifile"
require "json"
require "highline"
require "open3"
require_relative "target"
require_relative "aws"

module NHSx
  # Methods to facilitate logging in to different AWS accounts
  module Login
    include NHSx::TargetEnvironment
    # Accounts using MFA for authentication
    MFA_ACCOUNTS = ["prod", "staging"].freeze
    # Accounts using SSO for authentication
    SSO_ACCOUNTS = ["aa-prod", "aa-staging", "aa-dev"].freeze
    # The ARNs for the deployment roles per account
    AWS_DEPLOYMENT_ROLES = {
      "dev" => "arn:aws:iam::123456789012:role/dev-ApplicationDeploymentUser",
      "staging" => "arn:aws:iam::123456789012:role/staging-ApplicationDeploymentUser",
      "prod" => "arn:aws:iam::123456789012:role/prod-ApplicationDeploymentUser",
      "aa-dev" => "arn:aws:iam::181706652550:role/ApplicationDeploymentUser",
      "aa-staging" => "arn:aws:iam::074634264982:role/ApplicationDeploymentUser",
      "aa-prod" => "arn:aws:iam::353189165293:role/ApplicationDeploymentUser",
    }.freeze
    # The AWS profile to use when authenticating with MFA
    AWS_AUTH_PROFILE = "nhs-auth".freeze
    # Mapping for AWS_DEPLOYMENT_ROLES to Halo provided roles to switch from
    DOMAIN_HALO_ROLES = {
      "ApplicationDeploymentUser" => {
        "analytics" => {
          "aa-dev" => "WlAlyticDevApplicationDeployer",
          "aa-staging" => "WlAlyticStgApplicationDeployer",
          "aa-prod" => "WlAlyticProdApplicationDeployer",
        },
      },
    }.freeze

    def double_check_prompt(account_name)
      cli = HighLine.new
      answer = cli.ask "Do you really want to perform the task against #{account_name}? Type '#{account_name}' to confirm"
      raise GaudiError, "Aborted login to #{account_name}" unless [account_name].include?(answer.downcase)
    end

    # Will raise an exception if the current workspace is not in the devenv container
    def container_guard(system_config)
      raise GaudiError, "Looks like you're not running in the docker container...mate" unless system_config.base.start_with?("/workspace") || ENV["CODEBUILD_BUILD_ID"]
    end

    def login_to_mfa_account(account_name, use_prompt)
      use_prompt = false if ENV["CODEBUILD_BUILD_ID"]
      double_check_prompt(account_name) if use_prompt
      ENV["AWS_PROFILE"] = mfa_login(account_name) unless ENV["CODEBUILD_BUILD_ID"]

      ENV["AWS_REGION"] = NHSx::AWS::AWS_REGION
      ENV["ACCOUNT"] = account_name
    end

    # Perform an aws-mfa login to prompt for the MFA code
    def mfa_login(account)
      role_arn = AWS_DEPLOYMENT_ROLES[account]
      raise GaudiError, "No deployment role defined for account #{account}" unless role_arn

      cmdline = NHSx::AWS::Commandlines.multi_factor_authentication(NHSx::Login::AWS_AUTH_PROFILE, role_arn, account)

      sh(cmdline)
      mfa_cmd_output, _, mfa_cmd_status = Open3.capture3(cmdline)

      if mfa_cmd_status.success?
        check_mfa_login_for_renewal(account, mfa_cmd_output)
      else
        raise GaudiError, "MFA login was not successful!"
      end
      return "#{NHSx::Login::AWS_AUTH_PROFILE}-#{account}"
    end

    def check_mfa_login_for_renewal(account, mfa_cmd_output)
      seconds_till_timeout = mfa_cmd_output.split("INFO - Your credentials are still valid for ", -1)[1].split(".", -1)[0]
      if seconds_till_timeout.to_i > 600
        puts "INFO - Your credentials will not expire in the next 10 Minutes."
      else
        aws_credentials_filename = File.join(ENV["HOME"], ".aws/credentials")
        aws_credentials_content = File.read(aws_credentials_filename)
        current_expiry_date = mfa_cmd_output.split(" seconds they will expire at ", -1)[1].split(" ", -1)[0]
        write_file(aws_credentials_filename, aws_credentials_content.gsub(current_expiry_date, "1970-01-01"))
        puts "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! WARNING !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
        puts "!!!!!!!WARNING: Your AWS-MFA token will expire in less than 10 minutes. Logging out to prevent deployment timeouts. You need to login again!!!!!!!"
        puts "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! WARNING !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
        mfa_login(account)
      end
    end

    def login_to_sso_account(account, domain, use_prompt)
      use_prompt = false if ENV["CODEBUILD_BUILD_ID"]
      double_check_prompt(account) if use_prompt

      ENV.delete("AWS_PROFILE")
      ENV["AWS_PROFILE"] = sso_login(account, domain)
      ENV["AWS_REGION"] = NHSx::AWS::AWS_REGION
      ENV["ACCOUNT"] = account
    end

    # Perform an aws sso login
    def sso_login(account, domain)
      profile_info = sso_profile_info(account, domain)
      begin
        credentials_info = assume_role_credentials(profile_info)
        update_aws_settings("credentials", profile_info["profile_name"], credentials_info)
      rescue
        update_aws_settings("config", "profile #{profile_info["sso_profile_name"]}", sso_profile(profile_info, NHSx::AWS::AWS_REGION))
        get_sso_session(profile_info["sso_profile_name"])
        # retry assume_role_credentials after get_sso_session
        credentials_info = assume_role_credentials(profile_info)
        update_aws_settings("credentials", profile_info["profile_name"], credentials_info)
      end
      profile_info["profile_name"]
    end

    def update_aws_settings(file_type, section_key, info)
      config = IniFile.load(NHSx::AWS::AWS_CONFIG_PATHS[file_type])
      config[section_key] = info
      config.save
    end

    def get_sso_session(profile_name)
      sh(NHSx::AWS::Commandlines.sso_login(profile_name))
    end

    def sso_profile_info(account, domain)
      role_arn = AWS_DEPLOYMENT_ROLES[account]
      info = /^arn:aws:iam::(?<account_number>\d+):role\/(\w+-)?(?<effective_role_name>\w+$)/.match(role_arn)
      account_number = info[:account_number]
      effective_role_name = info[:effective_role_name]
      sso_role_name = NHSx::Login::DOMAIN_HALO_ROLES[effective_role_name][domain][account]
      profile_name = "#{domain}-#{account}-#{effective_role_name}"
      sso_profile_name = "#{profile_name}-sso"

      {
        "domain" => domain,
        "account_number" => account_number,
        "profile_name" => profile_name,
        "sso_profile_name" => sso_profile_name,
        "effective_role_arn" => role_arn,
        "effective_role_name" => effective_role_name,
        "sso_role_name" => sso_role_name,
      }
    end

    def sso_profile(profile_info, region)
      {
        "sso_start_url" => "https://halopr.awsapps.com/start",
        "sso_region" => region,
        "sso_account_id" => profile_info["account_number"],
        "sso_role_name" => profile_info["sso_role_name"],
        "region" => region,
      }
    end

    def assume_role_credentials(profile_info)
      cmdline = "aws sts assume-role --profile=#{profile_info["sso_profile_name"]}" +
                " --role-arn #{profile_info["effective_role_arn"]}" +
                " --role-session-name=#{profile_info["domain"]}_deploy$(openssl rand -hex 3)"
      creds_json = `#{cmdline}`
      creds = JSON.parse(creds_json)

      {
        "aws_access_key_id" => creds["Credentials"]["AccessKeyId"],
        "aws_secret_access_key" => creds["Credentials"]["SecretAccessKey"],
        "aws_session_token" => creds["Credentials"]["SessionToken"],
        "expiration" => creds["Credentials"]["Expiration"],
      }
    end

    def login_to_aws_account(account, domain, use_prompt)
      if MFA_ACCOUNTS.include?(account)
        login_to_mfa_account(account, use_prompt)
      elsif SSO_ACCOUNTS.include?(account)
        login_to_sso_account(account, domain, use_prompt)
      else
        raise GaudiError, "Unauthorized account #{account}" unless account == "dev"

        ENV.delete("AWS_PROFILE")
        ENV["AWS_REGION"] = NHSx::AWS::AWS_REGION
        ENV["ACCOUNT"] = account
      end
    end

    # Logs into the given account to perform the actions in the block
    #
    # Restores the AWS profile after the block returns
    def with_account(account, domain)
      raise GaudiError, "This method requires a block to run in the account context" unless block_given?

      current_aws_profile = ENV["AWS_PROFILE"]
      current_aws_region = ENV["AWS_REGION"]
      current_aws_account = ENV["ACCOUNT"]

      login_to_aws_account(account, domain, false)

      yield
    ensure
      ENV["AWS_PROFILE"] = current_aws_profile
      ENV["AWS_REGION"] = current_aws_region
      ENV["ACCOUNT"] = current_aws_account
    end
  end
end
