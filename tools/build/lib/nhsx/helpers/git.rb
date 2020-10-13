module NHSx
  module Git
    # Returns {tag=>SHA} with all the tags in the repository
    def tags_and_revisions(system_config)
      cmd = run_command("List all tags", "git show-ref --tags --abbrev", system_config)
      repo_tags = {}
      cmd.output.lines.each do |ln|
        tag = ln.split(" ").last.gsub("refs/tags/", "")
        cmd = run_command("Get tag revision", "git rev-parse #{tag}^{commit} ", system_config)
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

    # Tag the current git SHA with the environment tag
    #
    # Example, te-prod
    def push_git_tag(tgt_env, message, system_config)
      tag("te-#{tgt_env}", message, system_config)
    end

    # Tag the current git SHA with the environment tag
    # on a sub system of the app system
    # Example, te-prod
    def push_git_tag_subsystem(tgt_env, subsystem, message, system_config)
      tag("te-#{tgt_env}-#{subsystem}", message, system_config)
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
  end
end
