#!/usr/bin/ruby

table_name = nil
table_section_indent = 0
f = nil
columns = 0

while line = gets
    if line =~ /resource "aws_glue_catalog_table" "(.+)" {/
        table_section_indent = 1
        table_name = $1

        f = File.open("#{table_name}-cols.txt", "w")
    else
        if table_section_indent > 0
            if line.include? "}"
                table_section_indent -= 1
                columns = false
            end

            if line.include? "columns"
                columns = true
            end

            if columns and table_section_indent == 3 and f != nil
                f.puts line.strip
            end

            if line.include? "{"
                table_section_indent += 1
            end
        end
    end

    if table_section_indent == 0
        if f != nil
            f.close
            f = nil
        end
    end
end
