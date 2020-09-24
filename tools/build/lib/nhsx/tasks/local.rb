desc "Run the system devenv container"
task :devenv do
  include NHSx::Local
  include NHSx::Docker

  home_volumes = {".aws" => false,
                  ".m2" => true,
                  ".gradle" => true}

  mount_points = [mount_point($configuration.base, "/workspace")]

  docker_homedir = case os
                   when :linux
                     "/workspace"
                   else
                     "/root"
                   end

  docker_additional = case os
                      when :linux
                        "--ulimit memlock=-1:-1 "
                      else
                        ""
                      end

  mount_points += home_volumes.map {
      |dir, create_if_missing| path = File.join(user_home, dir)
    Dir.mkdir(path) if create_if_missing unless File.exist?(path)
    [dir, path]
  }.to_h.select {
      |_, d| raise "#{d} does not exist - did you set up AWS?" unless File.exist?(d)
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
