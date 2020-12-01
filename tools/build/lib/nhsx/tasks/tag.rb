namespace :tag do
  include NHSx::Git

  namespace :analytics do
    NHSx::TargetEnvironment::ANALYTICS_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
      tgt_envs.each do |tgt_env|
        desc "Push immutable tag of the current git SHA for the Analytics subsystem to #{tgt_env}"
        task :"#{tgt_env}" => [:"login:#{account}"] do
          push_timestamped_tag("analytics", tgt_env, "Release Analytics on #{tgt_env}", $configuration)
        end
      end
    end
  end

  namespace :"app-system" do
    NHSx::TargetEnvironment::TARGET_ENVIRONMENTS.each do |account, tgt_envs|
      tgt_envs.each do |tgt_env|
        desc "Push immutable tag of the current git SHA for the App System to #{tgt_env}"
        task :"#{tgt_env}" => [:"login:#{account}"] do
          push_timestamped_tag("system", tgt_env, "Release App System on #{tgt_env}", $configuration)
        end
      end
    end
  end

  namespace :doreto do
    NHSx::TargetEnvironment::DORETO_TARGET_ENVIRONMENTS["dev"].each do |tgt_env|
      desc "Push immutable tag of the current git SHA for the Document Reporting Tool to #{tgt_env}"
      task :"#{tgt_env}" do
        push_timestamped_tag("doreto", tgt_env, "Release doreto on #{tgt_env}", $configuration)
      end
    end
  end

  namespace :synth do
    NHSx::TargetEnvironment::TARGET_ENVIRONMENTS.each do |account, tgt_envs|
      tgt_envs.each do |tgt_env|
        desc "Push immutable tag of the current git SHA for the Synthetic Canaries to #{tgt_env}"
        task :"#{tgt_env}" => [:"login:#{account}"] do
          push_timestamped_tag("synth", tgt_env, "Release Synthetic Canaries on #{tgt_env}", $configuration)
        end
      end
    end
  end
end
