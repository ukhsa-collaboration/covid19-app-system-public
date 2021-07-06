namespace :release do
  desc "Release of version RELEASE_VERSION in staging of the full CTA system"
  task :"cta:staging" => [:"login:staging"] do
    puts "Initiating CTA release to staging"
    Rake::Task["deploy:cta:staging"].invoke
  end
  desc "Release of the tier metadata to staging"
  task :"tier_metadata:staging" => [:"login:staging"] do
    puts "Initiating CTA tier metadata release to staging"
    Rake::Task["deploy:tier_metadata:staging"].invoke
  end
  desc "Release of the availability configuration to staging"
  task :"availability:staging" => [:"login:staging"] do
    puts "Initiating CTA availability configuration release to staging"
    Rake::Task["deploy:availability:staging"].invoke
  end
  desc "Release of the local messages to staging"
  task :"local_messages:staging" => [:"login:staging"] do
    puts "Initiating CTA local messages release to staging"
    Rake::Task["deploy:local_messages:staging"].invoke
  end
  desc "Release of the analytics system to staging"
  task :"analytics:staging" => [:"login:staging"] do
    puts "Initiating CTA analytics release to staging"
    Rake::Task["deploy:analytics:staging"].invoke
  end
  desc "Release of the analytics system to aa-staging"
  task :"analytics:aa-staging" => [:"login:aa-staging"] do
    puts "Initiating CTA analytics release to aa-staging"
    Rake::Task["deploy:analytics:aa-staging"].invoke
  end
  desc "Release of the public dashboard to staging"
  task :"pubdash:staging" => [:"login:staging"] do
    puts "Initiating CTA public dashboard release to staging"
    Rake::Task["deploy:pubdash:staging"].invoke
  end
end
