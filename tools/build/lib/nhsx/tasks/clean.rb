namespace :clean do
  task :orphans do
    include NHSx::Terraform

    cmd = run_command("List all branches", "git branch -r --no-color", $configuration)
    branches = cmd.output.gsub("origin/", "").lines.map(&:chomp).map(&:strip).reject { |ln| ln =~ /master/ }

    puts "There are #{branches.size} remote branches excluding master"

    terraform_configuration = File.join($configuration.base, NHSx::Terraform::DEV_ACCOUNT)
    Dir.chdir(terraform_configuration) do
      run_command("Select default workspace", "terraform workspace select default", $configuration)
      cmd = run_command("List workspaces", "terraform workspace list", $configuration)
      workspaces = cmd.output.chomp.lines.map(&:chomp).map(&:strip).reject { |ln| ln =~ /default/ }

      temporary_workspaces = workspaces - NHSx::TargetEnvironment::TARGET_ENVIRONMENTS["dev"].map { |env| "te-#{env}" }

      puts "There are #{branches.size} remote branches excluding master"
      puts "There are #{temporary_workspaces.size} temporary target environments"

      active_workspaces = branches.map do |branch|
        generate_workspace_id(branch)
      end
      orphan_workspaces = temporary_workspaces - active_workspaces
      puts "There #{orphan_workspaces.size} orphan temporary target environments:\n #{orphan_workspaces.join(",")}"
      orphan_workspaces.each do |workspace_name|
        begin
          delete_workspace(workspace_name, terraform_configuration, $configuration)
        rescue GaudiError
          puts "Could not delete #{workspace_name}"
        end
      end
    end
  end
  NHSx::TargetEnvironment::TARGET_ENVIRONMENTS.keys.each do |account|
    desc "Remove the API keys used for tests from the #{account} account"
    task :"test:secrets:#{account}" => [:"login:#{account}"] do
      include NHSx::TargetEnvironment

      tst_secrets = test_secrets($configuration)
      tst_secrets.each do |test_secret|
        delete_test_secret(test_secret, $configuration)
      end
      zero_test_authentication_headers($configuration)
    end
  end
  task :config do
    rm_rf(File.join($configuration.out, "gen/config"), :verbose => false)
  end
  NHSx::TargetEnvironment::TARGET_ENVIRONMENTS.keys.each do |account|
    desc "Delete orphaned synthetics lambda layers from #{account} account"
    task :"synth:#{account}" => [:"login:#{account}"] do
      include NHSx::AWS
      region = "eu-west-1"
      layersList = get_orphaned_synthetics_lambda_layers(region, $configuration)
      layersList.each do |layer_name|
        layerVersionList = get_lambda_layer_versions(layer_name, region, $configuration)
        layerVersionList.each do |layer_version|
          delete_lambda_layer_version(layer_name, layer_version, region)
        end
      end
    end
  end
end
