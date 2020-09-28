require_relative "../version"
require_relative "errors"
require_relative "environment"
require "pathname"
require "yaml"

module Gaudi
  # The path to the default configuration file relative to the main rakefile
  DEFAULT_CONFIGURATION_FILE = File.expand_path(File.join(File.dirname(__FILE__), "../../../system.cfg"))
  # Loads and returns the system configuration
  def self.configuration
    if ENV["GAUDI_CONFIG"]
      ENV["GAUDI_CONFIG"] = File.expand_path(ENV["GAUDI_CONFIG"])
    else
      if File.exist?(DEFAULT_CONFIGURATION_FILE)
        ENV["GAUDI_CONFIG"] = File.expand_path(DEFAULT_CONFIGURATION_FILE)
      else
        raise "No configuration file (GAUDI_CONFIG is initially empty and #{DEFAULT_CONFIGURATION_FILE} is missing)"
      end
    end
    # Load the system configuration
    puts "Reading main configuration from \n\t#{ENV["GAUDI_CONFIG"]}"
    system_config = Configuration::SystemConfiguration.new(ENV["GAUDI_CONFIG"])
    return system_config
  end

  module Configuration
    module Helpers
      # Raises GaudiConfigurationError
      def required_path(fname)
        if fname && !fname.empty?
          if File.exist?(fname)
            return File.expand_path(fname)
          else
            raise GaudiConfigurationError, "Missing required file #{fname}"
          end
        else
          raise GaudiConfigurationError, "Empty value for required path"
        end
      end
    end

    # Switches the configuration for the given block
    #
    # It switches the system configuration by reading a completely different file.
    #
    # This makes for some interesting usage when you don't want to have multiple calls
    # with a different GAUDI_CONFIG parameter
    def self.switch_configuration(configuration_file)
      if block_given?
        current_configuration = ENV["GAUDI_CONFIG"]
        if File.expand_path(configuration_file) != File.expand_path(current_configuration)
          begin
            puts "Switching configuration to #{configuration_file}"
            $configuration = nil
            ENV["GAUDI_CONFIG"] = configuration_file
            $configuration = Gaudi::Configuration::SystemConfiguration.load([ENV["GAUDI_CONFIG"]])
            yield
          ensure
            puts "Switching configuration back to #{current_configuration}"
            $configuration = nil
            ENV["GAUDI_CONFIG"] = current_configuration
            $configuration = Gaudi::Configuration::SystemConfiguration.load([ENV["GAUDI_CONFIG"]])
          end
        end
      end
    end
    # Class to load the configuration from a key=value textfile with a few additions.
    #
    # Configuration classes derived from Loader override the Loader#keys method to specify characteristics for the keys.
    #
    # Paths are interpreted relative to the configuration file.
    #
    # In addition to the classic property format (key=value) Loader allows you to split the configuration in multiple files
    # and use import to put them together:
    # import additional.cfg
    #
    # Also use setenv to set the value of an environment variable
    #
    # setenv GAUDI=brilliant builder
    class Loader
      include Helpers
      # Goes through a list of configuration files and returns the resulting merged configuration
      def self.load(configuration_files, klass)
        cfg = nil
        configuration_files.each do |cfg_file|
          if cfg
            cfg.merge(cfg_file)
          else
            cfg = klass.new(cfg_file)
          end
        end
        if cfg
          return cfg
        else
          raise GaudiConfigurationError, "No #{klass.to_s} configuration files in '#{configuration_files}'"
        end
      end

      # Returns all the configuration files processed (e.g. those read with import and the platform configuration files)
      # The main file (the one used in initialize) is always first in the collection
      attr_reader :configuration_files
      attr_reader :config

      def initialize(filename)
        @configuration_files = [File.expand_path(filename)]
        @config = read_configuration(File.expand_path(filename))
      end

      # Returns an Array containing two arrays.
      #
      # Override in Loader derived classes.
      #
      # The first one lists the names of all keys that correspond to list values (comma separated values, e.g. key=value1,value2,value3)
      # Loader will assign an Array of the values to the key, e.g. config[key]=[value1,value2,value3]
      #
      # The second Array is a list of keys that correspond to pathnames.
      #
      # Loader will then expand_path the value so that the key contains the absolute path.
      def keys
        return [], []
      end

      def to_s
        "Gaudi #{Gaudi::Version::STRING} with #{configuration_files.first}"
      end

      # Merges the parameters from cfg_file into this instance
      def merge(cfg_file)
        begin
          cfg = read_configuration(cfg_file)
          list_keys, path_keys = *keys
          cfg.keys.each do |k|
            if @config.keys.include?(k) && list_keys.include?(k)
              @config[k] += cfg[k]
            else
              # last one wins
              @config[k] = cfg[k]
            end
          end
          @configuration_files << cfg_file
        rescue
        end
      end

      # Reads a configuration file and returns a hash with the
      # configuration as key-value pairs
      def read_configuration(filename)
        if File.exist?(filename)
          lines = File.readlines(filename)
          cfg_dir = File.dirname(filename)
          begin
            cfg = parse_content(lines, cfg_dir, *keys)
          rescue GaudiConfigurationError
            raise GaudiConfigurationError, "In #{filename} - #{$!.message}"
          end
        else
          raise GaudiConfigurationError, "Cannot load configuration.'#{filename}' not found"
        end
        return cfg
      end

      private

      #:stopdoc:
      def parse_content(lines, cfg_dir, list_keys, path_keys)
        cfg = {}
        lines.each do |l|
          l.gsub!("\t", "")
          l.chomp!
          # ignore if it starts with a hash
          unless l =~ /^#/ || l.empty?
            if /^setenv\s+(?<envvar>.+?)\s*=\s*(?<val>.*)/ =~ l
              environment_variable(envvar, val)
              # if it starts with an import get a new config file
            elsif /^import\s+(?<path>.*)/ =~ l
              cfg.merge!(import_config(path, cfg_dir))
            elsif /^(?<key>.*?)\s*\+=\s*(?<v>.*)/ =~ l
              handle_key_append(key, v, cfg_dir, list_keys, path_keys, cfg)
            elsif /^(?<key>.*?)\s*=\s*(?<v>.*)/ =~ l
              cfg[key] = handle_key(key, v, cfg_dir, list_keys, path_keys, cfg)
            else
              raise GaudiConfigurationError, "Syntax error: '#{l}'"
            end
          end # unless
        end # lines.each
        return cfg
      end

      # Appends data to a key that is defined as list key
      def handle_key_append(key, value, cfg_dir, list_keys, path_keys, cfg)
        thisValue = handle_key(key, value, cfg_dir, list_keys, path_keys, cfg)
        cfg[key] = [] unless cfg.has_key?(key)

        begin
          cfg[key].concat(thisValue).uniq!
        rescue
        end
      end

      # checks a key against the set of path or list keys and returns the value
      # in the format we want to have.
      #
      # This means keys in list_keys come back as an Array, keys in path_keys come back as full paths
      def handle_key(key, value, cfg_dir, list_keys, path_keys, cfg)
        final_value = value
        # replace %{symbol} with values from already existing config values
        final_value = final_value % Hash[cfg.map { |k, v| [k.to_sym, v] }] if final_value.include? "%{"

        if list_keys.include?(key) && path_keys.include?(key)
          final_value = Rake::FileList[*(value.gsub(/\s*,\s*/, ",").split(",").uniq.map { |d| absolute_path(d.strip, cfg_dir) })]
        elsif list_keys.include?(key)
          # here we want to handle a comma separated list of entries
          final_value = value.gsub(/\s*,\s*/, ",").split(",").uniq
        elsif path_keys.include?(key)
          final_value = absolute_path(value.strip, cfg_dir)
        end
        return final_value
      end

      def import_config(path, cfg_dir)
        path = absolute_path(path.strip, cfg_dir)
        raise GaudiConfigurationError, "Cannot find #{path} to import" unless File.exist?(path)

        @configuration_files << path
        read_configuration(path)
      end

      def absolute_path(path, cfg_dir)
        if Pathname.new(path.gsub("\"", "")).absolute?
          path
        else
          File.expand_path(File.join(cfg_dir, path.gsub("\"", "")))
        end
      end

      def environment_variable(envvar, value)
        ENV[envvar] = value
      end

      def load_key_modules(module_const)
        list_keys = []
        path_keys = []
        configuration_modules = module_const.constants
        # include all the modules you find in the namespace
        configuration_modules.each do |mod|
          klass = module_const.const_get(mod)
          extend klass
          list_keys += klass.list_keys
          path_keys += klass.path_keys
        end
        return list_keys, path_keys
      end

      #:startdoc:
    end

    # The central configuration for the system
    #
    # The available functionality is extended through SystemModules modules
    class SystemConfiguration < Loader
      include EnvironmentOptions
      def self.load(configuration_files)
        super(configuration_files, self)
      end

      attr_accessor :config_base, :workspace, :timestamp

      def initialize(filename)
        load_gaudi_modules(File.expand_path(filename))
        super(filename)
        @config_base = File.dirname(configuration_files.first)
        raise GaudiConfigurationError, "Setting 'base' must be defined" unless base
        raise GaudiConfigurationError, "Setting 'out' must be defined" unless out

        @workspace = Dir.pwd
        @timestamp = Time.now
      end

      def keys
        load_key_modules(Gaudi::Configuration::SystemModules)
      end

      private

      #:stopdoc:
      # makes sure we require the helpers from any modules defined in the configuration
      # before we start reading the configuration to ensure that extension modules work correctly
      def load_gaudi_modules(main_config_file)
        lines = File.readlines(main_config_file)
        relevant_lines = lines.select do |ln|
          /base=/ =~ ln || /gaudi_modules=/ =~ ln
        end
        cfg = parse_content(relevant_lines, File.dirname(main_config_file), *keys)
        require_modules(cfg.fetch("gaudi_modules", []), cfg["base"])
      end

      # Iterates over system_config.gaudi_modules and requires all helper files
      def require_modules(module_list, base_directory)
        module_list.each do |gm|
          mass_require(Rake::FileList["#{base_directory}/tools/build/lib/#{gm}/helpers/*.rb"])
          mass_require(Rake::FileList["#{base_directory}/tools/build/lib/#{gm}/rules/*.rb"])
        end
      end

      #:startdoc:
    end

    # Adding modules in this module allows SystemConfiguration to extend it's functionality
    #
    # Modules must implement two methods:
    # list_keys returning an Array with the keys that are comma separated lists
    # and path_keys returning an Array with the keys whose value is a file path
    #
    # Modules are guaranteed a @config Hash providing access to the configuration file contents
    module SystemModules
      # The absolute basics for configuration
      module BaseConfiguration
        #:stopdoc:
        def self.list_keys
          ["gaudi_modules"]
        end

        def self.path_keys
          ["base", "out"]
        end

        # :startdoc:
        # The root path.
        # Every path in the system can be defined relative to this
        def base
          return @config["base"]
        end

        # The output directory
        def out
          return @config["out"]
        end

        # A list of module names (directories) to automatically require next to core when loading Gaudi
        def gaudi_modules
          @config["gaudi_modules"] ||= []
          return @config["gaudi_modules"]
        end

        alias_method :base_dir, :base
        alias_method :out_dir, :out
      end
    end
  end
end
