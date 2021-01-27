require_relative "terraform"

module NHSx
  module Deploy
    include NHSx::Terraform

    # Deploy the COVID19 app system backend
    def deploy_app_system(tgt_env, account, system_config)
      terraform_configuration = File.join(system_config.base, "src/aws/accounts", account)
      raise GaudiError, "Cannot deploy #{tgt_env} in #{account}" unless system_config.account == account

      deploy_to_workspace(tgt_env, terraform_configuration, [], system_config)
      if tgt_env != "branch"
        push_git_tag(tgt_env, "Deployed on #{tgt_env}", system_config)
        tag("te-#{tgt_env}-i18n", "Translations deployed on #{tgt_env}", system_config)
      end
    end
  end
end
