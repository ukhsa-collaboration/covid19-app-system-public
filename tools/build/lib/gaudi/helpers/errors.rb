# General Gaudi error
#
# If at any point there is a different exception type in the code then it's going to be a bug
class GaudiError < RuntimeError
end

# Raised whenever an error is encountered while handling configuration files
class GaudiConfigurationError < GaudiError
end
