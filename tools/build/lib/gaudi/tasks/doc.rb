require 'rdoc'

RDoc::Task.new(:rdoc=>"doc:gaudi", :clobber_rdoc => "clean:doc:gaudi",:rerdoc=>"doc:gaudi:force") do |rdoc|
  rdoc.title= "Build System"
  rdoc.rdoc_files.include("#{$configuration.base}/doc/BUILDSYSTEM.md", "#{$configuration.base}/Tools/build/lib/**/*.rb")
  rdoc.rdoc_dir=File.join($configuration.out,"doc/gaudi")
  rdoc.main="BUILDSYSTEM.md"
  rdoc.options+=["--page-dir=#{$configuration.base}/doc","-O"]
end

namespace :doc do 
  desc "Generates a dependency graph for all tasks in scope grouped by namespace under #{$configuration.out}/graphs"
  task :"graph:gaudi" do 
    include Gaudi::Documentation
    task_graph($configuration)
  end
end
