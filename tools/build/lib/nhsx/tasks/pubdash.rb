namespace :build do
  desc "Builds public dashboard"
  task :pubdash do
    # public dashboard lambda
    project_path = File.join($configuration.base, "src/aws/lambdas/incremental_distribution")
    build_gradle_path = File.join(project_path, "build.gradle")
    output_path = File.join($configuration.out, "pubdash/build/distributions")
    jar_file = File.join(output_path, "pubdash-0.0.1-SNAPSHOT.zip")
    src_pattern = "#{$configuration.base}/src/aws/lambdas/incremental_distribution/src/**/*"
    gradlew = File.join(project_path, "gradlew")

    file jar_file => Rake::FileList[src_pattern, build_gradle_path] do
      cmdline = "#{gradlew} --console plain -p #{project_path} :pubdash:clean pubdash:lambdaZip"
      run_tee("Build public dashboard lambda", cmdline, $configuration)
    end
    Rake::Task[jar_file].invoke

    # public dashboard webapp
    project_dir = File.join($configuration.base, "src/pubdash/webapp/src/.")
    Dir.chdir(project_dir) do
      run_command("Install public dashboard webapp dependencies", "npm ci", $configuration)
      run_command("Build public dashboard webapp", "npm run build", $configuration)
      run_command("Run public dashboard webapp tests", "npm run test:nowatch", $configuration)
    end
  end
end

namespace :deploy do
  namespace :pubdash do
    NHSx::TargetEnvironment::PUBDASH_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
      tgt_envs.each do |tgt_env|
        desc "Deploy public dashboard to #{tgt_env}"
        desc "Deploys a public dashboard temporary environment for the current branch in the dev account" if tgt_env == "branch"
        task :"#{tgt_env}" => [:"login:#{account}", :"build:pubdash"] do
          include NHSx::Terraform
          terraform_configuration = File.join($configuration.base, "src/pubdash/infrastructure/accounts", account)
          deploy_to_workspace(tgt_env, terraform_configuration, [], $configuration)
          if tgt_env != "branch"
            push_git_tag_subsystem(tgt_env, "pubdash", "Deployed pubdash on #{tgt_env}", $configuration)
          end

          Rake::Task["publish:pubdash:#{tgt_env}"].invoke
        end
      end
    end
  end
end

namespace :plan do
  namespace :pubdash do
    NHSx::TargetEnvironment::PUBDASH_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
      tgt_envs.each do |tgt_env|
        desc "Run a public dashboard plan for the #{tgt_env}"
        desc "Creates the terraform public dashboard plan of a temporary environment for the current branch in the dev account" if tgt_env == "branch"
        task :"#{tgt_env}" => [:"login:#{account}", :"build:pubdash"] do
          include NHSx::Terraform
          terraform_configuration = File.join($configuration.base, "src/pubdash/infrastructure/accounts", account)
          plan_for_workspace(tgt_env, terraform_configuration, [], $configuration)
        end
      end
    end
  end
end

namespace :publish do
  namespace :pubdash do
    NHSx::TargetEnvironment::PUBDASH_TARGET_ENVIRONMENTS["dev"].each do |tgt_env|
      desc "Publish public dashboard to #{tgt_env}"
      task :"#{tgt_env}" do
        include NHSx::Publish
        publish_pubdash_website("dev", "src/pubdash/webapp/build", "pubdash_website_s3", tgt_env, $configuration)
        push_git_tag("#{tgt_env}-pubdash", "Published public dashboard on #{tgt_env}", $configuration) if tgt_env != "branch"
      end
    end
  end
end

namespace :destroy do
  namespace :pubdash do
    desc "Destroys public dashboard temporary environment for the current branch"
    task :branch do
      include NHSx::Terraform
      workspace_name = "branch"
      terraform_configuration = File.join($configuration.base, NHSx::Terraform::PUBDASH_DEV_ACCOUNT)
      delete_workspace(workspace_name, terraform_configuration, $configuration)
    end
  end
end

namespace :clean do
  task :"pubdash:orphans" do
    include NHSx::Terraform

    cmd = run_command("List all branches", "git branch -r --no-color", $configuration)
    branches = cmd.output.gsub("origin/", "").lines.map(&:chomp).map(&:strip).reject { |ln| ln =~ /master/ }

    puts "There are #{branches.size} remote branches excluding master"

    terraform_configuration = File.join($configuration.base, NHSx::Terraform::PUBDASH_DEV_ACCOUNT)
    Dir.chdir(terraform_configuration) do
      run_command("Select default workspace", "terraform workspace select default", $configuration)
      cmd = run_command("List workspaces", "terraform workspace list", $configuration)
      workspaces = cmd.output.chomp.lines.map(&:chomp).map(&:strip).reject { |ln| ln =~ /default/ }

      temporary_workspaces = workspaces - NHSx::TargetEnvironment::PUBDASH_TARGET_ENVIRONMENTS["dev"].map { |env| "te-#{env}" }

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
    namespace :pubdash do
      NHSx::TargetEnvironment::PUBDASH_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
        tgt_envs.each do |tgt_env|
          prerequisites = [:"login:#{account}"]
          desc "Queue a public dashboard deployment to the #{tgt_env} target environment in CodeBuild"
          task :"#{tgt_env}" => prerequisites do
            include NHSx::Queue
            build_info = queue("deploy-pubdash-#{tgt_env}", tgt_env, account, $configuration)
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
