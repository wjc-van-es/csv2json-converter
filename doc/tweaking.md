<style>
body {
  font-family: "Spectral", "Gentium Basic", Cardo , "Linux Libertine o", "Palatino Linotype", Cambria, serif;
  font-size: 100% !important;
  padding-right: 12%;
}
code {
  padding: 0.25em;
	
  white-space: pre;
  font-family: "Tlwg mono", Consolas, "Liberation Mono", Menlo, Courier, monospace;
	
  background-color: #ECFFFA;
  //border: 1px solid #ccc;
  //border-radius: 3px;
}

kbd {
  display: inline-block;
  padding: 3px 5px;
  font-family: "Tlwg mono", Consolas, "Liberation Mono", Menlo, Courier, monospace;
  line-height: 10px;
  color: #555;
  vertical-align: middle;
  background-color: #ECFFFA;
  border: solid 1px #ccc;
  border-bottom-color: #bbb;
  border-radius: 3px;
  box-shadow: inset 0 -1px 0 #bbb;
}

h1,h2,h3,h4,h5 {
  color: #269B7D; 
  font-family: "fira sans", "Latin Modern Sans", Calibri, "Trebuchet MS", sans-serif;
}

</style>

# Tweaking

## OpenCSV library
- [https://www.baeldung.com/opencsv](https://www.baeldung.com/opencsv)
- [https://javadoc.io/doc/com.opencsv/opencsv/latest/index.html](https://javadoc.io/doc/com.opencsv/opencsv/latest/index.html)
- [https://opencsv.sourceforge.net/#quick_start](https://opencsv.sourceforge.net/#quick_start)
- [https://stackoverflow.com/questions/9524191/converting-an-csv-file-to-a-json-object-in-java](https://stackoverflow.com/questions/9524191/converting-an-csv-file-to-a-json-object-in-java)
- [https://mvnrepository.com/artifact/com.opencsv/opencsv/5.12.0](https://mvnrepository.com/artifact/com.opencsv/opencsv/5.12.0)
- [https://sourceforge.net/p/opencsv/source/ci/master/tree/](https://sourceforge.net/p/opencsv/source/ci/master/tree/)
- [https://www.geeksforgeeks.org/java/reading-csv-file-java-using-opencsv/](https://www.geeksforgeeks.org/java/reading-csv-file-java-using-opencsv/)

## Issues
- we would like to use a Lazy `Stream<String[]>` instead of the `List<String[]>`
  see [https://www.perplexity.ai/search/can-i-use-opencsv-to-read-all-5NTXgkIyTv.gL.4cFcEM7w#0](https://www.perplexity.ai/search/can-i-use-opencsv-to-read-all-5NTXgkIyTv.gL.4cFcEM7w#0)

## Other sources
- [https://examples.javacodegeeks.com/convert-csv-to-json-using-java/](https://examples.javacodegeeks.com/convert-csv-to-json-using-java/)
- [https://medium.com/@AlexanderObregon/javas-bufferedreader-readline-method-explained-66b76877a7e4](https://medium.com/@AlexanderObregon/javas-bufferedreader-readline-method-explained-66b76877a7e4)
- [https://stackoverflow.com/questions/49456017/map-first-element-of-stream-differently-than-rest](https://stackoverflow.com/questions/49456017/map-first-element-of-stream-differently-than-rest)