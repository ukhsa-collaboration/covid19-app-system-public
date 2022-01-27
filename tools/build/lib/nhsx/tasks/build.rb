namespace :build do
  desc "Builds the docker image for the development environment"
  task :devenv do
    include Zuehlke::Execution
    include NHSx::Docker
    include NHSx::Login
    docker_out = File.join($configuration.out, "docker")

    rm_rf(docker_out, :secure=>true)
    mkdir_p(docker_out)
    docker_image_sourcefiles($configuration).each do |f|
      cp_r(f, docker_out)
    end

    begin
      content_tag = full_tag(content_version($configuration))
      login_to_aws_account("dev", "cta", false)
      pull_repository_image($configuration, content_tag)
      tag_content_version_as_latest($configuration, content_tag)
    rescue GaudiError
      # image doesn't exist, build it
      tags = [DEFAULT_VERSION, content_version($configuration)].map { |x| full_tag(x) }
      tag_cmds = tags.map { |label| "-t #{label}" }.join(" ")
      cmdline = "docker build --no-cache --progress=plain #{tag_cmds} '#{docker_out}'"
      run_tee("Build #{tags} container image", cmdline, $configuration)
    end
  end

  desc "Adds the external package dependencies to the lambdas"
  task :dependencies => [:"gen:version", :"build:java"]

  task :batch_creation => [:"gen:proto:java"] do
    java_project_path = File.join($configuration.base, "src/aws/lambdas/incremental_distribution")
    build_gradle_path = File.join(java_project_path, "build.gradle")
    java_output_path = File.join($configuration.out, "build")
    jar_file = File.join(java_output_path, "cta-0.0.1-SNAPSHOT.zip")
    java_src_pattern = "#{$configuration.base}/src/aws/lambdas/incremental_distribution/src/**/*"
    gradlew = File.join(java_project_path, "gradlew")

    file jar_file => Rake::FileList[java_src_pattern, build_gradle_path] do
      cmdline = "#{gradlew} --console plain -p #{java_project_path} :cta:clean cta:lambdaZip"
      run_tee("Build incremental distribution lambda", cmdline, $configuration)
    end
    Rake::Task[jar_file].invoke
  end

  task :java => [:"build:batch_creation"]

  namespace :pubdash do
    desc "Builds public dashboard backend"
    task :backend do
      # public dashboard lambda
      project_path = File.join($configuration.base, "src/aws/lambdas/incremental_distribution")
      build_gradle_path = File.join(project_path, "build.gradle")
      output_path = File.join($configuration.out, "build")
      jar_file = File.join(output_path, "pubdash-0.0.1-SNAPSHOT.zip")
      src_pattern = "#{$configuration.base}/src/aws/lambdas/incremental_distribution/src/**/*"
      gradlew = File.join(project_path, "gradlew")

      file jar_file => Rake::FileList[src_pattern, build_gradle_path] do
        cmdline = "#{gradlew} --console plain -p #{project_path} :pubdash:clean pubdash:lambdaZip"
        run_tee("Build public dashboard lambda", cmdline, $configuration)
      end
      Rake::Task[jar_file].invoke
    end

    namespace :frontend do
      NHSx::TargetEnvironment::PUBDASH_TARGET_ENVIRONMENTS.each do |account, _|
        desc "Builds public dashboard frontend (prod build removes the dev banner)"
        task :"#{account}" do
          project_dir = File.join($configuration.base, "src/pubdash/webapp/.")
          Dir.chdir(project_dir) do
            run_command("Install public dashboard webapp dependencies", "npm ci", $configuration)
            run_command("Run public dashboard webapp tests", "npm run test:nowatch", $configuration)

            if account == "prod" || account == "aa-prod"
              run_command("Build public dashboard webapp for prod", "npm run build:prod", $configuration)
            else
              run_command("Build public dashboard webapp for non-prod", "npm run build", $configuration)
            end
          end
        end
      end
    end
  end

  desc "Builds analytics backend"
  task :analytics do
    project_path = File.join($configuration.base, "src/aws/lambdas/incremental_distribution")
    build_gradle_path = File.join(project_path, "build.gradle")
    output_path = File.join($configuration.out, "build")
    jar_file = File.join(output_path, "analytics-0.0.1-SNAPSHOT.zip")
    src_pattern = "#{$configuration.base}/src/aws/lambdas/incremental_distribution/src/**/*"

    file jar_file => Rake::FileList[src_pattern, build_gradle_path] do
      gradlew = File.join(project_path, "gradlew")
      cmdline = "#{gradlew} --console plain -p #{project_path} :analytics:clean :analytics:lambdaZip"
      run_tee("Build analytics lambda zip", cmdline, $configuration)
    end
    Rake::Task[jar_file].invoke
  end
end
