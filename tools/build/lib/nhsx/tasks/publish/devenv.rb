namespace :publish do
  desc "Publish the devenv docker image to the registry"
  task :devenv => [:"build:devenv"] do
    include NHSx::Docker
    publish_devenv_image($configuration)
  end
end
