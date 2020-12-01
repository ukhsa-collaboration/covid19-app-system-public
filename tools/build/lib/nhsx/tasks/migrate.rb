# load every file you find in the migrations directory
mass_require(Rake::FileList["#{File.join(File.dirname(__FILE__), "migrations")}/*.rb"])
