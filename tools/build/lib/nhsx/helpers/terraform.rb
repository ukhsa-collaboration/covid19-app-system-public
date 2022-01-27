require "digest"
require "json"
require_relative "../../gaudi/helpers/utilities"
require_relative "../../zuehlke/helpers/templates"
require_relative "../../zuehlke/helpers/execution"
require_relative "versions"

module NHSx
  # Methods that standardize terraform invocation
  module Terraform
    include Gaudi::Utilities
    include Zuehlke::Templates
    include Zuehlke::Execution
    include NHSx::Versions

    # Runs terraform output for the given configuration and workspace, parses it and returns a Hash
    def terraform_output(terraform_workspace, terraform_configuration, system_config)
      Dir.chdir(terraform_configuration) do
        select_workspace(terraform_workspace, terraform_configuration, system_config)
        cmdline = "terraform output -json"
        cmd = run_command("terraform output for #{terraform_workspace} in #{File.basename(terraform_configuration)}", cmdline, system_config)
        json_output = filter_json_from_output(cmd.output)
        return JSON.parse(json_output)
      end
    end

    # Parses the values out of the terraform output and delivers a {"key"=>"value"} Hash
    def parse_terraform_output(terraform_data)
      transformed_output = {}
      terraform_data.each do |k, v|
        transformed_output[k] = v["value"]
      end
      return transformed_output
    end

    # Extracts json from the given string
    def filter_json_from_output(output)
      output.split("\n").select do |line|
        line.start_with? /\s*{/, /\s*}/, /\s*"/
      end.join("\n")
    end

    # Invokes 'terraform init'
    def init_terraform(system_config)
      run_command("Terraform initialisation", "terraform init", system_config)
    end

    # Invokes 'terraform init' in a given workspace
    # 
    # Terraform has the notion of a "default" workspace, and if you run
    # terraform init without specifying a workspace, it will initialize
    # based on this workspace.
    #
    # We do not use the default workspace anymore, yet there is still
    # some ancient state file on S3 which then gets read by TF.
    #
    # This old statefile is not compatible with 0.14.
    #
    # To work around this issue we'll always run tf init with a given workspace    
    def init_terraform_with_workspace(workspace_name, system_config)
      run_command("Terraform initialisation", "TF_WORKSPACE=#{workspace_name} terraform init", system_config)
    end

    # Codifies the naming convention for terraform workspaces related to the target environment
    #
    # Workspace names ending in "branch" trigger the calculation of the SHA of the current branch name,
    # of which the first 6 hex digits are used
    def target_environment_name(workspace_name, account, system_config)
      if /branch$/.match(workspace_name)
        version_metadata = subsystem_version_metadata("backend", system_config)
        branch_name = version_metadata["BranchName"]
        target_environment = generate_workspace_id(branch_name)
      else
        # "te" stands for "target environment".
        # If CTA_TARGET_ENVIRONMENTS contains a list of environment names for the requested account (dev, staging or prod),
        # and that list contains the workspace name, prefix the workspace name with "te-".
        # If not, leave it unchanged.
        environments = NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS[account] || []
        target_environment = environments.include?(workspace_name) ? "te-#{workspace_name}" : workspace_name
      end
      target_environment
    end

    # Switches the terraform workspace to the given workspace_name
    # (Workspaces map to target environments)
    #
    # This method implements the logic for differentiating between on-demand target environments
    # (which are created using the special workspace name "branch") and named target environments
    #
    # If "branch" is passed as the workspace name, then the SHA1 of the current branch is calculated and used as the target environment identifier
    # otherwise the workspace_name is taken as is.
    #
    # Pay attention to the length of workspace names as some AWS resources have a limit and the terraform workspace is prefixed to all
    # resource identifiers.
    def select_workspace(workspace_name, terraform_configuration, system_config)
      account_name = File.basename(terraform_configuration)
      target_environment = target_environment_name(workspace_name, account_name, system_config)
      Dir.chdir(terraform_configuration) do
        target_environment.start_with?("te-") ? init_terraform_with_workspace(target_environment, system_config) : init_terraform(system_config)
        begin
          run_command("Create #{target_environment} workspace for #{account_name}", "terraform workspace new #{target_environment}", system_config)
        rescue GaudiError
          run_command("Select #{target_environment} workspace for #{account_name}", "terraform workspace select #{target_environment}", system_config)
        end
      end
      return target_environment
    end

    # Deploys the system in a Terraform workspace.
    def deploy_to_workspace(workspace_name, terraform_configuration, tf_varfiles, system_config)
      include NHSx::Generate
      simple_name = File.basename(terraform_configuration)
      workspace_id = select_workspace(workspace_name, terraform_configuration, system_config)
      cmdline = "terraform apply -auto-approve -lock=true -no-color"
      cmdline += " -var-file=#{tf_varfiles.join(" -var-file=")}" unless tf_varfiles.empty?

      Dir.chdir(terraform_configuration) do
        run_tee("Deploy #{workspace_id} for #{simple_name}", cmdline, system_config)
      end
      return workspace_id
    end

    # Deploys the system in a Terraform workspace.
    #
    # If the workspace does not exist, it will be created
    #
    # We do not use the workspace_name directly, rather generate and ID based on it (see generate_workspace_id)
    def plan_for_workspace(workspace_name, terraform_configuration, tf_varfiles, system_config)
      simple_name = File.basename(terraform_configuration)
      workspace_id = select_workspace(workspace_name, terraform_configuration, system_config)
      cmdline = "terraform plan -no-color"
      cmdline += " -var-file=#{tf_varfiles.join(" -var-file=")}" unless tf_varfiles.empty?
      Dir.chdir(terraform_configuration) do
        run_tee("Plan #{workspace_id} for #{simple_name}", cmdline, system_config)
      end
      return workspace_id
    end

    # Deletes the Terraform workspace corresponding to the given name
    def delete_workspace(workspace_name, terraform_configuration, system_config)
      workspace_id = select_workspace(workspace_name, terraform_configuration, system_config)
      Dir.chdir(terraform_configuration) do
        run_command("Destroy #{workspace_id}", "terraform destroy -auto-approve -no-color", system_config)
        run_command("Select default workspace", "terraform workspace select default", system_config)
        run_command("Delete workspace #{workspace_id}", "terraform workspace delete #{workspace_id}", system_config)
      end
    end

    def refresh_workspace(terraform_configuration, system_config)
      simple_name = File.basename(terraform_configuration)
      Dir.chdir(terraform_configuration) do
        run_command("Refresh for #{simple_name}", "terraform refresh", system_config)
      end
    end

    # Calculates the SHA1 of the workspace name and returns the first 6 characters
    #
    # We do this to control the length of the workspace name as it is used in resource IDs that have name length restrictions
    def generate_workspace_id(workspace_name)
      workspace_id = Digest::SHA1.hexdigest(workspace_name)[0..5]
      raise GaudiError, "You are attempting to create/delete a workspace for master. Use the ci workspace" if workspace_name == "master"

      return workspace_id
    end

    def reset_all_resources_in_tfstate
      @all_resources = nil
    end

    def tf_state_mirror(system_config)
      unless @all_resources
        cmd = run_quiet("list terraform state", "terraform state list", system_config)
        @all_resources = TerraformStateTracker.new cmd.output
      end
      @all_resources
    end

    def contains_only_data_resources(system_config, dest)
      tf_state_mirror(system_config).expand_paths(dest).select{ |i| i !~ /\.data\./}.empty?
    end

    def check_path_exists_in_tfstate(system_config, source)
      tf_state_mirror(system_config).resource_exists source
    end
  end
end
