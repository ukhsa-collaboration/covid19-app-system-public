namespace :provision do
  namespace :devenv do
    desc "Pull the docker image from the registry"
    task :pull do
      include NHSx::Docker
      include NHSx::Login

      raise GaudiError, "You cannot pull a docker image from within the docker container" if $configuration.base.start_with?("/workspace")

      login_to_aws_account("dev", "cta", false)
      pull_devenv_image($configuration)
    end
  end
end
