
namespace :login do
  task :prod do
    require "highline"
    include NHSx::AWS
    cli = HighLine.new
    answer = cli.ask "Do you really want to perform the task against prod? Type 'prod' to confirm"
    raise GaudiError, "Aborted login to prod" unless ["prod"].include?(answer.downcase)

    mfa_login($configuration.aws_role, "prod")
    ENV["AWS_PROFILE"] = "nhs-auth-prod"
  end

  task :staging do
    include NHSx::AWS

    mfa_login($configuration.aws_role, "staging")
    ENV["AWS_PROFILE"] = "nhs-auth-staging"
  end

  task :dev
end
