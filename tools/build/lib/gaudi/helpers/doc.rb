require "graph"

module Gaudi
  module Documentation
    # Creates a .png with the dependency graph of all tasks detected by rake
    # in system_config.out/graphs
    #
    # Note that tasks do not include file tasks (tasks associated with a file path)
    # since these tend to be generated and flood the graph
    def task_graph(system_config)
      digraph do
        rotate
        colorscheme(:set1, 9)
        node_attribs << filled
        task_graph_details()
        mkdir_p(File.join(system_config.out, "graphs"), :verbose => false)
        save File.join(system_config.out, "graphs", "gaudi"), "png"
      end
    end

    #:stopdoc:
    # Iterate over all tasks building nodes, edges and an index of namespace->task name (where namespace only goes one level deep)
    # Use the index to cluster and color the nodes
    def task_graph_details
      grouped_by_namespace = {}
      no_filetasks = Rake::Task.tasks.select { |rt| rt.class == Rake::Task }
      no_filetasks.each do |rt|
        name_parts = rt.name.split(":").reverse
        name_space = ""
        name_space = name_parts.pop if name_parts.size > 1

        grouped_by_namespace[name_space] ||= []
        grouped_by_namespace[name_space] << rt.name
        rt.prerequisites.each { |prereq| edge rt.name, prereq }
      end
      grouped_by_namespace.each do |k, v|
        cluster k do
          v.each { |ar| colored_node(ar, k) }
        end
      end
    end

    # assign colors to the graph nodes according to the namespace
    def colored_node(node_name, node_namespace)
      case node_namespace
      when "gen"
        c5 << node(node_name)
      when "build"
        c1 << node(node_name)
      when "lint"
        c3 << node(node_name)
      when "unit"
        c6 << node(node_name)
      when "test"
        c2 << node(node_name)
      when "pkg"
        c9 << node(node_name)
      when "doc"
        c7 << node(node_name)
      when "deploy"
        c8 << node(node_name)
      else
        node(node_name)
      end
    end

    #:startdoc:
  end
end
