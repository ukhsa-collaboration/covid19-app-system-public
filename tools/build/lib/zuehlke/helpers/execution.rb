require "open3"
require "patir/command"

module Zuehlke
  # Methods that allow command line execution with added benefits (like stdout capture and time metrics)
  module Execution
    # Number of lines from the tail of the command output to attach to the exception message on failure
    EXCEPTION_LINES = 25
    # Runs the command line using Patir, creating a new process and suppressing the output of the command.
    #
    # Output, error output, command exit status and total time of execution are captured and returned on success as Patir::ShellCommand instance
    #
    # They are also saved under system_config.out/logs using the provided name (which might lead to failures if the name contains special characters like ':')
    #
    # Will raise a GaudiError if the command fails providing the last EXCEPTION_LINES lines of output as the exception message
    def run_command(name, cmdline, system_config)
      puts cmdline
      cmd = Patir::ShellCommand.new(:cmd => cmdline)
      cmd.run
      logfile = File.join(system_config.out, "logs", "#{Time.now.strftime("%Y%m%d_%H%M%S")}_#{name.gsub("\s", "_")}.log")
      write_file(logfile, "#{cmdline}\n#{cmd.output}\n#{cmd.error}\n")
      if cmd.success?
        puts "#{name} completed in #{cmd.exec_time.round(2)}s"
      else
        puts "#{name} failed after #{cmd.exec_time.round(2)}s"
        if cmd.output && cmd.output.lines.size > EXCEPTION_LINES
          message = "#{cmd.output.lines[-EXCEPTION_LINES..-1].join("\n")}"
        else
          message = cmd.output
        end
        raise GaudiError, "#{cmdline}\n#{message}\n#{cmd.error}"
      end
      return cmd
    end

    # Runs the command line using the rake shell
    #
    # This does not suppress or capture the output.
    #
    # Returns the execution duration
    def run_shell(name, cmdline)
      start_time = Time.now
      sh(cmdline)
      exec_time = Time.now - start_time
      puts "#{name} completed in #{exec_time.round(2)}"
      return exec_time
    end

    # Runs the command line using open3, creating a new process and tee the output to the console and to log files
    #
    # Output, error output, command exit status and total time of execution are captured during execution. They are
    # saved in a log file in system_config.out/logs using the provided name (which might lead to failures if the name
    # contains special characters like ':')
    #
    # Furthermore the output will also be piped to the console.
    #
    # Will raise a GaudiError if the command fails.
    #
    # Returns the execution duration
    def run_tee(name, cmdline, system_config)
      logfile = File.join(system_config.out, "logs", "#{Time.now.strftime("%Y%m%d_%H%M%S")}_#{name.gsub("\s", "_")}.log")
      mkdir_p(File.dirname(logfile), :verbose => false)

      File.open(logfile, "wb") do |out|
        puts cmdline
        out.write("#{cmdline}\n")

        start_time = Time.now
        success = true
        exitstatus = 0
        Open3.popen2(cmdline) do |_stdin, stdout, wait_thr|
          stdout.each_line do |line|
            puts line
            out.write(line)
          end

          success = wait_thr.value.success?
          exitstatus = wait_thr.value.exitstatus
        end
        raise GaudiError, "#{cmdline} failed with error: #{exitstatus}" unless success

        exec_time = Time.now - start_time
        puts "#{name} completed in #{exec_time.round(2)}"
        return exec_time
      end
    end

    # Runs the command line using Patir, creating a new process and suppressing the output of the command.
    # It does not print the command on stdout or in logs
    #
    # Output, error output, command exit status and total time of execution are captured and returned on success as Patir::ShellCommand instance
    # This does not suppress or capture the output.
    #
    # Returns the execution duration
    def run_quiet(name, cmdline, system_config)
      cmd = Patir::ShellCommand.new(:cmd => cmdline)
      cmd.run
      logfile = File.join(system_config.out, "logs", "#{Time.now.strftime("%Y%m%d_%H%M%S")}_#{name.gsub("\s", "_")}.log")
      write_file(logfile, "#{cmd.output}\n#{cmd.error}\n")
      puts "#{name} completed in #{cmd.exec_time.round(2)}"
      if !cmd.success?
        if cmd.output && cmd.output.lines.size > EXCEPTION_LINES
          message = "#{cmd.output.lines[-EXCEPTION_LINES..-1].join("\n")}"
        else
          message = cmd.output
        end
        raise GaudiError, "Failed to run #{name}:\n#{message}\n#{cmd.error}"
      end
      return cmd
    end
  end
end
