namespace :lint do
  desc "Lint all terraform scripts"
  task :terraform do
    sh("terraform fmt -recursive -check -diff")
  end
  desc "Lint all python code"
  task :python do
    include NHSx::Python
    lint_configuration = File.join($configuration.base, "tools/build/config/flake8.config")
    run_tee("Lint python", NHSx::Python::Commandlines.lint(lint_configuration), $configuration)
  end
end
