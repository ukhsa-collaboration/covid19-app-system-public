namespace :report do
  desc "Prints a report on the open branches and associated target environments"
  task :targets => [:"login:dev"] do
    include NHSx::Terraform
    include NHSx::Report
    cmd = run_command("List all branches", "git branch -r --no-color", $configuration)
    branches = cmd.output.gsub("origin/", "").lines.map(&:chomp).map(&:strip).reject { |ln| ln =~ /master/ }

    puts "There are #{branches.size} remote branches excluding master"
    puts "*" * 74
    puts "CTA Core"
    terraform_configuration = File.join($configuration.base, NHSx::TargetEnvironment::CTA_DEV_ACCOUNT)
    named_environments = NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS["dev"].map { |env| "te-#{env}" }
    targets_report(terraform_configuration, named_environments, branches, $configuration)
    puts "*" * 74
    puts "Analytics"
    terraform_configuration = File.join($configuration.base, NHSx::TargetEnvironment::ANALYTICS_DEV_ACCOUNT)
    named_environments = NHSx::TargetEnvironment::ANALYTICS_TARGET_ENVIRONMENTS["dev"].map { |env| "te-#{env}" }
    targets_report(terraform_configuration, named_environments, branches, $configuration)
    puts "*" * 74
    puts "PubDash"
    terraform_configuration = File.join($configuration.base, NHSx::TargetEnvironment::PUBDASH_DEV_ACCOUNT)
    named_environments = NHSx::TargetEnvironment::PUBDASH_TARGET_ENVIRONMENTS["dev"].map { |env| "te-#{env}" }
    targets_report(terraform_configuration, named_environments, branches, $configuration)
  end
  desc "Prints a report on all named target environments and their endpoints in the dev account"
  task :dev => [:"login:dev"] do
    include NHSx::Report
    include NHSx::Git

    te_configs = {}
    repo_tags = tags_and_revisions($configuration)

    NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS["dev"].each do |te|
      next if te == "branch"

      begin
        te_configs[te] = environment_report(te, "dev", $configuration)
      rescue GaudiError
        puts "Failed to query #{te} in #{account}"
      end
    end
    te_configs.each do |env, env_config|
      puts "*" * 74
      puts "* Target environment #{env}"
      base_url_report("distribution", env_config["exposure_configuration_distribution_endpoint"])
      base_url_report("submission", env_config["diagnosis_keys_submission_endpoint"])
      base_url_report("upload", env_config["risky_venues_upload_endpoint"])
      puts "* Deployed version is #{env_config["deployed_version"]}"
      puts " * WARNING: deployed version does not match the repo tag (#{repo_tags.fetch("te-#{env}", "N/A")})" if env_config.fetch("deployed_version", "N/A") != repo_tags.fetch("te-#{env}", "N/A")
      puts "* Full configuration in #{env_config["config_file"]}"
    end
  end
  desc "Prints a report on the prod target environment"
  task :prod => [:"login:prod"] do
    include NHSx::Report
    include NHSx::Git
    repo_tags = tags_and_revisions($configuration)
    env = "prod"
    env_config = environment_report(env, "prod", $configuration)
    puts "*" * 74
    puts "* Target environment #{env}"
    base_url_report("distribution", env_config["exposure_configuration_distribution_endpoint"])
    base_url_report("submission", env_config["diagnosis_keys_submission_endpoint"])
    base_url_report("upload", env_config["risky_venues_upload_endpoint"])
    puts "* Deployed version is #{env_config["deployed_version"]}"
    puts " * WARNING: deployed version does not match the repo tag (#{repo_tags.fetch("te-#{env}", "N/A")})" if env_config.fetch("deployed_version", "N/A") != repo_tags.fetch("te-#{env}", "N/A")
    puts "* Full configuration in #{env_config["config_file"]}"
  end
  desc "Prints a report on the staging target environment"
  task :staging => [:"login:staging"] do
    include NHSx::Report
    include NHSx::Git
    repo_tags = tags_and_revisions($configuration)
    env = "staging"
    env_config = environment_report(env, "staging", $configuration)
    puts "*" * 74
    puts "* Target environment #{env}"
    base_url_report("distribution", env_config["exposure_configuration_distribution_endpoint"])
    base_url_report("submission", env_config["diagnosis_keys_submission_endpoint"])
    base_url_report("upload", env_config["risky_venues_upload_endpoint"])
    puts "* Deployed version is #{env_config["deployed_version"]}"
    puts " * WARNING: deployed version does not match the repo tag (#{repo_tags.fetch("te-#{env}", "N/A")})" if env_config.fetch("deployed_version", "N/A") != repo_tags.fetch("te-#{env}", "N/A")
    puts "* Full configuration in #{env_config["config_file"]}"
  end
  task :releases do
  end
  desc "Generates a report of changes between FROM_VERSION(default the last Backend tag) and TO_VERSION(default local HEAD)"
  task :changes do
    include NHSx::Git
    include NHSx::Report
    include Zuehlke::Templates

    version_metadata = subsystem_version_metadata("backend", $configuration)
    target_commit = $configuration.to_version
    source_commit = $configuration.from_version(version_metadata)

    list_of_commits = list_of_commits(source_commit, target_commit)
    changeset = {}
    list_of_commits.each do |sha|
      msg, tickets, pr = parse_commit_message(commit_message(sha), sha)
      changeset[sha] = { "filelist" => commit_files(sha), "message" => msg, "tickets" => tickets, "pr" => pr }
    end
    significant_changes, insignificant_changes = changeset.partition { |_, object| significant?(object["filelist"]) }
    significant_changes = Hash[significant_changes]
    insignificant_changes = Hash[insignificant_changes]
    puts "*" * 74
    puts "* Significant Changes"
    print_out_changeset(significant_changes)
    puts "*" * 74
    puts "* Additional Changes"
    print_out_changeset(insignificant_changes)
    puts "*" * 74

    template_file = File.join($configuration.base, "tools/templates/changelog.html.erb")
    params = {
      "from_version" => source_commit,
      "to_version" => target_commit,
      "significant_changes" => significant_changes,
      "additional_changes" => insignificant_changes,
    }
    report_file = File.join($configuration.out, "reports/changelog.html")
    write_file(report_file, from_template(template_file, params))
  end
  desc "Provide a list of latest codebuild jobs that are either running or failed"
  task :"codebuild:dev" => [:"login:dev"] do
    include NHSx::CodeBuild

    cb_projects = codebuild_projects($configuration)
    puts "Collecting status of #{cb_projects.length} pipelines"

    job_metadata = cb_projects.map do |project_name|
      latest = latest_build_for_project(project_name)
      latest ? build_info(latest) : nil
    end.compact

    failed_jobs = job_metadata.select { |build| build.build_status == "FAILED" }
    running_jobs = job_metadata.reject(&:completed?)

    puts "*" * 74
    puts "#{running_jobs.length} job(s) running:"
    running_jobs.each do |job|
      puts job.build_id
    end
    puts "*" * 74
    puts "#{failed_jobs.length} job(s) failed:"
    failed_jobs.each do |job|
      puts "#{job.build_id}\n\t rake download:codebuild:dev JOB_ID=#{job.build_id}"
    end
  end
end
