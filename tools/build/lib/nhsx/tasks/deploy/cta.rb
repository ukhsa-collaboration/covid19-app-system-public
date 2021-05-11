namespace :deploy do
  NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
    tgt_envs.each do |tgt_env|
      prerequisites = [:"login:#{account}", :"build:dependencies", :"gen:signatures:#{account}"]
      desc "Deploy to the #{tgt_env} target environment"
      desc "Deploys a temporary target environment for the current branch in the dev account" if tgt_env == "branch"
      task :"#{tgt_env}" => prerequisites do
        include NHSx::Deploy
        deploy_app_system(tgt_env, account, $configuration)
      end
      desc "Full CTA system deployment with analytics and sanity checks"
      task :"cta:#{tgt_env}" => prerequisites do
        begin
          Rake::Task["clean:test:secrets:#{account}"].invoke unless account == "dev"
          Rake::Task["gen:secrets:#{account}"].invoke unless account == "dev"
          Rake::Task["deploy:#{tgt_env}"].invoke
          Rake::Task["publish:tier_metadata:#{tgt_env}"].invoke unless account == "dev" and tgt_env == "branch"
          Rake::Task["test:sanity_check:#{tgt_env}"].invoke
          Rake::Task["report:changes"].invoke
        ensure
          Rake::Task["clean:test:secrets:#{account}"].invoke unless account == "dev"
        end
      end
    end
  end
end

namespace :plan do
  NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
    tgt_envs.each do |tgt_env|
      desc "Run a plan for the #{tgt_env} target environment"
      desc "Creates the terraform plan of a temporary target environment for the current branch in the dev account" if tgt_env == "branch"
      task :"#{tgt_env}" => [:"login:#{account}", :"build:dependencies", :"gen:signatures:#{account}"] do
        include NHSx::Terraform
        terraform_configuration = File.join($configuration.base, "src/aws/accounts", account)
        plan_for_workspace(tgt_env, terraform_configuration, [], $configuration)
      end
    end
  end
end

namespace :destroy do
  desc "Destroys the temporary target environment for the current branch"
  task :branch => :"login:dev" do
    include NHSx::Terraform
    workspace_name = "branch"
    terraform_configuration = File.join($configuration.base, NHSx::TargetEnvironment::CTA_DEV_ACCOUNT)
    delete_workspace(workspace_name, terraform_configuration, $configuration)
  end
end
