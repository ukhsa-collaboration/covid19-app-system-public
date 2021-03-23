namespace :queue do
  namespace :deploy do
    NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
      tgt_envs.each do |tgt_env|
        desc "Queue a deployment to the #{tgt_env} target environment in CodeBuild"
        task :"#{tgt_env}" => [:"login:#{account}"] do
          include NHSx::Queue
          branch_name = branch_to_queue(tgt_env, $configuration)
          build_info = queue("deploy-cta-#{tgt_env}", branch_name, tgt_env, account, $configuration)
          if $configuration.print_logs
            pipe_logs(build_info)
            puts "Download the full logs with \n\trake download:codebuild:#{account} JOB_ID=#{build_info.build_id}"
          else
            puts "Job queued. Download logs with \n\trake download:codebuild:#{account} JOB_ID=#{build_info.build_id}"
          end
        end
      end
    end
  end
end
