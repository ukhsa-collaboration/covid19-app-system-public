module NHSx
    module Query

        def get_athena_named_query(query_id)
            cmd = run_command("Retrieve named query with ID: #{query_id}", NHSx::AWS::Commandlines.get_athena_named_query(query_id), $configuration)
            query = JSON.parse(cmd.output)["NamedQuery"]["QueryString"]
            return query.split("\n").join(" ").gsub!("\"", "\\\"")
        end

        def start_athena_query(query_id, query, database, workgroup)
            cmd = run_command("Start execution for query with ID: #{query_id}", NHSx::AWS::Commandlines.start_athena_query(query, database, workgroup), $configuration)
        end
    end
end