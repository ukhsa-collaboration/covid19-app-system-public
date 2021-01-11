require "shellwords"
namespace :queue do
  namespace :deploy do
    NHSx::TargetEnvironment::TARGET_ENVIRONMENTS.each do |account, tgt_envs|
      tgt_envs.each do |tgt_env|
        prerequisites = [:"login:#{account}"]
        desc "Queue a deployment to the #{tgt_env} target environment in CodeBuild"
        task :"#{tgt_env}" => prerequisites do
          include NHSx::Queue
          queue("deploy-app-system", tgt_env, account, $configuration)
        end
      end
    end

    namespace :tier_metadata do
      NHSx::TargetEnvironment::TARGET_ENVIRONMENTS.each do |account, tgt_envs|
        tgt_envs.each do |tgt_env|
          prerequisites = [:"login:#{account}"]
          desc "Queue a deployment to the #{tgt_env} target environment in CodeBuild"
          task :"#{tgt_env}" => prerequisites do
            include NHSx::Queue
            queue("deploy-tier-metadata", tgt_env, account, $configuration)
          end
        end
      end
    end
  end
end
