namespace :login do
  task :prod do
    require "highline"
    include NHSx::AWS
    cli = HighLine.new
    answer = cli.ask "Do you really want to perform the task against prod? Type 'prod' to confirm"
    raise GaudiError, "Aborted login to prod" unless ["prod"].include?(answer.downcase)

    mfa_login($configuration.aws_role, "prod")
    ENV["AWS_PROFILE"] = "nhs-auth-prod"
    ENV["AWS_REGION"] = NHSx::AWS::AWS_REGION
    ENV["ACCOUNT"] = "prod"
  end

  task :staging do
    include NHSx::AWS

    mfa_login($configuration.aws_role, "staging")
    ENV["AWS_PROFILE"] = "nhs-auth-staging"
    ENV["AWS_REGION"] = NHSx::AWS::AWS_REGION
    ENV["ACCOUNT"] = "staging"
  end

  task :dev do
    ENV["ACCOUNT"] = "dev"
    ENV["AWS_REGION"] = NHSx::AWS::AWS_REGION
  end
end
