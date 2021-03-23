namespace :queue do
  desc "Queue a deployment to cleanup the resources in dev account using CodeBuild"
  task :"cleanup:dev" => [:"login:dev"] do
    include NHSx::Queue
    build_info = queue("resources-cleanup", "master", "", "dev", $configuration)
    if $configuration.print_logs
      pipe_logs(build_info)
      puts "Download the full logs with \n\trake download:codebuild:dev JOB_ID=#{build_info.build_id}"
    else
      puts "Job queued. Download logs with \n\trake download:codebuild:dev JOB_ID=#{build_info.build_id}"
    end
  end
end
