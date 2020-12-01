desc "Run the system devenv container"
task :devenv do
  include NHSx::Local
  include NHSx::Docker

  # Ensure that the following files exist - we don't want to create them as directories
  FileUtils.touch File.expand_path('~/.gitconfig')
  FileUtils.touch File.expand_path('~/.git-credentials')

  home_volumes = {
      ".aws" => false,
      ".gitconfig" => false,
      ".git-credentials" => false,
      ".m2" => true,
      ".ssh" => true,
  }

  mount_points = [mount_point($configuration.base, "/workspace")]

  docker_homedir = "/home/developer"

  docker_additional = case os
                      when :linux
                        "--ulimit memlock=-1:-1 "
                      else
                        ""
                      end

  docker_env = case os
               when :windows
                 "--env HOST_UID=1000 --env HOST_GID=1000"
               else
                 "--env HOST_UID=$(id -u) --env HOST_GID=$(id -g)"
               end
  mount_points += home_volumes.map {
      |dir, create_if_missing| path = File.join(user_home, dir)
    Dir.mkdir(path) if create_if_missing unless File.exist?(path)
    [dir, path]
  }.to_h.select {
      |_, d| raise "#{d} does not exist - did you set up your AWS profile?" unless File.exist?(d)
    true
  }.map {
      |n, d| mount_point(d, "#{docker_homedir}/#{n}")
  }

  # special case for gradle as directories cannot be shared, annoyingly.
  # https://stackoverflow.com/questions/41704184/any-drawbacks-of-sharing-the-gradle-user-home-with-many-developers/41719563#41719563
  #
  gradle_docker = File.join(user_home, ".gradle-docker")
  Dir.mkdir(gradle_docker) unless File.exist?(gradle_docker)
  mount_points += [mount_point(gradle_docker, "#{docker_homedir}/.gradle")]

  cmdline = "docker run #{docker_additional} -it #{mount_points.join(" ")} #{docker_env} #{full_tag(NHSx::Docker::DEFAULT_VERSION)}"
  begin
    sh(cmdline)
  rescue
    #   #Exiting docker is not an error
  end
end
