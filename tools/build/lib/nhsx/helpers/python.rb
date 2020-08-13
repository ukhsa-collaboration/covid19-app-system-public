module NHSx
  # Helpers that codify the use of Python within the NHSx project
  module Python
    include Zuehlke::Execution
    # Location relative to the repository root
    LAMBDAS_LOCATION = "src/aws/lambdas"
    # Python commandlines in use by automation scripts
    module Commandlines
      # Command line for pip installing to a specific target directory
      def self.vendor_package(package, vendor_location)
        "pip3 install --target #{vendor_location} #{package}"
      end

      # Call flake8 with the given configuration file and force it to return an exit code of 1 if there are errors.
      #
      # When configuring exclusions note that paths are relative to the configuration file, not the working directory.
      def self.lint(lint_configuration)
        "flake8 --count --config #{lint_configuration}"
      end
    end

    # Vendor a Python library within a lambda function's directory to make it available during deployment
    #
    # This methods relies on the placement and naming conventions for lambdas - change it when the conventions change
    def vendor_package_in_lambda(python_package, lambda_function, system_config)
      lambda_function_location = File.join(system_config.base, LAMBDAS_LOCATION, lambda_function, "packages")
      cmdline = NHSx::Python::Commandlines.vendor_package(python_package, lambda_function_location)
      run_command("Vendor #{python_package} in #{lambda_function}", cmdline, system_config)
    end
  end
end
