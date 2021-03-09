namespace :publish do
  namespace :doreto do
    NHSx::TargetEnvironment::DORETO_TARGET_ENVIRONMENTS["dev"].each do |tgt_env|
      desc "Publish the Document Reporting Tool to #{tgt_env}"
      task :"#{tgt_env}" do
        include NHSx::Publish
        include NHSx::Git
        publish_doreto_website("dev", "src/documentation_reporting_tool/dist", "doreto_website_s3", tgt_env, $configuration)
        push_git_tag("#{tgt_env}-doreto", "Published doreto on #{tgt_env}", $configuration) if tgt_env != "branch"
      end
    end
  end
end
