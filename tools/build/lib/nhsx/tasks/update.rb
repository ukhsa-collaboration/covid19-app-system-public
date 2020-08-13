namespace :update do
  desc "Updates the staging and prod account configurations with the outputs from dev"
  task :outputs do
    dev_outputs = File.join($configuration.base, "src/aws/accounts/dev/outputs.tf")

    ["prod", "staging"].each do |account|
      tgt = File.join($configuration.base, "src/aws/accounts/#{account}/outputs.tf")
      cp_r(dev_outputs, tgt, :verbose => false)
    end
  end
end
