namespace :build do
  desc "Builds the docker image for the development environment"
  task :devenv do
    include Zuehlke::Execution
    include NHSx::Docker
    docker_out = File.join($configuration.out, "docker")

    mkdir_p(docker_out)
    docker_image_sourcefiles($configuration).each do |f|
      cp_r(f, docker_out)
    end

    begin
      content_tag = full_tag(content_version($configuration))
      pull_repository_image($configuration, content_tag)
      tag_content_version_as_latest($configuration, content_tag)
    rescue GaudiError
      # image doesn't exist, build it
      tags = [DEFAULT_VERSION, content_version($configuration)].map { |x| full_tag(x) }
      tag_cmds = tags.map { |label| "-t #{label}" }.join(" ")
      cmdline = "docker build \"#{docker_out}\" #{tag_cmds}"
      run_command("Build #{tags} container image", cmdline, $configuration)
    end
  end

  desc "Adds the external package dependencies to the lambdas"
  task :dependencies => [:"gen:version", :"build:java", :"build:python"]

  task :batch_creation => [:"gen:proto:java"] do
    java_project_path = File.join($configuration.base, "src/aws/lambdas/incremental_distribution")
    pom_xml_path = File.join(java_project_path, "pom.xml")
    java_output_path = File.join($configuration.out, "java/batch_creation")
    jar_file = File.join(java_output_path, "javalambda-0.0.1-SNAPSHOT.jar")
    java_src_pattern = "#{$configuration.base}/src/aws/lambdas/incremental_distribution/src/**/*"

    file jar_file => Rake::FileList[java_src_pattern, pom_xml_path] do
      cmdline = "mvn -P buildProfile -f=#{pom_xml_path} -DbuildOutput=#{java_output_path} clean install"
      run_command("Build incremental distribution lambda", cmdline, $configuration)
    end
    Rake::Task[jar_file].invoke
  end

  task :java => [:"build:batch_creation"]

  desc "Builds the source code for the Control Panel"
  task :conpan => [:"conpan:dependencies"] do
    include Zuehlke::Execution
    Dir.chdir(File.join($configuration.base, "src/control_panel"))
    cmdline = "npm rebuild node-sass"
    run_command("Rebuild node sass", cmdline, $configuration)
    cmdline = "npm run build"
    run_command("Install Control Panel dependencies", cmdline, $configuration)
    puts "Build"
    Dir.chdir($configuration.base)
  end

  task :"conpan:dependencies" do
    include Zuehlke::Execution
    Dir.chdir(File.join($configuration.base, "src/control_panel"))
    cmdline = "npm ci"
    run_command("Install Control Panel dependencies", cmdline, $configuration)
    Dir.chdir($configuration.base)
  end

  desc "Install python packages for AdvancedAnalytics before zipping"
  task :python do
    include NHSx::Python
    include Zuehlke::Execution
    python_project_path = File.join($configuration.base, "src/aws/lambdas/advanced_analytics")
    python_out = File.join($configuration.out, "python/build/advanced_analytics")
    mkdir_p(python_out)

    cp_r("#{python_project_path}/.", python_out)
    install_requirements(python_project_path, python_out, $configuration)
  end
end
