namespace :check do
  desc "Checks to see if there are any changes to doreto"
  task :doreto do
    include Zuehlke::Execution
    include Zuehlke::Git
    #get the SHA on master
    master_sha = `git log --pretty=%H -n 1 origin/master`
    source_commit_hash = current_sha
    master_sha.chomp!
    puts "Master HEAD is #{master_sha}, source commit is #{source_commit_hash}"
    #find the merge-base with the source_commit_hash
    from_sha = `git merge-base #{master_sha} #{source_commit_hash}`
    from_sha.chomp!
    puts "Common parent is #{from_sha}"
    #get the changeset
    changed_files = changeset(from_sha, source_commit_hash)
    puts "#{changed_files.size} files changed"
    if !changed_files.empty?
      full_path = File.expand_path("src/documentation_reporting_tool")
      changed_files.select! { |f| f.to_s.include?(full_path) }
      if !changed_files.empty?
        puts "Build is required due to\n#{changed_files.join("\n")}"
      else
        puts "Build is not required as no files have changed in src/documentation_reporting_tool"
      end
    end
  end
end

