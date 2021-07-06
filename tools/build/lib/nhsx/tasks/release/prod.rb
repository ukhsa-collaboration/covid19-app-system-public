namespace :release do
  def configure_release_process(bubble, system_config)
    sh("git config credential.helper 'cache'")
    temp_branch_name = "release-#{bubble}-#{Time.now.strftime("%Y%m%d%H%M%S")}"
    sh("git checkout -b #{temp_branch_name}")
    version_metadata = subsystem_version_metadata(bubble, system_config)
    system_config.release_version(version_metadata)
  end

  desc "Release of version RELEASE_VERSION in prod of the full CTA system"
  task :"cta:prod" => [:"clean:wipe", :"login:prod"] do
    include NHSx::Queue

    release_version = configure_release_process("cta", $configuration)
    puts "Initiating CTA release #{release_version}"

    Rake::Task["deploy:cta:prod"].invoke
    Rake::Task["tag:release:cta"].invoke
    Rake::Task["tag:release:tier_metadata"].invoke
    Rake::Task["tag:release:availability"].invoke
    Rake::Task["tag:release:local_messages"].invoke
  end
  desc "Release of version RELEASE_VERSION of the tier metadata"
  task :"tier_metadata:prod" => [:"clean:wipe", :"login:prod"] do
    release_version = configure_release_process("tiers", $configuration)
    puts "Initiating CTA tier metadata release #{release_version}"

    Rake::Task["deploy:tier_metadata:prod"].invoke
    Rake::Task["tag:release:tier_metadata"].invoke
  end
  desc "Release of version RELEASE_VERSION of the availability configuration"
  task :"availability:prod" => [:"clean:wipe", :"login:prod"] do
    release_version = configure_release_process("availability", $configuration)
    puts "Initiating CTA availability configuration release #{release_version}"

    Rake::Task["deploy:availability:prod"].invoke
    Rake::Task["tag:release:availability"].invoke
  end
  desc "Release version RELEASE_VERSION of local messaging configuration to prod"
  task :"local_messages:prod" => [:"clean:wipe", :"login:prod"] do
    release_version = configure_release_process("local_messages", $configuration)
    puts "Initiating CTA local messages configuration release #{release_version}"

    Rake::Task["deploy:local_messages:prod"].invoke
    Rake::Task["tag:release:local_messages"].invoke
  end
  desc "Release of version RELEASE_VERSION of the analytics system to prod"
  task :"analytics:prod" => [:"clean:wipe", :"login:prod"] do
    release_version = configure_release_process("analytics", $configuration)
    puts "Initiating CTA analytics release #{release_version}"

    Rake::Task["deploy:analytics:prod"].invoke
  end
  desc "Release of version RELEASE_VERSION of the analytics system to aa-prod"
  task :"analytics:aa-prod" => [:"clean:wipe", :"login:aa-prod"] do
    release_version = configure_release_process("analytics", $configuration)
    puts "Initiating CTA analytics release #{release_version}"
    Rake::Task["deploy:analytics:aa-prod"].invoke
    Rake::Task["tag:release:analytics"].invoke
  end
  desc "Release of version RELEASE_VERSION of the public dashboard"
  task :"pubdash:prod" => [:"clean:wipe", :"login:prod"] do
    release_version = configure_release_process("pubdash", $configuration)
    puts "Initiating CTA public dashboard release #{release_version}"

    Rake::Task["deploy:pubdash:prod"].invoke
    Rake::Task["tag:release:pubdash"].invoke
  end
end
