namespace :deploy do
  namespace :analytics do
    NHSx::TargetEnvironment::ANALYTICS_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
      tgt_envs.each do |tgt_env|
        prerequisites = [:"login:#{account}", :"build:analytics"]
        desc "Deploy to the #{tgt_env} analytics environment"
        desc "Deploys a temporary analytics environment for the current branch in the dev account" if /branch$/.match(tgt_env)
        task :"#{tgt_env}" => prerequisites do
          include NHSx::Terraform
          terraform_configuration = File.join($configuration.base, "src/analytics/accounts", account)
          deploy_to_workspace(tgt_env, terraform_configuration, [], $configuration)
          unless /branch$/.match(tgt_env)
            tag(pointer_tag_name("analytics", tgt_env), "Analytics deployed on #{tgt_env}", $configuration)
          end
        end
      end
    end
  end
end

namespace :plan do
  namespace :analytics do
    NHSx::TargetEnvironment::ANALYTICS_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
      tgt_envs.each do |tgt_env|
        prerequisites = [:"login:#{account}", :"build:analytics"]
        desc "Run a plan for the #{tgt_env} analytics environment"
        desc "Creates the terraform plan of a temporary analytics environment for the current branch in the dev account" if /branch$/.match(tgt_env)
        task :"#{tgt_env}" => prerequisites do
          include NHSx::Terraform
          terraform_configuration = File.join($configuration.base, "src/analytics/accounts", account)
          plan_for_workspace(tgt_env, terraform_configuration, [], $configuration)
        end
      end
    end
  end
end

namespace :destroy do
  namespace :analytics do
    ["", "aa-"].each do |prefix|
      tgt_env = "#{prefix}branch"
      account = "#{prefix}dev"
      prerequisites = [:"login:#{account}"]
      desc "Destroys the temporary analytics environment for the current branch"
      task :"#{tgt_env}" => prerequisites do
        include NHSx::Terraform
        terraform_configuration = File.join($configuration.base, NHSx::Terraform::ANALYTICS_DEV_ACCOUNT)
        delete_workspace(tgt_env, terraform_configuration, $configuration)
      end
    end
  end
end
