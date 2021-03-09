namespace :clean do
  task :"doreto:orphans" do
    include NHSx::Clean

    terraform_configuration = File.join($configuration.base, NHSx::TargetEnvironment::DORETO_DEV_ACCOUNT)
    clean_terraform_resources(terraform_configuration, NHSx::TargetEnvironment::DORETO_TARGET_ENVIRONMENTS["dev"], $configuration)
  end
end
