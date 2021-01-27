namespace :deploy do
  NHSx::TargetEnvironment::TARGET_ENVIRONMENTS.each do |account, tgt_envs|
    tgt_envs.each do |tgt_env|
      prerequisites = [:"login:#{account}", :"build:dependencies", :"gen:signatures:#{account}"]
      desc "Deploy to the #{tgt_env} target environment"
      desc "Deploys a temporary target environment for the current branch in the dev account" if tgt_env == "branch"
      task :"#{tgt_env}" => prerequisites do
        include NHSx::Deploy
        deploy_app_system(tgt_env, account, $configuration)
      end
    end

    desc "Deploys the AWS resources required for operation of the pipelines in #{account}"
    task :"ci-infra:#{account}" do
      include NHSx::Terraform
      include NHSx::Git
      terraform_configuration = File.join($configuration.base, "tools/ci/infra/accounts", account)

      template_file = File.join($configuration.base, "tools/templates/ci-infra.tfvars.erb")
      params = {
        "sha" => current_sha,
        "target_environments" => NHSx::TargetEnvironment::TARGET_ENVIRONMENTS[account],
      }
      variables_file = File.join($configuration.out, "ci-infra.tfvars")
      write_file(variables_file, from_template(template_file, params))
      deploy_to_workspace("ci-infra", terraform_configuration, [variables_file], $configuration)
    end
  end
end

namespace :plan do
  NHSx::TargetEnvironment::TARGET_ENVIRONMENTS.each do |account, tgt_envs|
    tgt_envs.each do |tgt_env|
      desc "Run a plan for the #{tgt_env} target environment"
      desc "Creates the terraform plan of a temporary target environment for the current branch in the dev account" if tgt_env == "branch"
      task :"#{tgt_env}" => [:"login:#{account}", :"build:dependencies", :"gen:signatures:#{account}"] do
        include NHSx::Terraform
        terraform_configuration = File.join($configuration.base, "src/aws/accounts", account)
        plan_for_workspace(tgt_env, terraform_configuration, $configuration)
      end
    end
    desc "Plans the AWS resource deployment required for operation of the pipelines in #{account}"
    task :"ci-infra:#{account}" do
      include NHSx::Terraform
      terraform_configuration = File.join($configuration.base, "tools/ci/infra/accounts", account)
      plan_for_workspace("ci-infra", terraform_configuration, $configuration)
    end
  end
end

namespace :destroy do
  desc "Destroys the temporary target environment for the current branch"
  task :branch => :"login:dev" do
    include NHSx::Terraform
    workspace_name = "branch"
    terraform_configuration = File.join($configuration.base, NHSx::Terraform::DEV_ACCOUNT)
    delete_workspace(workspace_name, terraform_configuration, $configuration)
  end
  desc "Destroy the CodeBuild pipelines in dev"
  task :"ci-infra:dev" => :"login:dev" do
    include NHSx::Terraform
    terraform_configuration = File.join($configuration.base, "tools/ci/infra/accounts", "dev")
    delete_workspace("ci-infra", terraform_configuration, $configuration)
  end
end
