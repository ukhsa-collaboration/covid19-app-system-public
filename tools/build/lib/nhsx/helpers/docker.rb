require "rake/file_list"
require "digest"

module NHSx
  # Helpers that codify the use of Docker within the NHSx project
  module Docker
    include Zuehlke::Execution
    # Docker commandlines in use by automation scripts
    module Commandlines
      # Pull an image
      def self.pull_image(image_tag)
        "docker image pull -q \"#{image_tag}\""
      end

      # Push a docker image
      def self.push_image(image_tag)
        "docker image push #{image_tag}"
      end

      def self.tag_existing(existing_tag, new_tag)
        "docker tag #{existing_tag} #{new_tag}"
      end
    end

    # The tag used for the devenv container image, minus the version
    REGISTRY = "123456789012.dkr.ecr.eu-west-2.amazonaws.com".freeze
    SERVICE = "nhsx-covid19".freeze

    DEFAULT_VERSION = "devenv-latest".freeze

    # Calculate the SHA256 hash of the content of some files
    def sha256(files)
      sha = Digest::SHA2.new
      files.each do |f|
        content = File.binread(f)
        # work around possible git checkout issues by removing CR and LF from the file
        content.gsub!("\n", "")
        content.gsub!("\r", "")
        sha << content
      end
      sha.hexdigest
    end

    def docker_image_sourcefiles(system_config)
      Rake::FileList["#{system_config.base}/tools/build/Gemfile",
                     "#{system_config.base}/tools/provisioning/python/requirements.txt",
                     "#{system_config.base}/tools/provisioning/dev/Dockerfile",
                     "#{system_config.base}/tools/provisioning/dev/useradd-init",
                     "#{system_config.base}/tools/provisioning/dev/profile.d/**"]
    end

    def content_version(system_config)
      "content-#{sha256(docker_image_sourcefiles(system_config))}"
    end

    def full_tag(version)
      "#{REGISTRY}/#{SERVICE}:#{version}"
    end

    def pull_repository_image(system_config, tag)
      registry_login(system_config)
      cmdline = Commandlines.pull_image(tag)
      run_command("Pull #{tag.split(":").last}", cmdline, system_config)
    end

    # Pulls the docker image for the development environment
    def pull_devenv_image(system_config)
      # doing it like this avoids leaking the login token in the logs
      registry_login(system_config)
      tag = full_tag(content_version(system_config))
      cmdline = Commandlines.pull_image(tag)
      run_command("Pull #{tag.split(":").last}", cmdline, system_config)
      tag_content_version_as_latest(system_config, tag)
    end

    def tag_content_version_as_latest(system_config, existing_tag)
      tag = full_tag(DEFAULT_VERSION)
      cmdline = Commandlines.tag_existing(existing_tag, tag)
      run_command("Tag #{existing_tag.split(":").last} as latest", cmdline, system_config)
    end

    # Publish the docker container image to the ECR registry
    def publish_devenv_image(system_config)
      registry_login(system_config)
      [content_version(system_config), DEFAULT_VERSION].map { |t| full_tag(t) }.each do |tag|
        cmdline = Commandlines.push_image(tag)
        run_command("Publish #{tag.split(":").last}", cmdline, system_config)
      end
    end

    # Logins to the AWS-based docker registry using a temporary ECR login token
    def registry_login(system_config)
      # doing it like this avoids leaking the login token in the logs
      cmdline = "#{NHSx::AWS::Commandlines.ecr_login}|docker login --username AWS --password-stdin 123456789012.dkr.ecr.eu-west-2.amazonaws.com"
      run_command("Logging into AWS registry", cmdline, system_config)
    end
  end
end
