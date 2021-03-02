module NHSx
  # Methods to facilitate logging in to different AWS accounts
  module Login
    include NHSx::AWS

    def double_check_prompt(account_name)
      require "highline"
      include NHSx::AWS
      cli = HighLine.new
      answer = cli.ask "Do you really want to perform the task against #{account_name}? Type '#{account_name}' to confirm"
      raise GaudiError, "Aborted login to #{account_name}" unless [account_name].include?(answer.downcase)
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

    def login_to_sso_account(account_name, use_prompt)
      double_check_prompt(account_name) if use_prompt

      raise GaudiError, "SSO login not implemented yet"

      unless ENV["CODEBUILD_BUILD_ID"]
        raise GaudiError, "Looks like you're not running in the docker container...mate" unless $configuration.base.start_with?("/workspace")
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
  end
end
