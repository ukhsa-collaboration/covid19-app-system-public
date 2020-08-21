namespace :build do
  desc "Builds the docker image for the development environment"
  task :devenv do
    include Zuehlke::Execution
    docker_out = File.join($configuration.out, "docker")
    docker_tag = NHSx::Docker::DEVENV
    docker_image_version = "latest"
    full_tag = "#{docker_tag}-#{docker_image_version}"
    mkdir_p(docker_out)
    cp_r("#{$configuration.base}/tools/build/Gemfile", docker_out)
    cp_r("#{$configuration.base}/tools/provisioning/python/requirements.txt", docker_out)
    cp_r("#{$configuration.base}/tools/provisioning/dev/Dockerfile", docker_out)
    cp_r("#{$configuration.base}/src/aws/lambdas/incremental_distribution/pom.xml", docker_out)
    cmdline = "docker build \"#{docker_out}\" -t #{full_tag}"
    run_command("Build #{full_tag} container image", cmdline, $configuration)
  end

  desc "Builds the docker image for the document reporting tool"
  task :doreto do
    include Zuehlke::Execution
    docker_tag = NHSx::Docker::DORETO
    docker_image_version = "latest"
    full_tag = "#{docker_tag}-#{docker_image_version}"
    dockerfile_dir = File.join($configuration.base, "src/document_reporting")
    cmdline = "docker build \"#{dockerfile_dir}\" -t #{full_tag}"
    run_command("Build #{full_tag} container image", cmdline, $configuration)
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
