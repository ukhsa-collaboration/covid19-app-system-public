module NHSx
  module Pubdash

    # Publishes a single file from local_dir onto s3_location.
    # The local_dir path must contain only 1 file
    def self.publish_single(local_dir, s3_object_location)
      files = FileList["#{local_dir}/**/*"]
      raise GaudiError, "Local dir does not contain only 1 file: [#{files}]" unless files.size == 1

      upload_file = files.first

      NHSx::AWS::upload_single_file_to_s3(upload_file, s3_object_location, $configuration)
    end

    # Publishes all files from local_dir onto s3_location.
    def self.publish_recursively(local_dir, s3_location)
      raise GaudiError, "Local dir is empty!" unless FileList["#{local_dir}/**/*"].size > 0

      NHSx::AWS::upload_recursively_to_s3(local_dir, s3_location, $configuration)
    end

    # Downloads all files from s3_location onto local_dir.
    # The local_dir must be empty before copying any files from s3.
    def self.download_recursively(local_dir, s3_location)
      raise GaudiError, "Local dir is not empty!" unless FileList["#{local_dir}/**/*"].size == 0

      NHSx::AWS::download_recursively_from_s3(s3_location, local_dir, $configuration)
    end

  end
end
