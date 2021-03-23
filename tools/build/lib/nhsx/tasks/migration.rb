namespace :migrate do
  namespace :v29 do
    namespace :cb_stats do
      namespace :backup do
        NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
          tgt_envs.each do |tgt_env|
            desc "backup contents of circuit breaker analytics stats to workspace"
            task :"#{tgt_env}" => [:"login:#{account}"] do
              begin
                old_s3_object_location = "te-#{tgt_env}-analytics-circuit-breaker"
                local_dir = File.join($configuration.base, "out/circuitbreaker/analytics", tgt_env)
                NHSx::AWS::download_recursively_from_s3(old_s3_object_location, local_dir, $configuration)
              end
            end
          end
        end
      end

      namespace :restore do
        NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
          tgt_envs.each do |tgt_env|
            desc "restore contents of circuit breaker analytics stats to new bucket"
            task :"#{tgt_env}" => [:"login:#{account}"] do
              begin
                new_s3_object_location = "te-#{tgt_env}-analytics-en-circuit-breaker"
                local_dir = File.join($configuration.base, "out/circuitbreaker/analytics", tgt_env)
                NHSx::AWS::upload_recursively_to_s3(local_dir, new_s3_object_location, $configuration)
              end
            end
          end
        end
      end
    end
  end
end

