namespace :publish do
  namespace :pubdash do
    NHSx::TargetEnvironment::PUBDASH_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
      tgt_envs.each do |tgt_env|
        desc "Publish public dashboard to #{tgt_env}"
        task :"#{tgt_env}" => [:"login:#{account}"] do
          include NHSx::Publish
          project_dir = File.join($configuration.base, "src/pubdash/webapp")

          build_data_dir = File.join(project_dir, "build/data")
          rm_rf(build_data_dir, :verbose => false)

          build_dir = File.join(project_dir, "build")
          publish_pubdash_website(account, build_dir, "pubdash_website_s3", tgt_env, $configuration)
        end
      end
    end
    namespace :all do
      NHSx::TargetEnvironment::PUBDASH_TARGET_ENVIRONMENTS["dev"].each do |tgt_env|
        desc "Delete and publish new data from local default pubdash analytics dir to #{tgt_env}"
        task :"#{tgt_env}" do
          Rake::Task["publish:pubdash:app_store:#{tgt_env}"].invoke
          Rake::Task["publish:pubdash:qr_posters:#{tgt_env}"].invoke
          Rake::Task["publish:pubdash:postcode_lookup:#{tgt_env}"].invoke
          Rake::Task["publish:pubdash:mobile_analytics:#{tgt_env}"].invoke
        end
      end
    end
    namespace :app_store do
      NHSx::TargetEnvironment::PUBDASH_TARGET_ENVIRONMENTS["dev"].each do |tgt_env|
        desc "Delete s3 contents and publish new data from local default pubdash analytics dir to #{tgt_env}"
        task :"#{tgt_env}" do
          s3_object_location = "te-#{tgt_env}-analytics-app-store-qr-posters/app-store/"
          NHSx::AWS::empty_s3_bucket(s3_object_location, $configuration)
          NHSx::Pubdash::publish_single($configuration.pubdash_app_store_dir, s3_object_location)
        end
      end
    end
    namespace :qr_posters do
      NHSx::TargetEnvironment::PUBDASH_TARGET_ENVIRONMENTS["dev"].each do |tgt_env|
        desc "Delete s3 contents and publish new data from local default pubdash analytics dir to #{tgt_env}"
        task :"#{tgt_env}" do
          s3_object_location = "te-#{tgt_env}-analytics-app-store-qr-posters/qr-posters/"
          NHSx::AWS::empty_s3_bucket(s3_object_location, $configuration)
          NHSx::Pubdash::publish_single($configuration.pubdash_qr_posters_dir, s3_object_location)
        end
      end
    end
    namespace :postcode_lookup do
      NHSx::TargetEnvironment::PUBDASH_TARGET_ENVIRONMENTS["dev"].each do |tgt_env|
        desc "Delete s3 contents and publish new data from local default pubdash analytics dir to #{tgt_env}"
        task :"#{tgt_env}" do
          s3_object_location = "te-#{tgt_env}-analytics-postcode-demographic-geographic-lookup"
          NHSx::AWS::empty_s3_bucket(s3_object_location, $configuration)
          NHSx::Pubdash::publish_single($configuration.pubdash_postcode_lookup_dir, s3_object_location)
        end
      end
    end
    namespace :mobile_analytics do
      NHSx::TargetEnvironment::PUBDASH_TARGET_ENVIRONMENTS["dev"].each do |tgt_env|
        desc "Delete s3 contents and publish new data from local default pubdash analytics dir to #{tgt_env}"
        task :"#{tgt_env}" do
          s3_object_location = "te-#{tgt_env}-analytics-submission-parquet"
          NHSx::AWS::empty_s3_bucket(s3_object_location, $configuration)
          NHSx::Pubdash::publish_recursively($configuration.pubdash_analytics_parquet_dir, s3_object_location)
        end
      end
    end
  end
end
