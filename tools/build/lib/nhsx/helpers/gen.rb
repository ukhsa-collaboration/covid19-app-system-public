require_relative "target"
require_relative "versions"
require "json"

module NHSx
  # Helpers for generating configurations and test input data
  module Generate
    include NHSx::TargetEnvironment
    include Gaudi::Utilities
    # Uses terraform output to query the target environment and generate
    # the test configuration file that drives the sanity tests
    #
    # The configuration is generated under out/gen/config
    #
    # Returns the full path to the generated configuration file
    def generate_test_config(environment_name, account_name, system_config)
      test_config_file = File.join(system_config.out, "gen/config", "test_config_#{environment_name}.json")

      target_config = target_environment_configuration(environment_name, account_name, system_config)
      write_file(test_config_file, JSON.dump(target_config))
      return test_config_file
    end

    def generate_analytics_test_config(environment_name, account_name, system_config)
      test_config_file = File.join(system_config.out, "gen/config/analytics", "test_config_#{environment_name}.json")

      target_config = analytics_target_environment_configuration(environment_name, account_name, system_config)
      write_file(test_config_file, JSON.dump(target_config))
      return test_config_file
    end

    def generate_ssh_keypair(system_config)
      key_file = File.join(system_config.out, "ssh", "ephemeral_deploy_id_rsa")
      file key_file do
        mkdir_p(File.dirname(key_file), :verbose => false)
        cmdline = "ssh-keygen -t rsa -C \"ephemeral-deploy@nhsx.nhs.uk\" -f #{key_file}"
        run_command("Create ephemeral SSH keypair", cmdline, system_config)
      end
      Rake::Task[key_file].invoke
      return key_file
    end
  end
end
