namespace :clean do
  desc "Removes the output directory"
  task :wipe do |t|
    if $configuration
      rm_rf(FileList[$configuration.out],:verbose=>false)
    end
    puts "#{t.name} done!"
  end
end