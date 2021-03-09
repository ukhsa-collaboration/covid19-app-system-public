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
    include NHSx::CodeBuild

    current_build_id = ENV.fetch("CODEBUILD_BUILD_ID", "")
    raise GaudiError, "No codebuild project detected - missing CODEBUILD_BUILD_ID" if current_build_id.empty?

    project_name = current_build_id.split(":").first

    builds_in_progress = builds_in_progress(project_name)
    puts "#{builds_in_progress.length} build(s) in the queue:\n#{builds_in_progress.map { |e| " #{e.build_id}, #{e.build_number}" }.join("\n")}"
    _, running_build = trim_build_queue(builds_in_progress)
    puts "Waiting on build #{running_build.build_id}"
    loop do
      break if running_build.completed? || current_build_id == running_build.build_id

      sleep 30
      running_build = build_info(running_build.build_id)
    end
    puts "Proceeding with build #{current_build_id}"
  end
end
