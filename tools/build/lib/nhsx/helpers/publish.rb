require_relative "target"

module NHSx
  # Methods that codify the conventions for publishing build artifacts
  module Publish
    include NHSx::TargetEnvironment

    # Publishes the given test_data_file as static data for a distribution API
    def publish_static_content(test_data_file, resource_name, distribution_store, target_config, system_config)
      object_name = "#{target_config[distribution_store]}/#{resource_name}"
      cmdline = NHSx::AWS::Commandlines.upload_to_s3(test_data_file, object_name, "application/json")
      puts "Deploying #{test_data_file} as #{resource_name} to #{object_name}"
      run_command("Publish test data", cmdline, system_config)
    end

    # Publishes all files from given test_data_folder as static data for a distribution API
    def publish_test_files(test_data_folder, resource_name, distribution_store, target_environment, system_config)
      target_config = target_environment_configuration(target_environment, "dev", system_config)
      Rake::FileList["#{test_data_folder}/*"].each do |filename|
        target_name = File.basename(filename)
        object_name = "#{target_config[distribution_store]}/#{resource_name}/#{target_name}"
        cmdline = NHSx::AWS::Commandlines.upload_to_s3(filename, object_name, "application/json")
        puts "Deploying #{filename} as #{resource_name}/#{target_name} to #{object_name}"
        run_command("Publish test data", cmdline, system_config)
      end
    end

    # Publish the control panel source code to the hosting S3
    def publish_conpan_website(account, build_path, store_name, target_environment, system_config)
      target_config = target_environment_configuration(target_environment, account, system_config)
      content_base_path = File.join(system_config.base, build_path)
      cmdline = NHSx::AWS::Commandlines.upload_to_s3_recursively(content_base_path, target_config[store_name])
      puts "Uploading #{content_base_path}\nTarget bucket #{target_config[store_name]}"
      run_command("Publish object to hosting bucket", cmdline, system_config)
    end

    # Publish the document reporting tool source code to the hosting S3
    def publish_doreto_website(account, build_path, store_name, target_environment, system_config)
      target_config = doreto_target_environment_configuration(target_environment, account, system_config)
      content_base_path = File.join(system_config.base, build_path)
      cmdline = NHSx::AWS::Commandlines.upload_to_s3_recursively(content_base_path, target_config[store_name])
      puts "Uploading #{content_base_path}\nTarget bucket #{target_config[store_name]}"
      run_command("Publish object to hosting bucket", cmdline, system_config)
    end
  end
end
