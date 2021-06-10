require "json"
require_relative "../../gaudi/helpers/configuration"
require_relative "../../zuehlke/helpers/git"
require_relative "../../zuehlke/helpers/execution"
require_relative "../../zuehlke/helpers/templates"
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
  # Provides methods to calculate the correct version number/scheme for CTA and subsystems
  module Versions
    include Zuehlke::Execution
    include Zuehlke::Templates
    include Zuehlke::Git
    # A Hash of the subsystems in the following format:
    #
    # subsystem => TagPrefix,Label
    #
    # TagPrefix is used to differentiate the versioning tags in the repository
    #
    # Label is used in composing the pointer tag names tracking the versions installed across target environments
    SUBSYSTEMS = {
      "backend" => { "prefix" => "Backend-", "label" => "" },
      "cta" => { "prefix" => "Backend-", "label" => "" },
      "tiers" => { "prefix" => "Tiers-", "label" => "tiers" },
      "analytics" => { "prefix" => "Analytics-", "label" => "analytics" },
      "availability" => { "prefix" => "Availability-", "label" => "availability" },
      "pubdash" => { "prefix" => "PublicDashboard-", "label" => "pubdash" },
      "pubdash-backend" => { "prefix" => "PublicDashboard-Backend-", "label" => "pubdash-backend" },
      "doreto" => { "prefix" => "Doreto-", "label" => "doreto" },
      "synthetics" => { "prefix" => "Synthetics-", "label" => "synthetics" },
      "local-messages" => { "prefix" => "LocalMessages-", "label" => "local-messages" },
    }.freeze

    # Returns the full GitVersion metadata for the given prefix
    def gitversion_metadata(subsystem_prefix, system_config)
      gitversion_options = "#{system_config.base} -config #{system_config.gitversion_config_file} -overrideconfig tag-prefix=#{subsystem_prefix}"
      cmdline = "gitversion #{gitversion_options}"
      cmd = run_command("Get #{subsystem_prefix.gsub("-", "")} version information", cmdline, system_config)
      return JSON.parse(cmd.output)
    end

    # Returns the version metadata for the given subsystem
    #
    # It assumes that the version tag is prefixed with TagPrefix as specified in Versions::SUBSYSTEMS
    def subsystem_version_metadata(subsystem, system_config)
      raise GaudiError, "Unrecognized subsystem #{subsystem} (cannot set version tag prefix)" unless SUBSYSTEMS.keys.include?(subsystem)

      gitversion_metadata = gitversion_metadata(SUBSYSTEMS[subsystem]["prefix"], system_config)
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
        "SubsystemPrefix" => SUBSYSTEMS[subsystem]["prefix"],
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
    rescue GaudiError
      return "N/A"
    end

    def pointer_tag_name(subsystem, tgt_env)
      raise GaudiError, "Unrecognized subsystem #{subsystem} (cannot set version tag prefix)" unless SUBSYSTEMS.keys.include?(subsystem)

      tag_name = "te-#{tgt_env}"
      tag_name << "-#{SUBSYSTEMS[subsystem]["label"]}" unless ["backend", "cta"].include?(subsystem)
      return tag_name
    end

    def label_tag_name(subsystem, version_number)
      raise GaudiError, "Unrecognized subsystem #{subsystem} (cannot set version tag prefix)" unless SUBSYSTEMS.keys.include?(subsystem)

      "#{SUBSYSTEMS[subsystem]["prefix"]}#{version_number}"
    end
  end
end
