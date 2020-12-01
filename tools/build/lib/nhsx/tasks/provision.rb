namespace :provision do
  namespace :devenv do
    desc "Pull the docker image from the registry"
    task :pull do
      include NHSx::Docker
      pull_devenv_image($configuration)
    end
  end
end
