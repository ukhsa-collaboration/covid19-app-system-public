require_relative "terraform"

module NHSx
  module Clean
    include NHSx::Terraform

    # Deletes any resources associated with branch deployments for branches that are no longer present in the repo
    # It does this by listing all terraform workspaces and matching them against the workspace names corresponding to the
    # existing branches in the repository.
    #
    # It then iterates over the list of workspace names corresponding to deleted branches, destroys all resources and deletes the workspace
    def clean_terraform_resources(terraform_configuration, target_environments, system_config)
      branches = list_all_branches(system_config)
      puts "There are #{branches.size} remote branches excluding master"

      Dir.chdir(terraform_configuration) do
        workspaces = list_terraform_workspaces(terraform_configuration, system_config)
        environment_workspaces = target_environments.map { |env| "te-#{env}" }
        temporary_workspaces = workspaces - environment_workspaces
        puts "There are #{temporary_workspaces.size} temporary target environments"

        active_workspaces = branches.map do |branch|
          generate_workspace_id(branch)
        end
        orphan_workspaces = temporary_workspaces - active_workspaces
        puts "There #{orphan_workspaces.size} orphan temporary target environments:\n #{orphan_workspaces.join(",")}"

        remove_workspaces(terraform_configuration, orphan_workspaces, system_config)
      end
    end

    def list_all_branches(system_config)
      cmd = run_command("List all branches", "git branch -r --no-color", system_config)
      cmd.output.gsub("origin/", "").lines.map(&:chomp).map(&:strip).reject { |ln| ln =~ /master/ }
    end

    def list_terraform_workspaces(terraform_configuration, system_config)
      sh("git clean -fdx #{terraform_configuration}")
      init_terraform(system_config)
      run_command("Select default workspace", "terraform workspace select default", system_config)
      cmd = run_command("List workspaces", "terraform workspace list", system_config)
      cmd.output.chomp.lines.map(&:chomp).map(&:strip).reject { |ln| ln =~ /default/ }
    end

    def remove_workspaces(terraform_configuration, orphan_workspaces, system_config)
      orphan_workspaces.each do |workspace_name|
        begin
          sh("git clean -fdx #{terraform_configuration}")
          delete_workspace(workspace_name, terraform_configuration, system_config)
        rescue GaudiError
          puts "Could not delete #{workspace_name}"
        end
      end
    end
  end
end
