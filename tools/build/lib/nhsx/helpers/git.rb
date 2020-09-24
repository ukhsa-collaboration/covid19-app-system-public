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

    # Create an immutable tag on the current git SHA for a target environment
    #
    # Example, te-prod-20200916-1600257965
    def push_immutable_git_tag(tgt_env, message, system_config)
      time = "#{Time.now.strftime("%Y%m%d-%s")}"
      tag("te-#{tgt_env}-#{time}", message, system_config)
    end

    # Create an immutable tag on the current git SHA for a target environment
    # on a sub system of the app system
    # Example, te-prod-20200916-1600257965
    def push_immutable_git_tag_subsystem(tgt_env, subsystem, message, system_config)
      time = "#{Time.now.strftime("%Y%m%d-%s")}"
      tag("te-#{tgt_env}-#{subsystem}-#{time}", message, system_config)
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

    def tag(tag, message, system_config)
      run_command("Tag the current SHA", "git tag -af #{tag} -m \"#{message}\"", system_config)
      run_command("Publish the tag", "git push -f origin #{tag}", system_config)
    end
  end
end
