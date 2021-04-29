module NHSx
  module Git
    # Returns {tag=>SHA} with all the tags in the repository
    def tags_and_revisions(system_config)
      cmd = run_command("List all tags", "git show-ref --tags --abbrev", system_config)
      repo_tags = {}
      cmd.output.lines.each do |ln|
        tag = ln.split(" ").last.gsub("refs/tags/", "")
        cmd = run_quiet("Get tag revision", "git rev-parse #{tag}^{commit} ", system_config)
        cmd.output.chomp
        repo_tags[tag] = cmd.output.chomp
      end
      return repo_tags
    end

    # Create and push a tag for the given subsystem in the target environment that includes a timestamp
    #
    # The tag will follow the convention te-{tgt_env}-{timestamp}-{subsystem}
    def push_timestamped_tag(subsystem, tgt_env, message, system_config)
      timestamp = Time.now.strftime("%Y%m%d-%s")
      subsystem = "#{subsystem}-" unless subsystem.empty?
      tag("te-#{tgt_env}-#{subsystem}#{timestamp}", message, system_config)
    end

    # Creates and pushes a git tag forcing the tag to be rewritten if it exists.
    def tag(tag, message, system_config)
      cmd1 = "git tag -af #{tag} -m \"#{message}\""
      cmd2 = "git push -f origin #{tag}"
      begin
        run_command("Tag the current SHA", cmd1, system_config)
        run_command("Publish the tag", cmd2, system_config)
      rescue RuntimeError => failure
        puts "#" * 23
        puts("In a native (not container) shell in this directory while logged in to GitHub, please run:")
        puts(cmd1)
        puts(cmd2)
        puts "#" * 23
      end
    end

    # gets list of SHA between two references
    def list_of_commits(from_sha, to_sha)
      cmdline = "git log --pretty='format:%C(auto)%h' #{from_sha}..#{to_sha}"
      cmd = Patir::ShellCommand.new(:cmd => cmdline)
      cmd.run
      cmd.output.split("\n")
    end

    def commit_files(sha)
      cmdline = "git diff-tree --no-commit-id --name-only -r #{sha}"
      cmd = Patir::ShellCommand.new(:cmd => cmdline)
      cmd.run
      cmd.output.split("\n")
    end

    def print_out_changeset(changeset)
      changeset.each do |_, object|
        puts "#{object.fetch("pr", "")} #{object.fetch("tickets", []).join(", ")} #{object["message"]}"
      end
    end

    def commit_message(sha)
      cmdline = "git show --oneline --no-patch #{sha}"
      cmd = Patir::ShellCommand.new(:cmd => cmdline)
      cmd.run
      return cmd.output.chop
    end

    def tag_exists?(tag_name, system_config)
      all_tags = tags_and_revisions(system_config)
      return all_tags.keys.include?(tag_name)
    end
  end
end
