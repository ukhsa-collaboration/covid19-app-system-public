namespace :key_distribution do
    NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
        tgt_envs.each do |tgt_env|
            desc "List all contents of the diagnosis key submission bucket"
            task :"list:#{tgt_env}" do
                include NHSx::AWS
                include Zuehlke::Execution
                          
                if account == "prod"
                    Rake::Task["login:prod-support"].invoke
                else
                    Rake::Task["login:#{account}"].invoke
                end

                prefix = target_environment_name(tgt_env, account, $configuration)
                s3_bucket = "#{prefix}-diagnosis-keys-submission"
                run_command("List contents for #{s3_bucket}", "aws s3 ls s3://#{s3_bucket} --recursive", $configuration)
            end
        end
    end
end
