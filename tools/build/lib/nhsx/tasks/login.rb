namespace :login do
  include NHSx::Login

  task :prod do
    container_guard($configuration)
    login_to_mfa_account("prod", true)
  end

  task :staging do
    container_guard($configuration)
    login_to_mfa_account("staging", false)
  end

  task :dev do
    container_guard($configuration)
    ENV["ACCOUNT"] = "dev"
    ENV["AWS_REGION"] = NHSx::AWS::AWS_REGION
  end

  task :'aa-prod' do
    container_guard($configuration)
    login_to_sso_account($configuration.aws_role, 'aa-prod', 'analytics', true)
  end

  task :'aa-staging' do
    container_guard($configuration)
    login_to_sso_account($configuration.aws_role, 'aa-staging', 'analytics', false)
  end

  task :'aa-dev' do
    container_guard($configuration)
    login_to_sso_account($configuration.aws_role, 'aa-dev', 'analytics', false)
  end
end
