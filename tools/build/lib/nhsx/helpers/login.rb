require "inifile"
require "json"
require "highline"
require_relative "target"
require_relative "aws"

module NHSx
  # Methods to facilitate logging in to different AWS accounts
  module Login
    include NHSx::TargetEnvironment

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
      double_check_prompt(account_name) if use_prompt

      unless ENV["CODEBUILD_BUILD_ID"]
        raise GaudiError, "Looks like you're not running in the docker container...mate" unless $configuration.base.start_with?("/workspace")

        mfa_login($configuration.aws_role, account_name)
        ENV["AWS_PROFILE"] = "nhs-auth-#{account_name}"
      end
      ENV["AWS_REGION"] = NHSx::AWS::AWS_REGION
      ENV["ACCOUNT"] = account_name
    end

    # Perform an aws-mfa login to prompt for the MFA code
    def mfa_login(role_name, account)
      role_arn = aws_role_arn(role_name, account)
      cmdline = NHSx::AWS::Commandlines.multi_factor_authentication(NHSx::AWS::AWS_AUTH_PROFILE, role_arn, account)
      sh(cmdline)
    end

    def login_to_sso_account(role_name, account_name, domain, use_prompt)
      double_check_prompt(account_name) if use_prompt

      ENV.delete("AWS_PROFILE")

      ENV["AWS_PROFILE"] = sso_login(role_name, account_name, domain)

      ENV["AWS_REGION"] = NHSx::AWS::AWS_REGION
      ENV["ACCOUNT"] = account_name
    end

    # Perform an aws sso login
    def sso_login(role_name, account, domain)
      profile_info = sso_profile_info(role_name, domain, account)
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

    def sso_profile_info(role_name, domain, account)
      role_arn = aws_role_arn(role_name, account)
      info = /^arn:aws:iam::(?<account_number>\d+):role\/(\w+-)?(?<effective_role_name>\w+$)/.match(role_arn)
      account_number = info[:account_number]
      effective_role_name = info[:effective_role_name]
      sso_role_name = NHSx::TargetEnvironment::DOMAIN_HALO_ROLES[effective_role_name][domain][account]
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
  end
end
