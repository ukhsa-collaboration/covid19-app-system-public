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
  namespace :local_stats do
    namespace :lambda do
      NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
        tgt_envs.each do |tgt_env|
          desc "Triggers local stats lambda"
          task :"#{tgt_env}" => [:"login:#{account}"] do
            target_config = target_environment_configuration(tgt_env, account, $configuration)
            lambda_function = target_config["local_stats_processing_function"]
            NHSx::AWS::invoke_lambda(lambda_function, "", $configuration)
          end
        end
      end
    end
  end
end
