namespace :prepare do
  desc "Prepares a local git workspace for a clean deployment"
  task :deploy do
    sh("git fetch --tags -f -p")
    sh("git checkout master")
    sh("git pull --rebase")
    sh("git clean -fdx")
    sh("git reset --hard")
  end
  desc "Prepares a local git worksapce for a clean deployment to prod"
  task :"deploy:prod" => [:"prepare:deploy"] do
    sh("git checkout te-staging")
  end
end
