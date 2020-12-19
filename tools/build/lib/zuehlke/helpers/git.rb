require "patir/command"
module Zuehlke
  module Git
    #Returns the short git SHA hash for the current HEAD
    #
    #Assumes it runs *in* the repository
    #
    #Use this when naming things dependent oh the repository version.
    def current_sha
      cmdline = "git rev-parse --short HEAD"
      cmd = Patir::ShellCommand.new(:cmd => cmdline)
      cmd.run
      raise GaudiError, "Could not get SHA. #{cmd.error}" unless cmd.success?
      return cmd.output.chomp
    end

    #Returns the full git SHA hash for the current HEAD
    #
    #Assumes it runs *in* the repository
    #
    #Use this when talking to APIs
    def current_full_sha
      cmdline = "git rev-parse HEAD"
      cmd = Patir::ShellCommand.new(:cmd => cmdline)
      cmd.run
      raise GaudiError, "Could not get SHA. #{cmd.error}" unless cmd.success?
      return cmd.output.chomp
    end

    #Returns the current branch name
    def current_branch
      cmdline = "git rev-parse --abbrev-ref HEAD"
      cmd = Patir::ShellCommand.new(:cmd => cmdline)
      cmd.run
      raise GaudiError, "Could not get SHA. #{cmd.error}" unless cmd.success?
      return cmd.output.chomp
    end

    #Returns the RFC2822 timestamp for the given SHA
    def commit_timestamp sha
      cmdline = "git show -s --format=%cD #{sha}"
      cmd = Patir::ShellCommand.new(:cmd => cmdline)
      cmd.run
      raise GaudiError, "Could not get date. #{cmd.error}" unless cmd.success?
      DateTime.rfc2822(cmd.output)
    end

    #Returns the commit date for the given SHA
    def commit_date sha
      commit_timestamp(sha).strftime("%Y%m%d")
    end

    #Short one line changelog of all changes since from_date
    def changelog from_date
      cmdline = "git log --since=\"#{from_date}\" --oneline --pretty=\"format: %ad %h %s %b\" --date=short"
      cmd = Patir::ShellCommand.new(:cmd => cmdline)
      cmd.run
      raise GaudiError, "Could not get changelog. #{cmd.error}" unless cmd.success?
      cmd.output
    end

    #Returns the SHA of the last commit that changed path
    def last_change_commit path
      cmdline = "git rev-list -2 HEAD -- #{path}"
      cmd = Patir::ShellCommand.new(:cmd => cmdline)
      cmd.run
      raise GaudiError, "Could not get date. #{cmd.error}" unless cmd.success?
      return cmd.output.lines.last.chomp
    end

    def changeset from_sha, to_sha
      git_diff_command = "git diff --name-only #{from_sha} #{to_sha}"
      changed_files = `#{git_diff_command}`
      changed_files = changed_files.split("\n")
      changed_files = changed_files.map { |file| file = File.expand_path(file) }
      return changed_files
    end
  end
end