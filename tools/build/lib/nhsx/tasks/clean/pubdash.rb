namespace :clean do
  task :"pubdash:orphans" => [:"plan:pubdash:ci"] do
    include NHSx::Clean

    terraform_configuration = File.join($configuration.base, NHSx::TargetEnvironment::PUBDASH_DEV_ACCOUNT)
    clean_terraform_resources(terraform_configuration, NHSx::TargetEnvironment::PUBDASH_TARGET_ENVIRONMENTS["dev"], $configuration)
  end
end
