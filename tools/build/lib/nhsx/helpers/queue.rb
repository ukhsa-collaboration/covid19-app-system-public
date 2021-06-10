require "patir/command"

module NHSx
  module Queue
    include Zuehlke::Templates
    include NHSx::Versions
    include NHSx::CodeBuild
   

    def queue(build_parameters, system_config)
      build_parameters["release_version"] = "" unless build_parameters["release_version"]     
      build_config = File.join(system_config.out, "codebuild", "#{Time.now.strftime("%Y%m%d%H%M%S")}-#{build_parameters["project_name"]}.json")
      write_file(build_config, from_template(File.join(system_config.base, "tools/templates/codebuild.json.erb"), build_parameters))

      cmdline = "aws codebuild start-build --cli-input-json file://#{build_config}"
      cmd = run_command("Triggering deployment for #{build_parameters["target_environment"]}", cmdline, system_config)
      puts "Deployment triggered"
      build_job_status_json = JSON.parse(cmd.output)
      build_info = CodeBuildInfo.new(build_job_status_json["build"])
      return build_info
    end

    # Determines the branch name to use when queueing a job in CodeBuild
    #
    # Handles the magic 'branch' target environment gracefully by setting the current branch automatically
    # otherwise sets the value passed by the system configuration
    def branch_to_queue(tgt_env, system_config)
      if tgt_env == "branch"
        branch_name = subsystem_version_metadata("backend", system_config)["BranchName"]
      else
        branch_name = system_config.branch
      end
      return branch_name
    end
  end
end
