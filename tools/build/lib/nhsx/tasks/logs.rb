# rake logs:cta:ci TOKENS="pgtcvp5w cc8f0b6z" OFFSET="-30"
namespace :logs do
  namespace :cta do
    NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
      tgt_envs.each do |tgt_env|
        desc "Find history of a given CTA Token in the #{tgt_env} target environment"
        task :"#{tgt_env}" do
          include Zuehlke::Execution
          
          if account == "prod"
            Rake::Task["login:prod-support"].invoke
          else
            Rake::Task["login:#{account}"].invoke
          end

          tokens = ENV['TOKENS']
          offset = (ENV['OFFSET'] || '-30').to_i

          cmdline = "cd #{File.join($configuration.base, "tools/cta-token-logs")} && make run ARGS=\"#{tokens} --env #{tgt_env} --offset #{offset}\""
          run_shell("Find history of a given CTA Token in #{tgt_env}", cmdline)
        end
      end
    end
  end
end
  