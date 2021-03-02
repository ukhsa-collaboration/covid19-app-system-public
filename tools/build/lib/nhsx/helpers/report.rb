require_relative "target"

module NHSx
  # Helper methods for producing reports
  module Report
    include Gaudi::Utilities
    include NHSx::TargetEnvironment
    include NHSx::Versions

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

    def parse_commit_message(msg, sha)
      ticket = nil
      pr = nil
      #remove the SHA
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

    def codebuild_projects(system_config)
      cmdline = "aws codebuild list-projects --sort-by NAME --sort-order ASCENDING"
      cmd = run_command("List of CodeBuild projects", cmdline, system_config)
      project_names = JSON.parse(cmd.output)["projects"]
      return project_names
    end

    def latest_build_for_project(project_name)
      cmdline = "aws codebuild list-builds-for-project --project-name #{project_name} --sort-order DESCENDING --max-items 1"
      cmd = Patir::ShellCommand.new(:cmd => cmdline)
      cmd.run
      raise GaudiError, "#{cmdline}\n#{cmd.output}\n#{cmd.error}" unless cmd.success?

      return JSON.parse(cmd.output)["ids"]
    end

    def all_builds_for_project(project_name)
      cmdline = "aws codebuild list-builds-for-project --project-name #{project_name} --sort-order DESCENDING --max-items 100"
      cmd = Patir::ShellCommand.new(:cmd => cmdline)
      cmd.run
      raise GaudiError, "#{cmdline}\n#{cmd.output}\n#{cmd.error}" unless cmd.success?

      return JSON.parse(cmd.output)["ids"]
    end

    def build_info(build_ids)
      cmdline = "aws codebuild batch-get-builds --ids #{build_ids.join(" ")}"
      cmd = Patir::ShellCommand.new(:cmd => cmdline)
      cmd.run
      raise GaudiError, "#{cmdline}\n#{cmd.output}\n#{cmd.error}" unless cmd.success?

      return JSON.parse(cmd.output)["builds"]
    end
  end
end
