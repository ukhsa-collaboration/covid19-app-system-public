namespace :publish do
  NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
    tgt_envs.each do |tgt_env|
      desc "Publish tier metadata to #{tgt_env}"
      task :"tier_metadata:#{tgt_env}" => [:"login:#{account}"] do
        include NHSx::Publish
        include NHSx::Terraform
        target_config = target_environment_configuration(tgt_env, account, $configuration)
        terraform_configuration = File.join($configuration.base, "src/aws/accounts", account)
        select_workspace(tgt_env, terraform_configuration, $configuration)
        static_content_file = File.join($configuration.base, "src/static/tier-metadata.json")
        publish_static_content(static_content_file, "tier-metadata", "post_districts_distribution_store", target_config, "", $configuration)

        post_districts_out_dir = File.join($configuration.out, "gen/post_districts")
        key_name = "backup/api-payload"
        distribution_store = "post_districts_distribution_store"
        object_name = "#{target_config[distribution_store]}/#{key_name}"
        local_target = File.join(post_districts_out_dir, key_name)
        run_command("Download current risk data of #{tgt_env} deployment", NHSx::AWS::Commandlines.download_from_s3(object_name, local_target), $configuration)

        ENV["UPLOAD_DATA"] = local_target
        Rake::Task["upload:post_districts:#{tgt_env}"].invoke
        tag("te-#{tgt_env}-i18n", "Translations deployed on #{tgt_env}", $configuration) if tgt_env != "branch"
      end
    end
  end
end
