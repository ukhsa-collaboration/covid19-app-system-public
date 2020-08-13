require "json"
# Configuration module for GitVersion
module Gaudi::Configuration::SystemModules::GitVersion
  #:stopdoc:
  def self.list_keys
    []
  end

  def self.path_keys
    ["gitversion_config_file"]
  end
  #:startdoc:

  # The URL to the NuGet feed used for publishing packages
  def gitversion_config_file
    required_path(@config["gitversion_config_file"])
  end
end

module NHSx
  # Provides methods to calculate the correct version number.
  #
  # Details on the versioning scheme are in VERSIONING.md
  module Versions
    include Zuehlke::Execution
    include Zuehlke::Templates
    include Zuehlke::Git
    # A Hash of the subsystems in the following format:
    #
    # subsystem => TagPrefix
    #
    # TagPrefix is used to differentiate the versioning tags in the repository
    SUBSYSTEMS = {
      "backend" => "backend-v",
    }.freeze

    # Returns the full GitVersion metadata for the given subsystem
    #
    # It assumes that the version tag
    # is prefixed with TagPrefix as specified in Versions::SUBSYSTEMS
    def subsystem_version_metadata(subsystem, system_config)
      subsystem_prefix = SUBSYSTEMS.fetch(subsystem, "")
      raise GaudiError, "Unrecognized subsystem #{subsystem} (cannot set version tag prefix)" if subsystem_prefix.empty?

      gitversion_options = "#{system_config.base} -config #{system_config.gitversion_config_file} -overrideconfig tag-prefix=#{subsystem_prefix}"
      cmdline = "gitversion #{gitversion_options}"
      cmd = run_command("Get #{subsystem} version information", cmdline, system_config)
      gitversion_metadata = JSON.parse(cmd.output)
      version_metadata = {
        "Major" => gitversion_metadata["Major"],
        "Minor" => gitversion_metadata["Minor"],
        "Patch" => gitversion_metadata["Patch"],
        "PreReleaseTag" => gitversion_metadata["PreReleaseTag"],
        "SemVer" => gitversion_metadata["SemVer"],
        "ShortSha" => gitversion_metadata["ShortSha"],
        "CommitsSinceVersionSource" => gitversion_metadata["CommitsSinceVersionSource"],
        "FullSemVer" => gitversion_metadata["FullSemVer"],
        "InformationalVersion" => gitversion_metadata["InformationalVersion"],
        "MajorMinorPatch" => gitversion_metadata["MajorMinorPatch"],
        "BranchName" => gitversion_metadata["BranchName"],
      }
      write_file(File.join(system_config.out, "#{subsystem}.version"), JSON.dump(version_metadata))
      return version_metadata
    end

    # Reads the version metadata embedded in the store buckets and returns it
    #
    # Returns N/A if the S3 object is not present
    def target_environment_version(env, env_config, system_config)
      object_name = "#{env_config["exposure_configuration_distribution_store"]}/version"
      local_target = File.join(system_config.out, "report/#{env}_version")
      run_command("Determine version of #{env} deployment", NHSx::AWS::Commandlines.download_from_s3(object_name, local_target), system_config)
      return File.read(local_target)
    rescue
      return "N/A"
    end
  end
end
