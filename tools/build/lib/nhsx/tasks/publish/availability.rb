namespace :publish do
  NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
    tgt_envs.each do |tgt_env|
      desc "Publish the iOS and Android availability configuration to #{tgt_env}"
      task :"availability:#{tgt_env}" => [:"login:#{account}", :"gen:signatures:#{account}"] do
        include NHSx::Publish
        include NHSx::Terraform
        target_config = target_environment_configuration(tgt_env, account, $configuration)
        terraform_configuration = File.join($configuration.base, "src/aws/accounts", account)
        select_workspace(tgt_env, terraform_configuration, $configuration)

        #Android
        generated_metadata = File.read(File.join($configuration.out, "signatures/availability-android.json.generated"))
        static_content_file = File.join($configuration.base, "src/static/availability-android.json")
        publish_static_content(static_content_file, "distribution/availability-android", "availability_android_distribution_store", target_config, generated_metadata, $configuration)

        #iOS

        generated_metadata = File.read(File.join($configuration.out, "signatures/availability-ios.json.generated"))
        static_content_file = File.join($configuration.base, "src/static/availability-ios.json")

        publish_static_content(static_content_file, "distribution/availability-ios", "availability_ios_distribution_store", target_config, generated_metadata, $configuration)
        tag("te-#{tgt_env}-availability", "Availability configuration deployed on #{tgt_env}", $configuration) if tgt_env != "branch"
      end
    end
  end
end
