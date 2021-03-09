namespace :clean do
  desc "Clean up orphan branch environments"
  task :"cta:orphans" => [:"build:dependencies"] do
    include NHSx::Clean

    terraform_configuration = File.join($configuration.base, NHSx::TargetEnvironment::CTA_DEV_ACCOUNT)
    clean_terraform_resources(terraform_configuration, NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS["dev"], $configuration)
  end
end
