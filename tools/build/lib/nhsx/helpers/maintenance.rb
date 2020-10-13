require "json"

module NHSx
  # Helpers codifying the concept of maintenance mode for APIs
  # See doc/design/api-maintenance-mode.md
  module Maintenance
    include Zuehlke::Execution
    include NHSx::TargetEnvironment
    # Set all virology APIs in operation mode
    def activate_virology(tgt_env, account, system_config)
      target_environment_config = target_environment_configuration(tgt_env, account, system_config)
      enable_api(target_environment_config["virology_submission_lambda_function_name"], system_config)
      enable_api(target_environment_config["virology_upload_lambda_function_name"], system_config)
    end

    # Set all virology APIs in maintenance mode
    def deactivate_virology(tgt_env, account, system_config)
      target_environment_config = target_environment_configuration(tgt_env, account, system_config)
      disable_api(target_environment_config["virology_submission_lambda_function_name"], system_config)
      disable_api(target_environment_config["virology_upload_lambda_function_name"], system_config)
    end

    # Disable an API
    #
    # Sets MAINTENANCE_MODE to true for the API lambda function
    def disable_api(lambda_function_name, system_config)
      current_env_vars = get_lambda_env_vars(lambda_function_name, system_config)
      new_env_vars = configure_maintenance_mode(current_env_vars)
      update_lambda_environment(lambda_function_name, new_env_vars, system_config)
    end

    # Enable an API
    #
    # Sets MAINTENANCE_MODE to false for the API lambda function
    def enable_api(lambda_function_name, system_config)
      current_env_vars = get_lambda_env_vars(lambda_function_name, system_config)
      new_env_vars = configure_operation_mode(current_env_vars)
      update_lambda_environment(lambda_function_name, new_env_vars, system_config)
    end

    # Updates the environment variables configuration on a lambda
    #
    # This will force AWS to reset the lambda
    def update_lambda_environment(lambda_function_name, lambda_env_vars, system_config)
      shorthand = "{#{lambda_env_vars.to_a.map { |el| "#{el.first}=#{el.last}" }.join(",")}}"
      cmdline = "aws lambda update-function-configuration --function-name #{lambda_function_name} --environment Variables=#{shorthand}"
      run_command("Set environment for #{lambda_function_name}", cmdline, system_config)
    end

    # Gets the current environment variable configuration for a lambda
    def get_lambda_env_vars(lambda_function_name, system_config)
      cmdline = "aws lambda get-function-configuration --function-name  #{lambda_function_name}"
      cmd = run_command("Gets current ENV_VAR config for #{lambda_function_name}", cmdline, system_config)
      env_vars = JSON.parse(cmd.output)
      return env_vars["Environment"]["Variables"]
    end

    # Changes MAINTENANCE_MODE in lambda_env_vars to true
    def configure_maintenance_mode(lambda_env_vars)
      lambda_env_vars["MAINTENANCE_MODE"] = "true"
      return lambda_env_vars
    end

    # Changes MAINTENANCE_MODE in lambda_env_vars to false
    def configure_operation_mode(lambda_env_vars)
      lambda_env_vars["MAINTENANCE_MODE"] = "false"
      return lambda_env_vars
    end
  end
end
