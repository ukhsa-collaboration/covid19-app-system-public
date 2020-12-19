namespace :config do
  task :codebuild => [:"config:codebuild:git"]

  task :"codebuild:git" do
    sh("git config credential.UseHttpPath true")
    sh("git config user.name \"COVID19 AppSystem Bot\"")
    sh("git config user.email bot@nhs.net")
    sh("git config push.default simple")
    sh("git checkout #{ENV["CODEBUILD_SOURCE_VERSION"]}")
  end
end
