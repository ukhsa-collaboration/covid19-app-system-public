namespace :download do
  namespace :pubdash do
    namespace :all do
      NHSx::TargetEnvironment::PUBDASH_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
        tgt_envs.each do |tgt_env|
          desc "Copy all analytics data from remote #{tgt_env} into local pubdash analytics dir"
          task :"#{tgt_env}" => [:"login:#{account}"] do
            Rake::Task["download:pubdash:app_store:#{tgt_env}"].invoke
            Rake::Task["download:pubdash:qr_posters:#{tgt_env}"].invoke
            Rake::Task["download:pubdash:postcode_lookup:#{tgt_env}"].invoke
            Rake::Task["download:pubdash:mobile_analytics:#{tgt_env}"].invoke
          end
        end
      end
    end
    namespace :app_store do
      NHSx::TargetEnvironment::PUBDASH_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
        tgt_envs.each do |tgt_env|
          desc "Copy app store data from remote #{tgt_env} into local pubdash analytics dir"
          task :"#{tgt_env}" => [:"login:#{account}"] do
            s3_object_location = "te-#{tgt_env}-analytics-app-store-qr-posters/app-store/"
            NHSx::Pubdash::download_recursively($configuration.pubdash_app_store_dir, s3_object_location)
          end
        end
      end
    end
    namespace :qr_posters do
      NHSx::TargetEnvironment::PUBDASH_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
        tgt_envs.each do |tgt_env|
          desc "Copy qr posters data from remote #{tgt_env} into local pubdash analytics dir"
          task :"#{tgt_env}" => [:"login:#{account}"] do
            s3_object_location = "te-#{tgt_env}-analytics-app-store-qr-posters/qr-posters/"
            NHSx::Pubdash::download_recursively($configuration.pubdash_qr_posters_dir, s3_object_location)
          end
        end
      end
    end
    namespace :postcode_lookup do
      NHSx::TargetEnvironment::PUBDASH_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
        tgt_envs.each do |tgt_env|
          desc "Copy postcode lookup data from remote #{tgt_env} into local pubdash analytics dir"
          task :"#{tgt_env}" => [:"login:#{account}"] do
            s3_object_location = "te-#{tgt_env}-analytics-postcode-demographic-geographic-lookup"
            NHSx::Pubdash::download_recursively($configuration.pubdash_postcode_lookup_dir, s3_object_location)
          end
        end
      end
    end
    namespace :mobile_analytics do
      NHSx::TargetEnvironment::PUBDASH_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
        tgt_envs.each do |tgt_env|
          desc "Copy analytics parquet data from remote #{tgt_env} into local pubdash analytics dir"
          task :"#{tgt_env}" => [:"login:#{account}"] do
            s3_object_location = "te-#{tgt_env}-analytics-submission-parquet"
            today = Date.today
            hour_of_day_to_download = 15
            number_of_days_to_download = 60
            (1..number_of_days_to_download)
              .map { |it| today - it }
              .map { |it| it.strftime("%Y/%m/%d") }
              .map { |it| "#{it}/#{hour_of_day_to_download}" }
              .each { |sub_dir|
              s3_subdir_location = "#{s3_object_location}/#{sub_dir}"
              analytics_parquet_subdir = "#{$configuration.pubdash_analytics_parquet_dir}/#{sub_dir}"
              NHSx::Pubdash::download_recursively(analytics_parquet_subdir, s3_subdir_location)
            }
          end
        end
      end
    end
  end
end

namespace :invoke do
  namespace :pubdash do
    namespace :trigger_export do
      NHSx::TargetEnvironment::PUBDASH_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
        tgt_envs.each do |tgt_env|
          desc "Triggers lambda export which will extract latest analytics datasets from athena"
          task :"#{tgt_env}" => [:"login:#{account}"] do
            target_config = pubdash_target_environment_configuration(tgt_env, account, $configuration)
            lambda_function = target_config["pubdash_trigger_export_lambda_function_name"]
            NHSx::AWS::invoke_lambda(lambda_function, "", $configuration)
          end
        end
      end
    end
  end
end
