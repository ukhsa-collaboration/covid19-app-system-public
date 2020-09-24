module NHSx
  # Helpers for use of the development environment
  # in a local host (i.e. a developer workstation)
  module Local
    # Returns the full path to the user home directory
    # irrespective of OS
    def user_home
      if Gem.win_platform?
        homepath = ENV["USERPROFILE"]
      else
        homepath = ENV["HOME"]
      end
      return File.expand_path(homepath)
    end

    def os
      @os ||= (
      host_os = RbConfig::CONFIG['host_os']
      case host_os
      when /mswin|msys|mingw|cygwin|bccwin|wince|emc/
        :windows
      when /darwin|mac os/
        :macosx
      when /linux/
        :linux
      else
        raise "unknown os: #{host_os.inspect}"
      end
      )
    end

    # Returns the command line parameters to allow
    # docker to mount the local path.
    #
    # Will detect Windows and convert path format accordingly
    def mount_point(local_path, docker_path)
      local_path.gsub!("/", "\\") if Gem.win_platform?
      "-v \"#{local_path}\":#{docker_path}"
    end
  end
end
