require 'set'
require_relative '../../gaudi/helpers/errors'

module NHSx
  module Terraform
    class TerraformStateTracker

      def initialize(resource_paths)
        @resources = resource_paths.lines.map!{|l| l.chomp}.to_set
      end

      def import(new_path)
        raise GaudiError, "#{new_path} already exists" if @resources.add?(new_path).nil?
      end

      def remove(resource_path)
        all_resources = expand_paths(resource_path)
        raise GaudiError, "#{resource_path} doesn't exist" unless all_resources.any?
        all_resources.each do |resource|
          @resources.delete resource
        end
      end

      def move(resource_path, new_path)
        all_resources = expand_paths(resource_path)
        raise GaudiError, "#{resource_path} doesn't exist" unless all_resources.any?
        new_resources = expand_paths(new_path)
        raise GaudiError, "#{new_path} already exists" if new_resources.any?
        all_resources.each do |resource|
          @resources.delete resource
          import resource.sub resource_path, new_path
        end
      end

      def expand_paths(resource_path)
        @resources.select { |path| path =~ /\b#{resource_path}\b/ }
      end

      def resource_exists(resource_path)
        expand_paths(resource_path).any?
      end

    end
  end
end
