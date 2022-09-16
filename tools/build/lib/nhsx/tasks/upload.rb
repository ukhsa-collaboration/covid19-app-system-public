require_relative "upload/cta"

namespace :upload do
  NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
    tgt_envs.each do |tgt_env|
      desc "Upload post district data to #{tgt_env}"
      task :"post_districts:#{tgt_env}" => [:"login:#{account}"] do
        include NHSx::Generate
        include NHSx::Upload
        upload_file = $configuration.upload_data
        target_config = JSON.parse(File.read(generate_test_config(tgt_env, account, $configuration)))
        status = upload_post_district_data(File.read(upload_file), tgt_env, target_config, $configuration)

        raise GaudiError, "Failed to upload post district data with #{status}" if status != 202

        puts "Post district data successfully completed"
      end

      desc "Upload data to mobile analytics #{tgt_env}"
      task :"analytics:#{tgt_env}" => [:"login:#{account}"] do
        include NHSx::Generate
        include NHSx::Upload
        upload_file = $configuration.upload_data
        target_config = JSON.parse(File.read(generate_test_config(tgt_env, account, $configuration)))
        status = upload_mobile_analytics_data(File.read(upload_file), tgt_env, target_config)

        raise GaudiError, "Failed to upload mobile analytics data with #{status}" if status != 200

        puts "Posted"
      end
    end
  end
end
