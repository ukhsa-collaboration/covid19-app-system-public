module NHSx
  module CodeBuild
    # Interval to wait between repeated AWS requests
    WAIT_TIME = 20
    # Type to wrap the metadata for a CodeBuild job
    class CodeBuildInfo
      attr_reader :build_info

      BUILD_PHASE = "BUILD".freeze
      COMPLETED_PHASE = "COMPLETED".freeze
      STOPPED = "STOPPED".freeze

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

      def stopped?
        return @build_info["buildStatus"] == STOPPED
      end

      def artifacts
        return @build_info["artifacts"]["location"]
      end
    end

    def build_info(build_id)
      cmdline = "aws codebuild batch-get-builds --ids #{build_id}"
      cmd = Patir::ShellCommand.new(:cmd => cmdline)
      cmd.run
      raise GaudiError, "#{cmdline}\n#{cmd.output}\n#{cmd.error}" unless cmd.success?

      build_info = JSON.parse(cmd.output)
      return NHSx::CodeBuild::CodeBuildInfo.new(build_info["builds"].first)
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

      return JSON.parse(cmd.output)["ids"].first
    end

    def all_builds_for_project(project_name)
      cmdline = "aws codebuild list-builds-for-project --project-name #{project_name} --sort-order DESCENDING --max-items 100"
      cmd = Patir::ShellCommand.new(:cmd => cmdline)
      cmd.run
      raise GaudiError, "#{cmdline}\n#{cmd.output}\n#{cmd.error}" unless cmd.success?

      return JSON.parse(cmd.output)["ids"]
    end

    def builds_in_progress(project_name)
      # all latest builds for project
      latest_builds = all_builds_for_project(project_name)

      # collect builds in progress
      builds_in_progress = []
      latest_builds.take_while do |job_id|
        b = build_info(job_id)
        builds_in_progress << b
        !b.completed? || b.stopped?
      end
      return builds_in_progress
    end

    def trim_build_queue(builds_in_progress)
      running_build = nil
      loop do
        running_build = builds_in_progress.pop
        break unless running_build.completed?
      end
      if builds_in_progress.empty?
        last_build = running_build
      else
        rest_of_the_builds = builds_in_progress.reverse
        last_build = rest_of_the_builds.pop

        rest_of_the_builds.each do |bi|
          unless bi.completed?
            puts "Stopping #{bi.build_id}"
            stop_build(bi.build_id)
          end
        end
      end
      return last_build, running_build
    end

    def stop_build(build_id)
      cmdline = "aws codebuild stop-build --id #{build_id}"
      cmd = Patir::ShellCommand.new(:cmd => cmdline)
      cmd.run
      raise GaudiError, "#{cmdline}\n#{cmd.output}\n#{cmd.error}" unless cmd.success?

      return JSON.parse(cmd.output)["build"]
    end

    # Will poll AWS CodeBuild continuously for the status of a running job and
    # pipe the CloudWatch logs from that job to stdout
    def pipe_logs(build_info)
      until build_info.completed?
        stream_log_segments(build_info) if build_info.log_stream_name

        sleep WAIT_TIME
        build_info = build_info(build_info.build_id)
      end
      stream_log_segments(build_info) if build_info.log_stream_name
    end

    def stream_log_segments(build_info)
      next_token = ""
      loop do
        message, next_token = build_log_segment(build_info.log_stream_name, build_info.log_group_name, next_token)
        puts message unless message.empty?

        break if message.empty?
      end
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
