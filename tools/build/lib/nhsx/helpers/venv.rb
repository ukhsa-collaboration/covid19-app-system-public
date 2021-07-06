require 'json'
require_relative 'errors'
require_relative '../../gaudi/helpers/utilities'

module NHSx
  # Methods to query and manipulate the target environment
  module Venv
    include NHSx::TargetEnvironment
    include Gaudi::Utilities

    PYTHON_ACTIVE_BIN = 'bin/activate'

    RELATIVE_PATH_TO_TESTS="./test/".freeze

    def python_venv_path(out_path)
      "#{out_path}/python"
    end

    def setup_python_virtual_environment(venv_path)
      # Create virtual environment
      cmd = "python3 -m venv #{venv_path}"
      puts cmd
      puts `#{cmd}`

      # Activate virtual environment
      cmd = "source #{File.join(venv_path, PYTHON_ACTIVE_BIN)}"
      puts cmd
      cmd = "bash -c '#{cmd} && echo $VIRTUAL_ENV && echo $PATH'"
      std_out = `#{cmd}`
      outputs = std_out.split("\n")
      raise GaudiError, "VIRTUAL_ENV undefined!" unless outputs[0]
      ENV['VIRTUAL_ENV'] = outputs[0]
      raise GaudiError, "PATH undefined!" unless outputs[1]
      ENV['PATH'] = outputs[1]

      # Upgrade to latest version of pip to avoid warnings
      # - don't pin to a specific version; the pip version won't affect versions of dependencies
      cmd = 'pip install --upgrade pip'
      puts cmd
      puts `#{cmd}`

      # Install dev dependencies into virtual env
      cmd = 'pip install -r src/analytics/lambdas/requirements.txt'
      puts cmd
      puts `#{cmd}`
    end

    def install_dependencies(tmp_path, src_path)
      cmd = "pip install --target '#{tmp_path}' -r '#{src_path}/requirements.txt' && " +
            "pip freeze --path '#{tmp_path}' > '#{tmp_path}/updated-requirements.txt'"
      puts cmd
      puts `#{cmd}`
    end

    def flake8(system_config)
      cmd = "flake8 --count --config tools/build/config/flake8.config"
      run_tee("Lint python files", cmd, system_config)
    end

    def run_pytest(system_config, src_path, package_name, log_dir)
      package_path = File.join(src_path, package_name)
      Dir.chdir(package_path) do
        cmd = "python -m pytest -n auto #{RELATIVE_PATH_TO_TESTS} -vv"
        begin
          run_tee("Running #{package_name} tests", cmd, system_config, log_dir)
        rescue UnitTestsError
          puts "#{package_name} tests failed."
          return false
        end
      end
      return true
    end

  end
end
