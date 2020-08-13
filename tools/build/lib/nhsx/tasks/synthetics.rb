namespace :synth do
  namespace :deploy do
    NHSx::TargetEnvironment::TARGET_ENVIRONMENTS.each do |account, tgt_envs|
      tgt_envs.each do |tgt_env|
        prerequisites = [:"login:#{account}"]
        desc "Deploy synthetic canaries to the #{tgt_env} target environment"
        desc "Deploy synthetic canaries for the current branch in the dev account" if tgt_env == "branch"
        task :"#{tgt_env}" => prerequisites do
          include NHSx::Terraform
          include NHSx::Generate
          accounts_folder = File.dirname(NHSx::Terraform::SYNTH_DEV_ACCOUNT)
          terraform_configuration = File.join($configuration.base, accounts_folder, account)
          generate_test_config(tgt_env, account, $configuration)
          deploy_to_workspace(tgt_env, terraform_configuration, $configuration)
          # if tgt_env != "branch"
          #   # tag the SHA with the environment tag
          #   run_command("Tag the deployed SHA", "git tag -af te-#{tgt_env} -m \"Synthetic canaries deployed on #{tgt_env}\"", $configuration)
          #   run_command("Publish the tag", "git push --force-with-lease --tags", $configuration)
          # end
        end
      end
    end
  end

  namespace :plan do
    NHSx::TargetEnvironment::TARGET_ENVIRONMENTS.each do |account, tgt_envs|
      tgt_envs.each do |tgt_env|
        prerequisites = [:"login:#{account}"]
        desc "Create a terraform plan to deploy synthetic canaries into the #{tgt_env} target environment"
        desc "Create a terraform plan to deploy synthetic canaries for the current branch in the dev account" if tgt_env == "branch"
        task :"#{tgt_env}" => prerequisites do
          include NHSx::Terraform
          include NHSx::Generate
          accounts_folder = File.dirname(NHSx::Terraform::SYNTH_DEV_ACCOUNT)
          terraform_configuration = File.join($configuration.base, accounts_folder, account)
          generate_test_config(tgt_env, account, $configuration)
          plan_for_workspace(tgt_env, terraform_configuration, $configuration)
        end
      end
    end
  end

  namespace :destroy do
    NHSx::TargetEnvironment::TARGET_ENVIRONMENTS.each do |account, tgt_envs|
      tgt_envs.each do |tgt_env|
        prerequisites = [:"login:#{account}"]
        desc "Destroy synthetic canaries in the #{tgt_env} target environment"
        desc "Destroy synthetic canaries for the current branch in the dev account" if tgt_env == "branch"
        task :"#{tgt_env}" => prerequisites do
          include NHSx::Terraform
          accounts_folder = File.dirname(NHSx::Terraform::SYNTH_DEV_ACCOUNT)
          terraform_configuration = File.join($configuration.base, accounts_folder, account)
          delete_workspace(tgt_env, terraform_configuration, $configuration)
        end
      end
    end
  end
end
