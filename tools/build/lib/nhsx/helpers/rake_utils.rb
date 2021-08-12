module NHSx
  # Methods to help with rake peculiarities
  module RakeUtils
    # Allow boolean values such as "dry-run" to be set true where their default is "false" and vice versa
    def handle_cmdline_switches(args, switch_defaults)
      switches = Marshal.load(Marshal.dump(switch_defaults))
      args.shift
      # By default, rake considers each 'argument' to be the name of an actual task.
      # It will try to invoke each one as a task.  By dynamically defining a dummy
      # task for every argument, we can prevent an exception from being thrown
      # when rake inevitably doesn't find a defined task with that name.
      args.each do |arg|
        symbolic_arg = arg.to_sym
        task symbolic_arg do ; end
        if switches[symbolic_arg].nil?
          puts "Argument not recognised: '#{arg}'" unless arg.include? "="
        else
          switches[symbolic_arg] = !switches[symbolic_arg]  # override the default value
        end
      end
      switches
    end

    def get_targeted_resources(system_config, tgt_env, account, resourcefilename)
      # Assume that resourcefilename is in the standard output folder
      r_file = resourcefilename.to_s.strip
      if r_file.empty?
        resources = []
      else
        resourcesfilepath = File.join(system_config.out, tgt_env, account, r_file)
        begin
          resources = IO.readlines resourcesfilepath, chomp: true
        rescue Errno::ENOENT
          # Maybe the resourcefilename is a relative or absolute path?
          begin
            resources = IO.readlines r_file, chomp: true
          rescue Errno::ENOENT
            # Maybe the resourcefilename is in the logs folder?
            resourcesfilepath = File.join(system_config.out, 'logs', r_file)
            begin
              resources = IO.readlines resourcesfilepath, chomp: true
            rescue Errno::ENOENT
              # Give up!
              raise GaudiError, "No such file or path in searched directories: #{r_file}"
            end
          end
        end
      end

      resources
    end

  end
end
