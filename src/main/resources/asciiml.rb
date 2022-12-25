class AsciiMlConverter
  include Asciidoctor::Converter
  register_for 'asciiml'

  def initialize *args
    super
    outfilesuffix '.xml'
  end

  def convert node, transform = node.node_name, opts = nil



    extract_text = transform.start_with? 'inline_'
    extract_text = true if transform == "table_cell" and node.style != :asciidoc
    extract_text = true if transform == "list_item"

    process_content = false
    process_content = true if not transform.start_with? 'inline_' and not ['table_cell', 'list_item'].include?(transform)
    process_content = true if transform == "table_cell" and node.style == :asciidoc
    process_content = true if transform == "list_item" and node.blocks?


    has_location = true
    has_location = false if transform.start_with? 'inline_'
    sourcemap_attributes = has_location ? "file ='#{node.source_location.file}' lineno='#{node.source_location.lineno}'" : ""

    title =  (defined? node.title and not node.title.nil?) ? "<title  #{sourcemap_attributes}>" + node.title + "</title>" : ""
    inline_quoted_type = (defined? node.type and not node.type.nil?) ? "type = '#{node.type}'" : ""

    element_type = transform.start_with?('inline_') ? "inline = 'true'" : "block = 'true'"

    case transform
    when 'table'
      <<~EOS.chomp
        <#{transform} #{sourcemap_attributes}>
          #{title}
          #{iterate_table_cells(node)}
        </#{transform}>
      EOS
    when 'ulist', 'olist'
      <<~EOS.chomp
        <#{transform} #{sourcemap_attributes}>
          #{title}
          #{iterate_list_items(node)}
        </#{transform}>
      EOS
    else
      <<~EOS.chomp
        <#{transform} #{sourcemap_attributes}
            #{inline_quoted_type} 
            #{element_type}
          >
          #{title}
          #{extract_text ? node.text : ""}
          #{process_content ? node.content : ""}
        </#{transform}>
      EOS
    end
  end
  def iterate_list_items node
    items_r = ''
    node.items.each do |list_item|
      items_r = "#{items_r} #{convert list_item}"
    end
    items_r
  end
  def iterate_table_cells node
    tsecs_r = ''
    node.rows.to_h.each do |tsec, rows|
      rows_r = ''
      rows.each do |row|
        cells_r = ''
        row.each do |table_cell|
          cells_r = "#{cells_r}#{convert table_cell}"
        end
        rows_r = "#{rows_r}<row>#{cells_r}</row>"
      end
      tsecs_r = "#{tsecs_r}<#{tsec}>#{rows_r}</#{tsec}>"
    end
    tsecs_r
  end
end
