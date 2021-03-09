namespace :clean do
  desc "Clean up orphaned analytics resources owned by defunct branches in dev"
  task :"analytics:orphans" => [:"login:dev", :"plan:analytics:ci"] do
    include NHSx::Clean

    terraform_configuration = File.join($configuration.base, NHSx::TargetEnvironment::ANALYTICS_DEV_ACCOUNT)
    clean_terraform_resources(terraform_configuration, NHSx::TargetEnvironment::ANALYTICS_TARGET_ENVIRONMENTS["dev"], $configuration)
  end
end
