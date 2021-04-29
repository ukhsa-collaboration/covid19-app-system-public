namespace :deploy do
  desc "Deploys the mock AWS resources required for dev testing"
  task :"mocks:dev" => [:"login:dev"] do
    include NHSx::Terraform
    terraform_configuration = File.join($configuration.base, "test/aws/accounts/dev")
    deploy_to_workspace("mocks", terraform_configuration, [], $configuration)
  end
end

namespace :plan do
  desc "Plans the mock AWS resource deployment required for dev testing"
  task :"mocks:dev" => [:"login:dev"] do
    include NHSx::Terraform
    terraform_configuration = File.join($configuration.base, "test/aws/accounts/dev")
    plan_for_workspace("mocks", terraform_configuration, [], $configuration)
  end
end

namespace :destroy do
  desc "Destroy the mock AWS resources in dev"
  task :"mocks:dev" => :"login:dev" do
    include NHSx::Terraform
    terraform_configuration = File.join($configuration.base, "test/aws/accounts/dev")
    delete_workspace("mocks", terraform_configuration, $configuration)
  end
end
