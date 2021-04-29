namespace :deploy do
  namespace :doreto do
    include NHSx::Git
    NHSx::TargetEnvironment::DORETO_TARGET_ENVIRONMENTS["dev"].each do |tgt_env|
      desc "Deploy to the #{tgt_env} DoReTo environment"
      desc "Deploys a temporary DoReTo environment for the current branch in the dev account" if tgt_env == "branch"
      task :"#{tgt_env}" do
        include NHSx::Terraform
        terraform_configuration = File.join($configuration.base, NHSx::TargetEnvironment::DORETO_DEV_ACCOUNT)
        deploy_to_workspace(tgt_env, terraform_configuration, [], $configuration)
        if tgt_env != "branch"
          tag(pointer_tag_name("doreto", tgt_env), "Doreto deployed on #{tgt_env}", $configuration)
        end
      end
    end
  end
end

namespace :plan do
  namespace :doreto do
    NHSx::TargetEnvironment::DORETO_TARGET_ENVIRONMENTS["dev"].each do |tgt_env|
      desc "Run a plan for the #{tgt_env} DoReTo environment"
      desc "Creates the terraform plan of a temporary DoReTo environment for the current branch in the dev account" if tgt_env == "branch"
      task :"#{tgt_env}" do
        include NHSx::Terraform
        terraform_configuration = File.join($configuration.base, NHSx::TargetEnvironment::DORETO_DEV_ACCOUNT)
        plan_for_workspace(tgt_env, terraform_configuration, [], $configuration)
      end
    end
  end
end

namespace :destroy do
  namespace :doreto do
    desc "Destroys the temporary DoReTo environment for the current branch"
    task :branch do
      include NHSx::Terraform
      workspace_name = "branch"
      terraform_configuration = File.join($configuration.base, NHSx::TargetEnvironment::DORETO_DEV_ACCOUNT)
      delete_workspace(workspace_name, terraform_configuration, $configuration)
    end
  end
end
