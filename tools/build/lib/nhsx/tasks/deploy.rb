namespace :deploy do
  NHSx::TargetEnvironment::TARGET_ENVIRONMENTS.each do |account, tgt_envs|
    tgt_envs.each do |tgt_env|
      prerequisites = [:"login:#{account}", :"build:dependencies", :"unit:all", :"gen:signatures:#{account}"]
      prerequisites << :"backup:s3:#{tgt_env}" if ["prod", "staging"].include?(tgt_env)
      desc "Deploy to the #{tgt_env} target environment"
      desc "Deploys a temporary target environment for the current branch in the dev account" if tgt_env == "branch"
      task :"#{tgt_env}" => prerequisites do
        include NHSx::Terraform
        terraform_configuration = File.join($configuration.base, "src/aws/accounts", account)
        deploy_to_workspace(tgt_env, terraform_configuration, $configuration)
        if tgt_env != "branch"
          push_git_tag(tgt_env, "Deployed on #{tgt_env}", $configuration)
        end
      end
    end
  end
end

namespace :plan do
  NHSx::TargetEnvironment::TARGET_ENVIRONMENTS.each do |account, tgt_envs|
    tgt_envs.each do |tgt_env|
      desc "Run a plan for the #{tgt_env} target environment"
      desc "Creates the terraform plan of a temporary target environment for the current branch in the dev account" if tgt_env == "branch"
      task :"#{tgt_env}" => [:"login:#{account}", :"build:dependencies", :"unit:all", :"gen:signatures:#{account}"] do
        include NHSx::Terraform
        terraform_configuration = File.join($configuration.base, "src/aws/accounts", account)
        plan_for_workspace(tgt_env, terraform_configuration, $configuration)
      end
    end
  end
end

namespace :destroy do
  desc "Destroys the temporary target environment for the current branch"
  task :branch do
    include NHSx::Terraform
    workspace_name = "branch"
    terraform_configuration = File.join($configuration.base, NHSx::Terraform::DEV_ACCOUNT)
    delete_workspace(workspace_name, terraform_configuration, $configuration)
  end
end
