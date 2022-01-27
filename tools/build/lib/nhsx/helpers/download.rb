require "patir/command"

module NHSx
  module Download
    def codebuild_logs(system_config)
      # get build info
      job_id = system_config.job_id
      job_info = build_info(job_id)
      # download zip from s3 bucket
      downloads_out_dir = File.join(system_config.out, "downloads/")
      object_name = job_info.artifacts
      object_name = object_name.sub("arn:aws:s3:::", "")
      zip_file_path = File.join(downloads_out_dir, "#{object_name}.zip")
      run_command("Download the build artifacts of #{job_id}", NHSx::AWS::Commandlines.download_from_s3(object_name, zip_file_path), system_config)
      # unzip to base dir
      run_command("Unzip archive", "unzip #{zip_file_path} -d #{system_config.base}", system_config)
      # remove downloaded s3 zip file
      File.delete(zip_file_path)
      stream_log_segments(job_info)
    end
  end
end
