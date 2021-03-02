namespace :config do
  task :codebuild => [:"config:codebuild:git", :"codebuild:wait"]

  task :"codebuild:git" do
    sh("git config credential.UseHttpPath true")
    sh("git config user.name \"COVID19 AppSystem Bot\"")
    sh("git config user.email bot@test-and-trace.nhs.uk")
    sh("git config push.default simple")
    if ENV["CODEBUILD_BUILD_ARN"] =~ /build\/ci-app-system/ && ENV["CODEBUILD_INITIATOR"] =~ /GitHub-Hookshot/
      # If triggered by GH and in the CI build, checkout master because the pr/NNNN reference is not there anymore
      sh("git checkout master")
    else
      # this is a workaround for when we use SHA or a tag as the reference.
      # the reason is that we require symbolic reference to be able to traverse the revision tree
      # so, if in detached HEAD mode, we create a local branch to get the symbolic reference
      sh("git checkout #{$configuration.branch}")
      cmd = Patir::ShellCommand.new(:cmd => "git symbolic-ref --short -q HEAD")
      cmd.run
      sh("git checkout -b bot-branch") unless cmd.success?
    end
  end

  task :"codebuild:wait" do
    include NHSx::Report

    project_name = ENV.fetch("CODEBUILD_BUILD_ID", "")
    raise GaudiError, "No codebuild project detected - missing CODEBUILD_BUILD_ID" if project_name.empty?

    project_name = project_name.split(":").first

    # all latest builds for project
    latest_builds = all_builds_for_project(project_name)

    # collect builds in progress
    builds_in_progress = []
    for e in latest_builds
      job_metadata = build_info([e])
      build_job = NHSx::Queue::CodeBuildInfo.new(job_metadata.first)
      if build_job.build_status == "IN_PROGRESS"
        builds_in_progress << build_job
      else
        break
      end
    end

    # print out builds in progress
    puts "builds in progress: #{builds_in_progress.length}"
    builds_in_progress.each { |e|
      puts "#{e.build_id}, #{e.build_number}"
    }

    # poll for the oldest build in progress and compare id with current build
    # if it matches it means its this build's turn otherwise wait and discard completed build
    loop do
      oldest_in_progress = NHSx::Queue::CodeBuildInfo.new(build_info([builds_in_progress.last.build_id]).first)
      puts "checking progress for build: #{oldest_in_progress.build_id}, #{oldest_in_progress.build_number}, #{oldest_in_progress.current_phase}, #{oldest_in_progress.build_status}"

      break if oldest_in_progress.build_id == ENV["CODEBUILD_BUILD_ID"]

      if oldest_in_progress.completed?
        puts "discarded completed build"
        builds_in_progress.pop
      end

      sleep 30
    end

    puts "proceeding with build"
  end
end
