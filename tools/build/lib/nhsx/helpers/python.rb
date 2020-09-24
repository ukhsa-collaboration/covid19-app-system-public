module NHSx
  # Helpers that codify the use of Python within the NHSx project
  module Python
    include Zuehlke::Execution
    # Location relative to the repository root
    LAMBDAS_LOCATION = "src/aws/lambdas"
    # Python commandlines in use by automation scripts
    module Commandlines
      # Command line for pip installing from requirements to a specific target directory
      def self.requirements(requirements_location, target_location)
        "pip3 install --target #{target_location} -r #{requirements_location}"
      end

      # Call flake8 with the given configuration file and force it to return an exit code of 1 if there are errors.
      #
      # When configuring exclusions note that paths are relative to the configuration file, not the working directory.
      def self.lint(lint_configuration)
        "flake8 --count --config #{lint_configuration}"
      end
    end

    def install_requirements(lambda_function, output_path, system_config)
      requirements_location = File.join(lambda_function, "requirements.txt")
      cmdline = NHSx::Python::Commandlines.requirements(requirements_location, output_path)
      run_command("Installing python requirements for #{lambda_function}", cmdline, system_config)
    end
  end
end
