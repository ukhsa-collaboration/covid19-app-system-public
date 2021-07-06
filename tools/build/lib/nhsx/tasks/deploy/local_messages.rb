namespace :deploy do
  NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
    tgt_envs.each do |tgt_env|
      desc "Local messages content deployment"
      task :"local_messages:#{tgt_env}" => [:"login:#{account}"] do
        begin
          Rake::Task["clean:test:secrets:#{account}"].invoke unless account == "dev"
          Rake::Task["gen:secrets:#{account}"].invoke unless account == "dev"
          # ensure no override is possible when deploying
          ENV["MESSAGE_MAPPING"] = "#{$configuration.base}/src/static/local-authority-message-mapping.json"
          Rake::Task["publish:local_messages:#{tgt_env}"].invoke
        ensure
          Rake::Task["clean:test:secrets:#{account}"].invoke unless account == "dev"
        end
      end
    end
  end
end
