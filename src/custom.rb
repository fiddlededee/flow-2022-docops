class MyPreprocessor < StyleSubstitutor

  # def h_table_docx
  #   tables =
  #     @pre.xpath("//table:table[not(contains(@table:style-name, ' no_margin_bottom '))]",
  #                'table' => 'urn:oasis:names:tc:opendocument:xmlns:table:1.0')
  #   tables.each do |table|
  #     # Отступ после таблицы
  #     table.after('<text:p text:style-name="Docx_20_Paragraph_20_after_20_Table"/>')
  #   end
  # end

  def h_paragraph_before_image
    image_paragraphs =
      @pre.xpath("//text:p[starts-with(@text:style-name, 'adoc_bip ')]",
                 'text' => 'urn:oasis:names:tc:opendocument:xmlns:text:1.0')
    image_paragraphs.each do |image_paragraph|
      # Вставка нулевого параграфа перед картинкой, иначе ворд 10 не показывает верхнюю границу рисунка (вероятен баг ворда)
      image_paragraph.before('<text:p text:style-name="Docx_20_Paragraph_20_before_20_Image"/>')
    end
  end

  def h_before_header
    header_paragraphs =
      @pre.xpath("//text:h",
                 'text' => 'urn:oasis:names:tc:opendocument:xmlns:text:1.0')
    header_paragraphs.each do |header_paragraph|
      # Вставка нулевого параграфа перед заголовком. Если заголовок с новой страницы, то в него вставится разрыв
      # Если не с новой страницы, то этот абзац заставит не объединять отступы до заголовка и после
      # предыдущего абзаца (именно так работает LibreOffice)
      header_paragraph.before('<text:p text:style-name="Docx_20_Paragraph_20_before_20_Header"/>')
    end
  end


end
