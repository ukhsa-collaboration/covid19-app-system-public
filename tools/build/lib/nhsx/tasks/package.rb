namespace :package do
  namespace :python do
    include NHSx::Lambda
    desc "Packages lambda for analytics"
        task :analytics do
              lambdas_count = pack()
        end
end
end
