require 'erubis'

module Zuehlke
  #Templates simplify many build system tasks.
  #
  #Some examples include generation of IDE project files, 
  #code generation for things like a unit test harness or generated version information to be added to assemblies etc.
  #
  #The recommended way to integrate templates in Gaudi and Zuehlke projects is to place all templates under tools/templates
  #
  #The methods provided in this module support templates in the ERB format and use the erubis gem 
  #which offers significant performance improvements over the standard ERB library.
  module Templates
    #populates an ERB template from the params Hash
    def from_template template_file,params
      template_content=File.read(template_file)
      template=Erubis::Eruby.new(template_content)
      return template.result(params).gsub("\n","\r\n")
    end
  end
end


