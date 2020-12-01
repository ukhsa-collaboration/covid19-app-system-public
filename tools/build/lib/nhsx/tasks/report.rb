namespace :report do
  desc "Prints a report on the open branches and associated target environments"
  task :targets do
    include NHSx::Terraform
    cmd = run_command("List all branches", "git branch -r --no-color", $configuration)
    branches = cmd.output.gsub("origin/", "").lines.map(&:chomp).map(&:strip).reject { |ln| ln =~ /master/ }

    puts "There are #{branches.size} remote branches excluding master"

    terraform_configuration = File.join($configuration.base, NHSx::Terraform::DEV_ACCOUNT)
    Dir.chdir(terraform_configuration) do
      run_command("Select default workspace", "terraform workspace select default", $configuration)
      cmd = run_command("List workspaces", "terraform workspace list", $configuration)
      workspaces = cmd.output.chomp.lines.map(&:chomp).map(&:strip).reject { |ln| ln =~ /default/ }

      temporary_workspaces = workspaces - NHSx::TargetEnvironment::TARGET_ENVIRONMENTS["dev"].map { |env| "te-#{env}" }

      puts "There are #{branches.size} remote branches excluding master"
      puts "There are #{temporary_workspaces.size} temporary target environments"

      active_workspaces = {}
      branches.each do |branch|
        active_workspaces[branch] = generate_workspace_id(branch)
      end

      active_workspaces.each do |branch, workspace|
        puts "Workspace #{workspace} corresponds to branch #{branch}" if temporary_workspaces.include?(workspace)
      end

      orphan_workspaces = temporary_workspaces - active_workspaces.values
      puts "There #{orphan_workspaces.size} orphan temporary target environments\n #{orphan_workspaces.join(",")}"
    end
  end
  desc "Prints a report on all named target environments and their endpoints in the dev account"
  task :dev do
    include NHSx::Report
    include NHSx::Git

    te_configs = {}
    repo_tags = tags_and_revisions($configuration)

    NHSx::TargetEnvironment::TARGET_ENVIRONMENTS["dev"].each do |te|
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
end
