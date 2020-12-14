require "shellwords"
namespace :queue do
  namespace :deploy do
    NHSx::TargetEnvironment::TARGET_ENVIRONMENTS.each do |account, tgt_envs|
      tgt_envs.each do |tgt_env|
        prerequisites = [:"login:#{account}"]
        desc "Queue a deployment to the #{tgt_env} target environment in CodeBuild"
        task :"#{tgt_env}" => prerequisites do
          include Zuehlke::Templates

          build_parameters = {
            "project_name" => "deploy-app-system",
            "source_version" => $configuration.branch,
            "target_environment" => tgt_env,
            "account" => account,
          }
          build_config = File.join($configuration.out, "codebuild", "#{Time.now.strftime("%Y%m%d%H%M%S")}-deploy-app-system.json")
          write_file(build_config, from_template(File.join($configuration.base, "tools/templates/codebuild.json.erb"), build_parameters))
          cmdline = "aws codebuild start-build --cli-input-json file://#{build_config}"
          cmd = run_command("Triggering deployment for #{tgt_env}", cmdline, $configuration)
          job_metadata = JSON.parse(cmd.output)
          puts "Deployment triggered"
        end
      end
    end
  end
end
