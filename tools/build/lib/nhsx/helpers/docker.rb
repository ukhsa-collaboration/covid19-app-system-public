module NHSx
  # Helpers that codify the use of Docker within the NHSx project
  module Docker
    include Zuehlke::Execution
    # Docker commandlines in use by automation scripts
    module Commandlines
      # Pull an image
      def self.pull_image(image_tag)
        "docker image pull -q #{image_tag}"
      end

      # Push a docker image
      def self.push_image(image_tag)
        "docker image push #{image_tag}"
      end
    end

    # The tag used for the devenv container image, minus the version
    DEVENV = "123456789012.dkr.ecr.eu-west-2.amazonaws.com/nhsx-covid19:devenv".freeze
    DORETO = "123456789012.dkr.ecr.eu-west-2.amazonaws.com/nhsx-covid19:doreto".freeze

    # Pulls the docker image for the development environment
    def pull_devenv_image(system_config)
      # doing it like this avoids leaking the login token in the logs
      registry_login(system_config)
      cmdline = Commandlines.pull_image("#{DEVENV}-latest")
      run_command("Pull #{DEVENV}-latest", cmdline, system_config)
    end

    # Publish the docker container image to the ECR registry
    def publish_devenv_image(system_config)
      registry_login(system_config)
      cmdline = Commandlines.push_image("#{DEVENV}-latest")
      run_command("Publish #{DEVENV}-latest", cmdline, system_config)
    end

    # Publish the document reporting tool docker container image to the ECR registry
    def publish_doreto_image(system_config)
      registry_login(system_config)
      cmdline = Commandlines.push_image("#{DORETO}-latest")
      run_command("Publish #{DORETO}-latest", cmdline, system_config)
    end

    # Logins to the AWS-based docker registry using a temporary ECR login token
    def registry_login(system_config)
      # doing it like this avoids leaking the login token in the logs
      cmdline = "#{NHSx::AWS::Commandlines.ecr_login}|docker login --username AWS --password-stdin 123456789012.dkr.ecr.eu-west-2.amazonaws.com"
      run_command("Logging into AWS registry", cmdline, system_config)
    end
  end
end
