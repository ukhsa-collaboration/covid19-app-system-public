desc "Run the system devenv container"
task :devenv do
  include NHSx::Local

  aws_configuration = File.join(user_home, ".aws")

  mount_points = [mount_point($configuration.base, "/workspace")]
  mount_points += [mount_point(aws_configuration, "/root/.aws")] if File.exist?(aws_configuration)

  cmdline = "docker run -it #{mount_points.join(" ")} #{NHSx::Docker::DEVENV}-latest"
  begin
    sh(cmdline)
  rescue
    #   #Exiting docker is not an error
  end
end
