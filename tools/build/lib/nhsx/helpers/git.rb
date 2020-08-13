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
  end
end
