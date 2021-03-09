require "patir/command"

module NHSx
  module Queue
    include Zuehlke::Templates
    include NHSx::Versions
    include NHSx::CodeBuild

    def queue(project_name, tgt_env, account, system_config)
      if tgt_env == "branch"
        branch_name = subsystem_version_metadata("backend", system_config)["BranchName"]
      else
        branch_name = system_config.branch
      end
      build_parameters = {
        "project_name" => project_name,
        "source_version" => branch_name,
        "target_environment" => tgt_env,
        "account" => account,
      }
      build_config = File.join(system_config.out, "codebuild", "#{Time.now.strftime("%Y%m%d%H%M%S")}-#{project_name}.json")
      write_file(build_config, from_template(File.join(system_config.base, "tools/templates/codebuild.json.erb"), build_parameters))

      cmdline = "aws codebuild start-build --cli-input-json file://#{build_config}"
      cmd = run_command("Triggering deployment for #{tgt_env}", cmdline, system_config)
      puts "Deployment triggered"
      build_job_status_json = JSON.parse(cmd.output)
      build_info = CodeBuildInfo.new(build_job_status_json["build"])
      return build_info
    end
  end
end
