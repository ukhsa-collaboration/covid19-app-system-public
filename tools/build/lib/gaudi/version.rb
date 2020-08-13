# Gaudi is a collection of helpers and tools that together with a small set of conventions and rake
# allows you to create complex build systems.
module Gaudi
  # Gaudi follows SemVer even though it's not a gem
  module Version
    # Major version
    MAJOR = 1
    # Minor version
    MINOR = 1
    # Tiny version
    TINY = 1
    # All-in-one
    STRING = [MAJOR, MINOR, TINY].join(".")
  end
end
