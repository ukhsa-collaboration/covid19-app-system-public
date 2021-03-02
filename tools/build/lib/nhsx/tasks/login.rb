namespace :login do
  include NHSx::Login

  task :prod do |t|
    login_to_mfa_account("prod", true)
  end

  task :staging do |t|
    login_to_mfa_account("staging", false)
  end

  task :dev do
    ENV["ACCOUNT"] = "dev"
    ENV["AWS_REGION"] = NHSx::AWS::AWS_REGION

    unless ENV["CODEBUILD_BUILD_ID"]
      raise GaudiError, "Looks like you're not running in the docker container...mate" unless $configuration.base.start_with?("/workspace")
    end
  end

  task :"aa-dev" do |t|
    login_to_sso_account("aa-dev", false)
  end

  task :"aa-prod" do
    login_to_sso_account("aa-prod", true)
  end

  task :"aa-staging" do
    login_to_sso_account("aa-staging", false)
  end
end
