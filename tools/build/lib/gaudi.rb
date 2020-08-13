require "rake/dsl_definition"
require "rake/file_list"
require_relative "gaudi/helpers/utilities"
include Rake::DSL
include Gaudi::Utilities

# load every file you find in the helpers directory
mass_require(Rake::FileList["#{File.join(File.dirname(__FILE__), "gaudi/helpers")}/*.rb"].exclude("utilities.rb"))

module Gaudi::Utilities
  # Reads the configuration and sets the environment up
  #
  # This is the system's entry point
  def env_setup(work_dir)
    # Ensure that stdout and stderr output is properly ordered
    $stdout.sync = true
    $stderr.sync = true
    # Tell rake not to truncate it's task output
    Rake.application.terminal_columns = 999
    unless $configuration
      system_config = Gaudi.configuration()
      system_config.workspace = File.expand_path(work_dir)
      Rake.application.start_time = Time.now if Rake.application.respond_to?(:start_time=)
      $configuration = system_config
    end
    return $configuration
  end
end
