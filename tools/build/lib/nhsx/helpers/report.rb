require_relative "target"

module NHSx
  # Helper methods for producing reports
  module Report
    include Gaudi::Utilities
    include NHSx::TargetEnvironment
    include NHSx::Versions

    def base_url_report(service, endpoint)
      uri = URI(endpoint)
      puts "* #{service} API base URL: #{uri.scheme}//#{uri.host}"
    rescue
      puts "Failed to determine base URL for #{service} API from #{endpoint}"
    end

    def environment_report(target_env, account, system_config)
      target_config = target_environment_configuration(target_env, account, system_config)
      test_config_file = File.join(system_config.out, "gen/config", "test_config_#{target_env}.json")
      write_file(test_config_file, JSON.dump(target_config))
      target_config["config_file"] = test_config_file
      target_config["deployed_version"] = target_environment_version(target_env, target_config, system_config)
      return target_config
    end
  end
end
