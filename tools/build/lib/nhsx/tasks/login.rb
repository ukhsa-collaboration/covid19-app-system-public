namespace :login do
  include NHSx::Login

  task :prod do
    container_guard($configuration)
    login_to_aws_account("prod", "cta", true)
  end

  task :"prod-support" do
    container_guard($configuration)
    login_to_aws_account("prod-support", "cta", true)
  end

  task :staging do
    container_guard($configuration)
    login_to_aws_account("staging", "cta", true)
  end

  task :dev do
    container_guard($configuration)
    login_to_aws_account("dev", "cta", false)
  end

  task :"aa-prod" do
    container_guard($configuration)
    login_to_aws_account("aa-prod", "analytics", true)
  end

  task :"aa-staging" do
    container_guard($configuration)
    login_to_aws_account("aa-staging", "analytics", true)
  end

  task :"aa-dev" do
    container_guard($configuration)
    login_to_aws_account("aa-dev", "analytics", false)
  end
end
