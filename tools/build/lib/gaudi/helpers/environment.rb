module Gaudi
  # Helper methods to enable handling of environment variables
  module EnvironmentHelpers
    # Will raise an exception if env_var is not defined
    def mandatory(env_var)
      ENV[env_var] || raise(GaudiError, "Environment variable '#{env_var}' not defined.\nValue mandatory for the current task.")
    end
  end

  module Configuration
    # Encapsulates the environment variables used to adjust the builder's configuration
    #
    # Is mixed in with SystemConfiguration
    module EnvironmentOptions
      include EnvironmentHelpers
      # Returns the value of GAUDI_CONFIG used to find the system configuration file
      def gaudi_config
        mandatory("GAUDI_CONFIG")
      end

      # Defines the user name to work with, raises GaudiConfigurationError if not defined
      def user!
        mandatory("USER")
      end

      # Returns the user name to work with, raises no exception whatsoever
      def user
        return ENV["USER"]
      end
    end
  end
end
