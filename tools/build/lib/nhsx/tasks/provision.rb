namespace :provision do
  namespace :devenv do
    desc "Pull the docker image from the registry"
    task :pull do
      include NHSx::Docker

      raise GaudiError, "You cannot pull a docker image from within the docker container" if $configuration.base.start_with?("/workspace")

      pull_devenv_image($configuration)
    end
  end
end
