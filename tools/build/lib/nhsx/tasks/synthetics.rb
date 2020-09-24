def create_linked_secrets(api_name, auth_key_name, hash_key_name)
  authorization_header = create_and_store_api_key(api_name, hash_key_name)
  name = "/#{api_name}/#{auth_key_name}"
  store_secret(authorization_header, name, "eu-west-1") # cf. src/synthetics/accounts/staging/terraform.tf
end

namespace :synth do

  namespace :secret do
    include Zuehlke::Execution
    include NHSx::Secret
    include BCrypt

    hash_key_name = 'synthetic_canary'
    auth_key_name = "#{hash_key_name}_auth"
    desc "Create hashed secret & auth header for mobile APIs with key synthetic_canary[_auth]"
    task :mobile do # see uk.nhs.nhsx.core.auth.ApiName
      create_linked_secrets("mobile", auth_key_name, hash_key_name)
    end
    desc "Create hashed secret & auth header for test result APIs with key synthetic_canary[_auth]"
    task :test_result do # see uk.nhs.nhsx.core.auth.ApiName
      create_linked_secrets("testResultUpload", auth_key_name, hash_key_name)
    end
    desc "Create hashed secret & auth header for high risk venue APIs with key synthetic_canary[_auth]"
    task :venues do # see uk.nhs.nhsx.core.auth.ApiName
      create_linked_secrets("highRiskVenuesCodeUpload", auth_key_name, hash_key_name)
    end
    desc "Create hashed secret & auth header for high risk post district APIs with key synthetic_canary[_auth]"
    task :post_districts do # see uk.nhs.nhsx.core.auth.ApiName
      create_linked_secrets("highRiskPostCodeUpload", auth_key_name, hash_key_name)
    end
  end

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
          deploy_to_workspace(tgt_env, terraform_configuration, $configuration)
          if tgt_env != "branch"
            push_git_tag_subsystem(tgt_env, "synth", "Deployed synthetics on #{tgt_env}", $configuration)
          end
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
