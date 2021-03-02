require "shellwords"
namespace :queue do
  namespace :deploy do
    NHSx::TargetEnvironment::TARGET_ENVIRONMENTS.each do |account, tgt_envs|
      tgt_envs.each do |tgt_env|
        prerequisites = [:"login:#{account}"]
        desc "Queue a deployment to the #{tgt_env} target environment in CodeBuild"
        task :"#{tgt_env}" => prerequisites do
          include NHSx::Queue
          build_info = queue("deploy-cta-#{tgt_env}", tgt_env, account, $configuration)
          if $configuration.print_logs
            pipe_logs(build_info)
            puts "Download the full logs with \n\trake download:codebuild:#{account} JOB_ID=#{build_info.build_id}"
          else
            puts "Job queued. Download logs with \n\trake download:codebuild:#{account} JOB_ID=#{build_info.build_id}"
          end
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
            build_info = queue("deploy-tier-metadata-#{tgt_env}", tgt_env, account, $configuration)
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

    desc "Queue a deployment to cleanup the resources in dev account using CodeBuild"
    task :"cleanup:dev" => [:"login:dev"] do
      include NHSx::Queue
      build_info = queue("resources-cleanup", "", "dev", $configuration)
      if $configuration.print_logs
        pipe_logs(build_info)
        puts "Download the full logs with \n\trake download:codebuild:dev JOB_ID=#{build_info.build_id}"
      else
        puts "Job queued. Download logs with \n\trake download:codebuild:dev JOB_ID=#{build_info.build_id}"
      end
    end
  end
end
