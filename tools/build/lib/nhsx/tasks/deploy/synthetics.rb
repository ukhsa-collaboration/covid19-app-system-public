namespace :deploy do
  NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
    tgt_envs.each do |tgt_env|
      prerequisites = [:"login:#{account}"]
      desc "Deploy synthetic canaries to the #{tgt_env} target environment"
      desc "Deploy synthetic canaries for the current branch in the #{account} account" if tgt_env == "branch"
      task :"synthetics:#{tgt_env}" => prerequisites do
        include NHSx::Terraform
        include NHSx::Generate
        accounts_folder = File.dirname(NHSx::Terraform::SYNTH_DEV_ACCOUNT)
        terraform_configuration = File.join($configuration.base, accounts_folder, account)
        deploy_to_workspace(tgt_env, terraform_configuration, [], $configuration)
        if tgt_env != "branch"
          push_git_tag_subsystem(tgt_env, "synth", "Deployed synthetics on #{tgt_env}", $configuration)
          push_timestamped_tag("synth", tgt_env, "Deployed synthetics on #{tgt_env}", $configuration)
        end
      end
    end
  end
end

namespace :plan do
  NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
    tgt_envs.each do |tgt_env|
      prerequisites = [:"login:#{account}"]
      desc "Create a terraform plan to deploy synthetic canaries into the #{tgt_env} target environment"
      desc "Create a terraform plan to deploy synthetic canaries for the current branch in the dev account" if tgt_env == "branch"
      task :"synthetics:#{tgt_env}" => prerequisites do
        include NHSx::Terraform
        include NHSx::Generate
        accounts_folder = File.dirname(NHSx::Terraform::SYNTH_DEV_ACCOUNT)
        terraform_configuration = File.join($configuration.base, accounts_folder, account)
        plan_for_workspace(tgt_env, terraform_configuration, $configuration)
      end
    end
  end
end

namespace :destroy do
  NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
    tgt_envs.each do |tgt_env|
      prerequisites = [:"login:#{account}"]
      desc "Destroy synthetic canaries in the #{tgt_env} target environment"
      desc "Destroy synthetic canaries for the current branch in the dev account" if tgt_env == "branch"
      task :"synthetics:#{tgt_env}" => prerequisites do
        include NHSx::AWS
        include NHSx::AWS_Synth
        include NHSx::Terraform
        accounts_folder = File.dirname(NHSx::Terraform::SYNTH_DEV_ACCOUNT)
        terraform_configuration = File.join($configuration.base, accounts_folder, account)
        empty_workspace_buckets(tgt_env, terraform_configuration, $configuration)
        delete_workspace(tgt_env, terraform_configuration, $configuration)
        region = "eu-west-1"
        delete_orphan_synth_resources(region, $configuration)
      end
    end
  end
end
