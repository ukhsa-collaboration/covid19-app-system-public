namespace :provision do
  namespace :devenv do
    desc "Pull the docker image from the registry"
    task :pull do
      include NHSx::Docker
      pull_devenv_image($configuration)
    end
  end

  namespace :target do
    desc "Provision the dev target environment on AWS"
    task :dev do
      varfiles = generate_variable_files("dev")
      run_terraform("apply", File.join($configuration.base, "tools/provision/terraform"), varfiles, "-auto-approve -lock=true")
    end
  end
end
