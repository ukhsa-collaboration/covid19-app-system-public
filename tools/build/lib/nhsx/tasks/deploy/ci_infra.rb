namespace :deploy do
  NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
    desc "Deploys the AWS resources required for operation of the pipelines in #{account}"
    task :"ci-infra:#{account}" => [:"login:#{account}"] do
      include NHSx::Terraform
      include NHSx::Git
      terraform_configuration = File.join($configuration.base, "tools/ci/infra/accounts", account)

      template_file = File.join($configuration.base, "tools/templates/ci-infra.tfvars.erb")
      params = {
        "sha" => current_sha,
        "target_environments" => NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS[account],
      }
      variables_file = File.join($configuration.out, "ci-infra.tfvars")
      write_file(variables_file, from_template(template_file, params))
      deploy_to_workspace("ci-infra", terraform_configuration, [variables_file], $configuration)
    end
  end
end

namespace :plan do
  NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
    desc "Plans the AWS resource deployment required for operation of the pipelines in #{account}"
    task :"ci-infra:#{account}" => [:"login:#{account}"] do
      include NHSx::Terraform
      terraform_configuration = File.join($configuration.base, "tools/ci/infra/accounts", account)
      template_file = File.join($configuration.base, "tools/templates/ci-infra.tfvars.erb")
      params = {
        "sha" => current_sha,
        "target_environments" => NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS[account],
      }
      variables_file = File.join($configuration.out, "ci-infra.tfvars")
      write_file(variables_file, from_template(template_file, params))
      plan_for_workspace("ci-infra", terraform_configuration, [variables_file], $configuration)
    end
  end
end

namespace :destroy do
  desc "Destroy the CodeBuild pipelines in dev"
  task :"ci-infra:dev" => :"login:dev" do
    include NHSx::Terraform
    terraform_configuration = File.join($configuration.base, "tools/ci/infra/accounts", "dev")
    delete_workspace("ci-infra", terraform_configuration, $configuration)
  end
end
