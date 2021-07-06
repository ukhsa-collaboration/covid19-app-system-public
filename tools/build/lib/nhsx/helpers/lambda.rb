require 'fileutils'
require 'json'
require_relative 'target'
require_relative 'venv'
require_relative '../../gaudi/helpers/utilities'

module NHSx
  # Methods to query and manipulate the target environment
  module Lambda
    include NHSx::Venv
    include Gaudi::Utilities

    PYTHON_ACTIVE_BIN = 'bin/activate'
    TMP_PATH = 'out/analytics/tmp'

    def package(out_path)
      python_venv_path = python_venv_path(out_path)
      setup_python_virtual_environment(python_venv_path)
      source_path = "src/analytics/lambdas"
      puts "copying to temp"
      p `chmod -R a+x #{source_path}`
      if Dir.exist?(TMP_PATH)
            if rm_r(TMP_PATH)
            end
      end

      cp_r(source_path, TMP_PATH)
      install_dependencies(TMP_PATH, source_path)
    end


    def pack()
          if Dir.exist?('out/python_package')
              if rm_r("out/python_package/")
              end
          end
        out_path = "out/analytics/lambdas"
        package(out_path)
    end
  end
end
