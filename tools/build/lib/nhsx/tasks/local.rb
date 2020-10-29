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
    ".gradle" => true,
    ".m2" => true,
    ".ssh" => true,
  }

  mount_points = [mount_point($configuration.base, "/workspace")]

  docker_homedir = case os
                   when :linux
                     "/home/dev100x"
                   else
                     "/root"
                   end

  docker_additional = case os
                      when :linux
                        "--ulimit memlock=-1:-1 --user $(id -u) "
                      else
                        ""
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

  cmdline = "docker run #{docker_additional} -it #{mount_points.join(" ")} #{full_tag(NHSx::Docker::DEFAULT_VERSION)}"
  begin
    sh(cmdline)
  rescue
    #   #Exiting docker is not an error
  end
end
