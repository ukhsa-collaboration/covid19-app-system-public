require "patir/command"

module NHSx
  module Queue
    include Zuehlke::Templates
    include NHSx::Versions
    # Interval to wait between repeated AWS requests
    WAIT_TIME = 20

    class CodeBuildInfo
      attr_reader :build_info

      BUILD_PHASE = "BUILD".freeze
      COMPLETED_PHASE = "COMPLETED".freeze

      def initialize(build_job_status_json)
        @build_info = build_job_status_json
      end

      def log_stream_name
        @build_info["logs"]["streamName"]
      end

      def log_group_name
        @build_info["logs"]["groupName"]
      end

      def build_id
        @build_info["id"]
      end

      def build_number
        @build_info["buildNumber"]
      end

      def current_phase
        @build_info["currentPhase"]
      end

      def build_status
        @build_info["buildStatus"]
      end

      def completed?
        return @build_info["currentPhase"] == COMPLETED_PHASE
      end
    end

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

    # Will poll AWS CodeBuild continuously for the status of a running job and
    # pipe the CloudWatch logs from that job to stdout
    def pipe_logs(build_info)
      next_token = ""
      until build_info.completed?
        if build_info.current_phase == NHSx::Queue::CodeBuildInfo::BUILD_PHASE
          loop do
            message, next_token = build_log_segment(build_info.log_stream_name, build_info.log_group_name, next_token)
            puts message unless message.empty?
            break if message.empty?
          end
        end

        sleep WAIT_TIME
        build_info = build_info(build_info.build_id)
      end
      loop do
        message, next_token = build_log_segment(build_info.log_stream_name, build_info.log_group_name, next_token)
        puts message unless message.empty?
        break if message.empty?
      end
    end

    def build_info(build_id)
      cmdline = "aws codebuild batch-get-builds --ids #{build_id}"
      cmd = Patir::ShellCommand.new(:cmd => cmdline)
      cmd.run
      raise GaudiError, "#{cmdline}\n#{cmd.output}\n#{cmd.error}" unless cmd.success?

      build_info = JSON.parse(cmd.output)
      return NHSx::Queue::CodeBuildInfo.new(build_info["builds"].first)
    end

    def build_log_segment(log_stream_name, log_group_name, log_token)
      cmdline = "aws logs get-log-events --log-group-name #{log_group_name} --log-stream-name #{log_stream_name} --start-from-head "
      cmdline += " --next-token #{log_token}" unless log_token.empty?
      cmd = Patir::ShellCommand.new(:cmd => cmdline)
      cmd.run
      raise GaudiError, "#{cmdline}\n#{cmd.output}\n#{cmd.error}" unless cmd.success?

      output_logs = JSON.parse(cmd.output)
      return output_logs["events"].map { |ev| ev["message"] }.join("\n"), output_logs["nextForwardToken"]
    end
  end
end
