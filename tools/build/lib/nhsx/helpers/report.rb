require_relative "target"

module NHSx
  # Helper methods for producing reports
  module Report
    include Gaudi::Utilities
    include NHSx::TargetEnvironment
    include NHSx::Versions
    include NHSx::CodeBuild

    EXCLUDED_FILES = [
      "src/**/*.md",
      "src/aws/lambdas/incremental_distribution/*gradle*",
      "src/aws/lambdas/incremental_distribution/.idea/**/*",
      "src/aws/lambdas/incremental_distribution/build/**/*",
      "src/aws/lambdas/incremental_distribution/gradle/**/*",
      "src/aws/lambdas/incremental_distribution/out/**/*",
      "src/aws/lambdas/incremental_distribution/src/.gradle/**/*",
      "src/aws/lambdas/incremental_distribution/src/test/**/*",
      "src/aws/lambdas/incremental_distribution/target/**/*",
      "src/aws/lambdas/incremental_distribution/tools/**/*",
      "src/aws/lambdas/synthetics/**/*",
    ]
    SIGNIFICANT_FILES = Rake::FileList["src/aws/**/*", "src/static/**/*", "src/proto/**/*"].exclude(*EXCLUDED_FILES)

    def parse_commit_message(msg, sha)
      ticket = nil
      pr = nil
      # remove the SHA
      msg.gsub!(sha, "")
      # Extract the PR number
      if /(\(?\#(\d\d\d\d+)\)?)/ =~ msg
        pr = $2
        msg.gsub!($1, "")
      end

      tickets = []
      # Get ticket number(s)
      while /([Cc][Oo][Vv][- _]\d\d\d\d+)/ =~ msg
        ticket = $1
        msg.gsub!(ticket, "")
        ticket.gsub!(" ", "-")
        ticket.gsub!("_", "-")
        ticket.upcase!
        tickets << ticket
      end
      # Cleanup any free-standing punctuation and tags
      msg.gsub!(/\[.*\]/, "")
      msg.gsub!(/^\s*[:\-_]\s*/, "")
      msg.strip!
      return msg, tickets, pr
    end

    def base_url_report(service, endpoint)
      uri = URI(endpoint)
      puts "* #{service} API base URL: #{uri.scheme}//#{uri.host}"
    rescue
      puts "Failed to determine base URL for #{service} API from #{endpoint}"
    end

    def environment_report(target_env, account, system_config)
      target_config = target_environment_configuration(target_env, account, system_config)
      test_config_file = File.join(system_config.out, "gen/config", "test_config_#{target_env}.json")
      write_file(test_config_file, JSON.dump(target_config))
      target_config["config_file"] = test_config_file
      target_config["deployed_version"] = target_environment_version(target_env, target_config, system_config)
      return target_config
    end

    def significant?(files_changed)
      files_changed.map { |path| SIGNIFICANT_FILES.include?(path) }.uniq.reduce(:|)
    end

    def targets_report(terraform_configuration, named_environments, branches, system_config)
      Dir.chdir(terraform_configuration) do
        init_terraform(system_config)
        run_command("Select default workspace", "terraform workspace select default", system_config)
        cmd = run_command("List workspaces", "terraform workspace list", system_config)
        workspaces = cmd.output.chomp.lines.map(&:chomp).map(&:strip).reject { |ln| ln =~ /default/ }

        temporary_workspaces = workspaces - named_environments

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
  end
end
