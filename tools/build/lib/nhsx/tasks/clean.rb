require_relative "clean/account"
require_relative "clean/analytics"
require_relative "clean/cta"
require_relative "clean/doreto"
require_relative "clean/pubdash"
require_relative "clean/synthetics"
require_relative "clean/virology"

namespace :clean do
  task :orphans => [:"clean:cta:orphans", :"clean:analytics:orphans", :"clean:pubdash:orphans"]
end
