namespace :"refactor-terraform-state" do
  NHSx::TargetEnvironment::ANALYTICS_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
    tgt_envs.each do |tgt_env|
      desc "Refactor tf state in #{tgt_env} #{account} resources=SPEC-FILE [dry-run]"
      task :"analytics:#{tgt_env}" => [:"login:#{account}"] do
        terraform_configuration = File.join($configuration.base, 'src', 'analytics', 'accounts', account)
        refactor_terraform_state terraform_configuration, account, tgt_env
      end
    end
  end

  def refactor_terraform_state(terraform_configuration, account, tgt_env)
    include NHSx::Terraform
    include NHSx::RefactorTerraform
    include NHSx::RakeUtils
    switches = handle_cmdline_switches ARGV, {"dry-run": false}
    dry_run = switches[:"dry-run"]
    puts "The task will run in dry-run mode." if dry_run
    raise GaudiError, "Resources are not defined. Please set 'resources' env variable." if ENV['resources'].nil?
    resources_json = get_targeted_resources($configuration, tgt_env, account, ENV['resources'])
    summary = tf_refactor($configuration, terraform_configuration, tgt_env, account, resources_json.join("\n"), dry_run)
    report_summary = build_report_summary(summary, dry_run)
    puts report_summary
    save_report_summary($configuration, report_summary)
  end

end
