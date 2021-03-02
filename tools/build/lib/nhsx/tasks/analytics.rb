namespace :deploy do
  namespace :analytics do
    NHSx::TargetEnvironment::TARGET_ENVIRONMENTS.each do |account, tgt_envs|
      tgt_envs.each do |tgt_env|
        prerequisites = [:"login:#{account}"]
        desc "Deploy to the #{tgt_env} analytics environment"
        desc "Deploys a temporary analytics environment for the current branch in the dev account" if tgt_env == "branch"
        task :"#{tgt_env}" => prerequisites do
          include NHSx::Terraform
          terraform_configuration = File.join($configuration.base, "src/analytics/accounts", account)
          deploy_to_workspace(tgt_env, terraform_configuration, [], $configuration)
          if tgt_env != "branch"
            push_git_tag_subsystem(tgt_env, "analytics", "Deployed analytics on #{tgt_env}", $configuration)
          end
        end
      end
    end
  end
end

namespace :plan do
  namespace :analytics do
    NHSx::TargetEnvironment::TARGET_ENVIRONMENTS.each do |account, tgt_envs|
      tgt_envs.each do |tgt_env|
        desc "Run a plan for the #{tgt_env} analytics environment"
        desc "Creates the terraform plan of a temporary analytics environment for the current branch in the dev account" if tgt_env == "branch"
        task :"#{tgt_env}" => [:"login:#{account}"] do
          include NHSx::Terraform
          terraform_configuration = File.join($configuration.base, "src/analytics/accounts", account)
          plan_for_workspace(tgt_env, terraform_configuration, [], $configuration)
        end
      end
    end
  end
end

namespace :destroy do
  namespace :analytics do
    desc "Destroys the temporary analytics environment for the current branch"
    task :branch do
      include NHSx::Terraform
      workspace_name = "branch"
      terraform_configuration = File.join($configuration.base, NHSx::Terraform::ANALYTICS_DEV_ACCOUNT)
      delete_workspace(workspace_name, terraform_configuration, $configuration)
    end
  end
end

namespace :clean do
  task :"analytics:orphans" do
    include NHSx::Terraform

    cmd = run_command("List all branches", "git branch -r --no-color", $configuration)
    branches = cmd.output.gsub("origin/", "").lines.map(&:chomp).map(&:strip).reject { |ln| ln =~ /master/ }

    puts "There are #{branches.size} remote branches excluding master"

    terraform_configuration = File.join($configuration.base, NHSx::Terraform::ANALYTICS_DEV_ACCOUNT)
    Dir.chdir(terraform_configuration) do
      run_command("Select default workspace", "terraform workspace select default", $configuration)
      cmd = run_command("List workspaces", "terraform workspace list", $configuration)
      workspaces = cmd.output.chomp.lines.map(&:chomp).map(&:strip).reject { |ln| ln =~ /default/ }

      temporary_workspaces = workspaces - NHSx::TargetEnvironment::TARGET_ENVIRONMENTS["dev"].map { |env| "te-#{env}" }

      puts "There are #{branches.size} remote branches excluding master"
      puts "There are #{temporary_workspaces.size} temporary target environments"

      active_workspaces = branches.map do |branch|
        generate_workspace_id(branch)
      end
      orphan_workspaces = temporary_workspaces - active_workspaces
      puts "There #{orphan_workspaces.size} orphan temporary target environments:\n #{orphan_workspaces.join(",")}"
      orphan_workspaces.each do |workspace_name|
        begin
          delete_workspace(workspace_name, terraform_configuration, $configuration)
        rescue GaudiError
          puts "Could not delete #{workspace_name}"
        end
      end
    end
  end
end

namespace :queue do
  namespace :deploy do
    namespace :analytics do
      NHSx::TargetEnvironment::TARGET_ENVIRONMENTS.each do |account, tgt_envs|
        tgt_envs.each do |tgt_env|
          prerequisites = [:"login:#{account}"]
          desc "Queue a deployment to the #{tgt_env} target environment in CodeBuild"
          task :"#{tgt_env}" => prerequisites do
            include NHSx::Queue
            build_info = queue("deploy-analytics-#{tgt_env}", tgt_env, account, $configuration)
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
end
