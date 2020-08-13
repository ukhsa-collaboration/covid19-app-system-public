require 'date'
require 'zip'
require 'pathname'

module Zuehlke
  #Helper methods to create .zip packages
  #
  #This functionality depends on the rubyzip gem.
  #Note that rubyzip 1.1.x has a bug that leads to crashes with a NoMemory exception if the file is too large.
  #
  #use gem "rubyzip","~>1.2.0"
  module Package
    #Returns the full path for the package created for the given name
    def full_package_path pkg_name,system_config
      File.join(system_config.out,"pkg/#{pkg_name}.zip")
    end
    #Creates the package structure in system_config.out/pkg/package_name
    #
    #It will create a structure in the package relative to pkg_root which
    #is initialized to system_config.base if nil.
    #
    #Because it calculates relative paths, if the filelist contains file from a
    #completely separate directory tree (i.e. without common elements) it will then raise an exception.
    #Since we always operate within our repository, this should never happen, n'est-ce pas? ;)
    #
    #Returns the package file name
    def collect_package pkg_name,filelist,pkg_root,system_config
      pkg_root||=system_config.base
      pkg_root=File.expand_path(pkg_root)
      release_target=File.expand_path(File.join(system_config.out,"pkg/#{pkg_name}"))
      filelist.each do |filename|
        fn=Pathname.new(File.expand_path(filename))
        tgt=File.join(release_target,fn.relative_path_from(Pathname.new(pkg_root)))
        mkdir_p(File.dirname(tgt),:verbose=>false)
        cp(fn,tgt,:verbose=>false) unless File.directory?(filename)
      end
      return release_target
    end

    #Packages the release directory recursively in a .zip
    #
    #Returns the package file name
    def create_package release_dir,system_config
      archive_file = File.join(system_config.out,'pkg',"#{File.basename(release_dir)}.zip")
      pack(release_dir,archive_file)
    end

    def pack input_dir,package_file
      rm_rf(package_file,:verbose=>false)
      mkdir_p(File.dirname(package_file),:verbose=>false)
      files=FileList["#{input_dir}/**/*"]

      Zip::File.open(package_file, Zip::File::CREATE) do |zipfile|
        files.each do |filename|
          tgt=Pathname.new(filename).relative_path_from(Pathname.new(input_dir))
          zipfile.add(tgt, filename)
        end
      end
      return package_file
    end

    def unpack(package_file,to)
      tgt=File.expand_path(to) if to
      tgt||=package_file.pathmap('%X')
      
      mkdir_p(tgt,:verbose=>false)
      Zip::File.open(package_file) do |zip_file|
        # Handle entries one by one
        zip_file.each do |entry|
          dest_file=File.join(tgt,entry.name)
          mkdir_p(File.dirname(dest_file),:verbose=>false)
          entry.extract(dest_file)
        end
      end
      return tgt
    end
  end
end
