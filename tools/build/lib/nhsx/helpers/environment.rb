module Gaudi
  module Configuration
    # Encapsulates the environment variables used to adjust the builder's configuration
    #
    # Is mixed in with SystemConfiguration
    module EnvironmentOptions
      include EnvironmentHelpers
      # Specify the target environment to use when deploying or running tests
      def target_environment
        mandatory("TARGET_ENVIRONMENT")
      end

      # Specify the name for the new api key
      def api_key_name
        mandatory("API_KEY_NAME")
      end

      def test_data
        test_data_file = File.expand_path(mandatory("TEST_DATA"))
        raise GaudiError, "Test data #{test_data_file} not found" unless File.exist?(test_data_file)

        return test_data_file
      end

      # Retrieve an AWS_ROLE used to determine the actual login credentials to use for AWS
      #
      # This can be one of ["deploy","read"]
      #
      # Returns "deploy" by default
      def aws_role
        role_name = ENV["AWS_ROLE"]
        role_name = "deploy" if role_name.nil?

        raise GaudiError, "You can only specify one of the following roles: #{NHSx::AWS::AWS_ROLE_NAMES.join(",")} " unless NHSx::AWS::AWS_ROLE_NAMES.include?(role_name)

        return role_name
      end

      # Pass the value for a TOKEN
      def token
        mandatory("TOKEN")
      end

      # Pass a point in time value for restoring backups
      #
      # Returns epoch time for given value
      #
      # Eg: RESTORE_AT = "September 9, 2020 at 8:32:06 PM UTC+8"
      def restore_at
        restore_time = mandatory("RESTORE_AT")
        parsed_time = DateTime.parse(restore_time)
        return parsed_time.strftime("%s")
      end
    end
  end
end
