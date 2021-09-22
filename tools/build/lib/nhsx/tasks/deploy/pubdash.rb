namespace :deploy do
  namespace :pubdash do
    NHSx::TargetEnvironment::PUBDASH_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
      tgt_envs.each do |tgt_env|
        desc "Deploy public dashboard backend and frontend to #{tgt_env}"
        desc "Deploys a public dashboard backend and frontend temporary environment for the current branch in the dev account" if tgt_env == "branch"
        task :"#{tgt_env}" => [:"login:#{account}", :"build:pubdash:backend"] do
          include NHSx::Terraform
          terraform_configuration = File.join($configuration.base, "src/pubdash/infrastructure/accounts", account)
          deploy_to_workspace(tgt_env, terraform_configuration, [], $configuration)
          if ["dev", "staging", "prod"].include?(account)
            puts "Skipping cta account website publish task"
          else
            Rake::Task["publish:pubdash:#{tgt_env}"].invoke
          end
          tag(pointer_tag_name("pubdash", tgt_env), "Public dashboard deployed on #{tgt_env}", $configuration) if tgt_env != "branch"
        end
      end
    end
    namespace :backend do
      NHSx::TargetEnvironment::PUBDASH_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
        tgt_envs.each do |tgt_env|
          desc "Deploy public dashboard backend/infrastructure to #{tgt_env}"
          desc "Deploys a public dashboard backend/infrastructure temporary environment for the current branch in the dev account" if tgt_env == "branch"
          task :"#{tgt_env}" => [:"login:#{account}", :"build:pubdash:backend"] do
            include NHSx::Terraform
            terraform_configuration = File.join($configuration.base, "src/pubdash/infrastructure/accounts", account)
            deploy_to_workspace(tgt_env, terraform_configuration, [], $configuration)
            tag(pointer_tag_name("pubdash-backend", tgt_env), "Public dashboard backend deployed on #{tgt_env}", $configuration) if tgt_env != "branch"
          end
        end
      end
    end
  end
end

namespace :plan do
  namespace :pubdash do
    NHSx::TargetEnvironment::PUBDASH_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
      tgt_envs.each do |tgt_env|
        desc "Run a public dashboard plan for the #{tgt_env}"
        desc "Creates the terraform public dashboard plan of a temporary environment for the current branch in the dev account" if tgt_env == "branch"
        task :"#{tgt_env}" => [:"login:#{account}", :"build:pubdash:backend", :"build:pubdash:frontend:#{account}"] do
          include NHSx::Terraform
          terraform_configuration = File.join($configuration.base, "src/pubdash/infrastructure/accounts", account)
          plan_for_workspace(tgt_env, terraform_configuration, [], $configuration)
        end
      end
    end
    namespace :backend do
      NHSx::TargetEnvironment::PUBDASH_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
        tgt_envs.each do |tgt_env|
          desc "Deploy public dashboard backend/infrastructure to #{tgt_env}"
          desc "Deploys a public dashboard backend/infrastructure temporary environment for the current branch in the dev account" if tgt_env == "branch"
          task :"#{tgt_env}" => [:"login:#{account}", :"build:pubdash:backend"] do
            include NHSx::Terraform
            terraform_configuration = File.join($configuration.base, "src/pubdash/infrastructure/accounts", account)
            plan_for_workspace(tgt_env, terraform_configuration, [], $configuration)
          end
        end
      end
    end
  end
end

namespace :destroy do
  namespace :pubdash do
    desc "Destroys public dashboard temporary environment for the current branch"
    task :branch do
      include NHSx::Terraform
      workspace_name = "branch"
      terraform_configuration = File.join($configuration.base, NHSx::Terraform::PUBDASH_DEV_ACCOUNT)
      delete_workspace(workspace_name, terraform_configuration, $configuration)
    end
  end
end
