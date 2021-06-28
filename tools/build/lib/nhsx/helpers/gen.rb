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

    def generate_local_messages(mapping_filepath, metadata_filepath, system_config)
      mapping = JSON.parse(File.read(mapping_filepath))
      metadata = JSON.parse(File.read(metadata_filepath))

      metadata["messages"] = filter_unused_messages_from_metadata(mapping["las"], metadata["messages"])
      metadata["messages"].each { |_, msg| msg["updated"] = Time.now.utc.iso8601 }
      mapping_metadata = { "las" => mapping["las"], "messages" => metadata["messages"] }
      output_file = "#{system_config.out}/local-messages/local-messages.json"
      write_file(output_file, JSON.pretty_generate(mapping_metadata))
    end

    def filter_unused_messages_from_metadata(la_mapping, messages_metadata)
      used_messages = la_mapping.values.each_with_object([]) do |mapped_msgs, used_msgs|
        used_msgs.append(*mapped_msgs)
      end.uniq
      messages_metadata.select { |k, _| used_messages.include?(k) }
    end
  end
end
