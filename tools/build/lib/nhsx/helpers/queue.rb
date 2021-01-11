module NHSx
    module Queue
      include Zuehlke::Templates
      include NHSx::Versions
      def queue(project_name, tgt_env, account, config)
        if tgt_env == "branch"
          branch_name = subsystem_version_metadata("backend", config)["BranchName"]
        else
          branch_name = config.branch
        end
        build_parameters = {
            "project_name" => project_name,
            "source_version" => branch_name,
            "target_environment" => tgt_env,
            "account" => account
        }
        build_config = File.join(config.out, "codebuild", "#{Time.now.strftime("%Y%m%d%H%M%S")}-#{project_name}.json")        
        write_file(build_config, from_template(File.join(config.base, "tools/templates/codebuild.json.erb"), build_parameters))
        cmdline = "aws codebuild start-build --cli-input-json file://#{build_config}"
        cmd = run_command("Triggering deployment for #{tgt_env}", cmdline, config)
        job_metadata = JSON.parse(cmd.output)
        puts "Deployment triggered"        
        end
    end
end