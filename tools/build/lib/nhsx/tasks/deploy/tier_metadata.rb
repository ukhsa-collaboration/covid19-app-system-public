namespace :deploy do
  NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
    tgt_envs.each do |tgt_env|
      desc "Tier metadata content deployment"
      task :"tier_metadata:#{tgt_env}" => [:"login:#{account}"] do
        begin
          Rake::Task["gen:secrets:#{account}"].invoke unless account == "dev"
          Rake::Task["publish:tier_metadata:#{tgt_env}"].invoke
        ensure
          Rake::Task["clean:test:secrets:#{account}"].invoke unless account == "dev"
        end
      end
    end
  end
end
