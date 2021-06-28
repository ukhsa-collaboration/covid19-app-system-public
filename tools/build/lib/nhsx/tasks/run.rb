namespace :run do
  namespace :parquet_consolidation_job do
    NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
      tgt_envs.each do |tgt_env|
        desc "run the parquet consolidation job"
        task :"#{tgt_env}" => [:"login:#{account}"] do
          prefix = target_environment_name(tgt_env, account, $configuration)

          cmdline = "aws glue start-job-run --job-name #{"#{prefix}-parquet-consolidation"}"
          run_command("Run parquet consolidation job", cmdline, $configuration)
        end
      end
    end
  end
end
