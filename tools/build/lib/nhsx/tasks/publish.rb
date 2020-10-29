namespace :publish do
  include NHSx::Git
  desc "Publish the devenv docker image to the registry"
  task :devenv => [:"build:devenv"] do
    include NHSx::Docker
    publish_devenv_image($configuration)
  end

  namespace :conpan do
    NHSx::TargetEnvironment::TARGET_ENVIRONMENTS.each do |account, tgt_envs|
      tgt_envs.each do |tgt_env|
        desc "Publish the Control Panel to #{tgt_env}"
        task :"#{tgt_env}" => [:"login:#{account}"] do
          include NHSx::Publish
          publish_conpan_website(account, "src/control_panel/build", "conpan_store", tgt_env, $configuration)
        end
      end
    end
  end

  namespace :doreto do
    NHSx::TargetEnvironment::DORETO_TARGET_ENVIRONMENTS["dev"].each do |tgt_env|
      desc "Publish the Document Reporting Tool to #{tgt_env}"
      task :"#{tgt_env}" do
        include NHSx::Publish
        publish_doreto_website("dev", "src/documentation_reporting_tool/dist", "doreto_website_s3", tgt_env, $configuration)
        if tgt_env != "branch"
          push_git_tag("#{tgt_env}-doreto", "Published doreto on #{tgt_env}", $configuration)
        end
      end
    end
  end

  namespace :data do
    NHSx::TargetEnvironment::TARGET_ENVIRONMENTS["dev"].each do |tgt_env|
      desc "Publish exposure-configuration test data to #{tgt_env}"
      task :"exposure_configuration:#{tgt_env}" do
        include NHSx::Publish
        include NHSx::Terraform
        target_config = target_environment_configuration(tgt_env, "dev", $configuration)
        terraform_configuration = File.join($configuration.base, NHSx::Terraform::DEV_ACCOUNT)
        select_workspace(tgt_env, terraform_configuration, $configuration)
        static_content_file = $configuration.static_content
        publish_static_content(static_content_file, "distribution/exposure-configuration", "exposure_configuration_distribution_store", target_config, $configuration)
      end
      desc "Publish self-isolation test data to #{tgt_env}"
      task :"self_isolation:#{tgt_env}" do
        include NHSx::Publish
        include NHSx::Terraform
        target_config = target_environment_configuration(tgt_env, "dev", $configuration)
        terraform_configuration = File.join($configuration.base, NHSx::Terraform::DEV_ACCOUNT)
        select_workspace(tgt_env, terraform_configuration, $configuration)
        static_content_file = $configuration.static_content
        publish_static_content(static_content_file, "distribution/self-isolation", "self_isolation_distribution_store", target_config, $configuration)
      end
      desc "Publish analytics test data to #{tgt_env}"
      task :"analytics_data:#{tgt_env}" do
        include NHSx::Publish
        include NHSx::Terraform
        terraform_configuration = File.join($configuration.base, NHSx::Terraform::DEV_ACCOUNT)
        select_workspace(tgt_env, terraform_configuration, $configuration)
        test_data_folder = $configuration.test_data
        publish_test_files(test_data_folder, "analytics-data", "analytics_submission_store", tgt_env, $configuration)
      end
    end
  end

  NHSx::TargetEnvironment::TARGET_ENVIRONMENTS.each do |account, tgt_envs|
    tgt_envs.each do |tgt_env|
      desc "Publish tier meta data to #{tgt_env}"
      task :"tier_metadata:#{tgt_env}" => [:"login:#{account}"] do
        include NHSx::Publish
        include NHSx::Terraform
        target_config = target_environment_configuration(tgt_env, account, $configuration)
        terraform_configuration = File.join($configuration.base, "src/aws/accounts", account)
        select_workspace(tgt_env, terraform_configuration, $configuration)
        static_content_file = $configuration.static_content
        publish_static_content(static_content_file, "tier-metadata", "post_districts_distribution_store", target_config, $configuration)

       
        post_districts_out_dir = File.join($configuration.out, "gen/post_districts")
        key_name = "raw/risky-post-districts"
        distribution_store = "post_districts_distribution_store"
        object_name = "#{target_config[distribution_store]}/#{key_name}"
        local_target = File.join(post_districts_out_dir, key_name)
        run_command("Download tier meta data of #{tgt_env} deployment", NHSx::AWS::Commandlines.download_from_s3(object_name, local_target), $configuration)

        ENV["UPLOAD_DATA"] = local_target
        Rake::Task["upload:post_districts:#{tgt_env}"].invoke
       end
    end
  end
end
