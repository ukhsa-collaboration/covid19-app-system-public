namespace :unit do
  task :all => [:"unit:static_post_districts"]

  desc "Run unit test for static postal districts"
  task :static_post_districts do
    static_json_path = File.join($configuration.base, "src/static/risky-post-districts.json")
    static_json = File.read(static_json_path)
    post_districts_json = JSON.parse(static_json)

    post_districts_json["postDistricts"].each { |e|
      key = e[0]
      value = e[1]

      raise "Invalid postal district for entry [#{key},#{value}]" unless !key.empty? and !key.strip.empty?
      raise "Invalid risk level for entry: [#{key},#{value}]" unless value == "L" or value == "M" or value == "H"
      raise "Duplicate entry found: [#{key},#{value}]" unless static_json.scan("\"#{key}\"").count == 1
    }
  end
end
