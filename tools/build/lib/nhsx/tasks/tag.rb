namespace :tag do
  include NHSx::Git

  namespace :release do
    desc "Tag the current SHA as a release of analytics with RELEASE_VERSION"
    task :analytics do
      include NHSx::Versions
      version_metadata = subsystem_version_metadata("analytics", $configuration)
      release_version = $configuration.release_version(version_metadata)
      tag("Analytics-#{release_version}", "CTA Analytics release #{release_version}", $configuration)
    end
    desc "Tag the current SHA as a release of the ContactTracingApp (app-system) with RELEASE_VERSION"
    task :cta do
      include NHSx::Versions
      version_metadata = subsystem_version_metadata("backend", $configuration)
      release_version = $configuration.release_version(version_metadata)
      tag("Backend-#{release_version}", "AppSystem (cta) backend release #{release_version}", $configuration)
    end
    desc "Tag the current SHA as a release of tier_metadata with RELEASE_VERSION"
    task :tier_metadata do
      include NHSx::Versions
      version_metadata = subsystem_version_metadata("tiers", $configuration)
      release_version = $configuration.release_version(version_metadata)
      tag("Tiers-#{release_version}", "CTA Tier Metadata release #{release_version}", $configuration)
    end
    desc "Tag the current SHA as a release of availability configuration with RELEASE_VERSION"
    task :availability do
      include NHSx::Versions
      version_metadata = subsystem_version_metadata("availability", $configuration)
      release_version = $configuration.release_version(version_metadata)
      tag("Availability-#{release_version}", "CTA Availability Configuration release #{release_version}", $configuration)
    end
    desc "Tag the current SHA as a release of public dashboard with RELEASE_VERSION"
    task :pubdash do
      include NHSx::Versions
      version_metadata = subsystem_version_metadata("pubdash", $configuration)
      release_version = $configuration.release_version(version_metadata)
      tag("PublicDashboard-#{release_version}", "CTA Public Dashboard release #{release_version}", $configuration)
    end
  end
end
