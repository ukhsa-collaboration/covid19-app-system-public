#:stopdoc:
require "rake"

module Rake
  class Application
    attr_accessor :start_time
  end

  class Task
    alias_method :_original_invoke, :invoke

    def invoke(*args)
      enhance() {
        puts "Total execution time #{Time.now - Rake.application.start_time}" if Rake.application.start_time
      }
      _original_invoke(*args)
    end
  end
end

#:startdoc:
