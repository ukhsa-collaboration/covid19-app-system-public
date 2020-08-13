namespace :login do
  task :prod do
    include NHSx::AWS
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
