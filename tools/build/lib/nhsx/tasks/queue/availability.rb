namespace :queue do
    namespace :release do
      desc "Queue a release to the staging target environment in CodeBuild"
      task :"availability:staging" => [:"login:staging"] do
        include NHSx::Queue
        branch_name = branch_to_queue("staging", $configuration)      
        build_parameters = {
              "project_name" => "release-availability-staging",
              "source_version" => branch_name,
              "target_environment" => "staging",
              "account" => "staging"            
            }
        build_info = queue(build_parameters, $configuration)
        if $configuration.print_logs
          pipe_logs(build_info)
          puts "Download the full logs with \n\trake download:codebuild:staging JOB_ID=#{build_info.build_id}"
        else
          puts "Job queued. Download logs with \n\trake download:codebuild:staging JOB_ID=#{build_info.build_id}"
        end
      end
    end
    namespace :release do
      desc "Queue a release to the prod target environment in CodeBuild"
      task :"availability:prod" => [:"login:prod"] do
        include NHSx::Queue
        version_metadata = subsystem_version_metadata("availability", $configuration)
        release_version = $configuration.release_version(version_metadata)
        build_parameters = {
              "project_name" => "release-availability-prod",
              "source_version" => "te-staging",
              "target_environment" => "prod",
              "account" => "prod",
              "release_version" => release_version
            }
        build_info = queue(build_parameters, $configuration)
        if $configuration.print_logs
          pipe_logs(build_info)
          puts "Download the full logs with \n\trake download:codebuild:prod JOB_ID=#{build_info.build_id}"
        else
          puts "Job queued. Download logs with \n\trake download:codebuild:prod JOB_ID=#{build_info.build_id}"
        end
      end
    end
  end
  