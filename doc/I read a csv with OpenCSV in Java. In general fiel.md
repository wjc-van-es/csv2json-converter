<img src="https://r2cdn.perplexity.ai/pplx-full-logo-primary-dark%402x.png" style="height:64px;margin-right:32px"/>

# I read a csv with OpenCSV in Java. In general fields are not quoted, but there is one double quoted to escape the separator character it contains.

Now this escaping is ignored resulting in the row having an extra field.
How must I configure the CSVParser (preferrably with calls to CSVParserBuilder methods) to prevent this?

In OpenCSV you must make sure that quotations are not ignored and that the quote character and separator match your file; then quoted fields will correctly “protect” the separator inside them.[^1_1][^1_2]

### Required CSVParserBuilder configuration

Configure your parser roughly like this (adapt separator and quote as needed):

- Use the correct separator (e.g. comma or semicolon).
- Use the correct quote character (double quote `"` in your case).
- Do not enable “ignore quotations”.

Example:

```java
CSVParser parser = new CSVParserBuilder()
        .withSeparator(';')          // or ','
        .withQuoteChar('"')          // fields like "a;b" stay one field
        .withIgnoreQuotations(false) // this is the default, but ensure it
        .build();
```

If you had previously set `withIgnoreQuotations(true)`, that will cause OpenCSV to treat the quotes as normal characters and split on the separator inside them, which is exactly the extra-field behavior you are seeing.[^1_3][^1_1][^1_2]
<span style="display:none">[^1_10][^1_4][^1_5][^1_6][^1_7][^1_8][^1_9]</span>

<div align="center">⁂</div>

[^1_1]: https://javadoc.io/static/com.opencsv/opencsv/3.5/com/opencsv/CSVParserBuilder.html

[^1_2]: https://javadoc.io/static/com.opencsv/opencsv/4.2/com/opencsv/CSVParser.html

[^1_3]: https://stackoverflow.com/questions/72188560/java-opencsv-csvparser-delimiter-inside-quoted-string

[^1_4]: https://b-greve.gitbook.io/beginners-guide-to-clean-data/common-csv-problems/quotation-characters

[^1_5]: https://stackoverflow.com/questions/41948442/parse-csv-with-opencsv-with-double-quotes-inside-a-quoted-field

[^1_6]: https://codingtechroom.com/question/prevent-opencsv-quotes-csv-file

[^1_7]: https://dev.to/sadiul_hakim/csv-quotes-explained-simply-with-java-example-5dom

[^1_8]: https://stackoverflow.com/questions/63510275/opencsv-reader-strips-ending-quotes-instead-of-ignoring-them

[^1_9]: https://www.baeldung.com/opencsv

[^1_10]: https://opencsv.sourceforge.net

