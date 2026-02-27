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
- [https://opencsv.sourceforge.net/licenses.html](https://opencsv.sourceforge.net/licenses.html)
- [https://www.baeldung.com/opencsv](https://www.baeldung.com/opencsv)
- [https://javadoc.io/doc/com.opencsv/opencsv/latest/index.html](https://javadoc.io/doc/com.opencsv/opencsv/latest/index.html)
- [https://opencsv.sourceforge.net/#quick_start](https://opencsv.sourceforge.net/#quick_start)
- [https://stackoverflow.com/questions/9524191/converting-an-csv-file-to-a-json-object-in-java](https://stackoverflow.com/questions/9524191/converting-an-csv-file-to-a-json-object-in-java)
- [https://mvnrepository.com/artifact/com.opencsv/opencsv/5.12.0](https://mvnrepository.com/artifact/com.opencsv/opencsv/5.12.0)
- [https://sourceforge.net/p/opencsv/source/ci/master/tree/](https://sourceforge.net/p/opencsv/source/ci/master/tree/)
- [https://www.geeksforgeeks.org/java/reading-csv-file-java-using-opencsv/](https://www.geeksforgeeks.org/java/reading-csv-file-java-using-opencsv/)

## OpenAPI service integration
- MMO question
  [https://www.perplexity.ai/search/with-openapi-spring-boot-defin-OtV.Qvl7TuG_5UDcvLVJMQ](https://www.perplexity.ai/search/with-openapi-spring-boot-defin-OtV.Qvl7TuG_5UDcvLVJMQ)

## Issues

### Applying a Stream instead of loading a List in memory
- we would like to use a Lazy `Stream<String[]>` instead of the `List<String[]>`
  see [https://www.perplexity.ai/search/can-i-use-opencsv-to-read-all-5NTXgkIyTv.gL.4cFcEM7w#0](https://www.perplexity.ai/search/can-i-use-opencsv-to-read-all-5NTXgkIyTv.gL.4cFcEM7w#0)

### Configuring for casual use of double quotes to escape the use of the separator character within a field
- [https://www.perplexity.ai/search/i-read-a-csv-with-opencsv-in-j-aTFImm3hQkC05fhJSCDmHQ#0](https://www.perplexity.ai/search/i-read-a-csv-with-opencsv-in-j-aTFImm3hQkC05fhJSCDmHQ#0)

### Establishing the csv row count whilst using the iterator and stream instead of in-memory collection
- We need to know the row count to be able to know when _not_ to add a comma when adding the last element to a json list.
- The only reliable solution is reading the csv file twice:
  - once to establish the row count
  - twice to do the actual conversion to json by supplying the row count value calculated in the first read.
- This strategy is perfectly acceptable when optimizing for memory use and won't take up much extra time.
- It is reminiscent to Apache Spark reading a data file twice when no schema is provided: 
  - once to defer the data schema and 
  - the second time to fill the appropriate dataframe object with the actual data
- [https://www.perplexity.ai/search/in-java-writing-from-elements-D.Y8lyorT0SMPHwpea_FKg#0](https://www.perplexity.ai/search/in-java-writing-from-elements-D.Y8lyorT0SMPHwpea_FKg#0)

### Can we use a BufferedWriter with the Spring Boot REST client to produce a rather large json request body
- It appears this is not possible, and we should use Spring WebClient with Flux<DataBuffer>
- [https://www.perplexity.ai/search/can-we-use-a-bufferedwriter-wi-PA5S2lBxQge8Ii_wgTWDmA#2](https://www.perplexity.ai/search/can-we-use-a-bufferedwriter-wi-PA5S2lBxQge8Ii_wgTWDmA#2)
- Chapter 8 from _Cloud Native Spring in Action_ by Thomas Vitale: Reactive Spring: Resilience and scalability

#### Reactive programming Quarkus vs Spring (Mutiny vs Reactor)
- [https://github.com/rodrigorodrigues/quarkus-vs-springboot-reactive-rest-api](https://github.com/rodrigorodrigues/quarkus-vs-springboot-reactive-rest-api)

### Benchmarking memory use
- [https://www.perplexity.ai/search/testing-java-code-for-memory-h-0IGkt_nUSuyevYc_Kq6Yog#0](https://www.perplexity.ai/search/testing-java-code-for-memory-h-0IGkt_nUSuyevYc_Kq6Yog#0)
- [https://www.baeldung.com/java-microbenchmark-harness](https://www.baeldung.com/java-microbenchmark-harness)
- [https://nljug.org/foojay/benchmarking-and-profiling-java-with-jmh/](https://nljug.org/foojay/benchmarking-and-profiling-java-with-jmh/)

### Spring Boot RestClient & OpenAPI
- [https://docs.spring.io/spring-framework/reference/integration/rest-clients.html](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html)
- [https://www.baeldung.com/spring-rest-openapi-documentation](https://www.baeldung.com/spring-rest-openapi-documentation)
- [https://medium.com/@nina.trausner/mastering-openapi-a-comprehensive-guide-to-building-and-integrating-openapi-in-your-spring-boot-8613f633b6c0](https://medium.com/@nina.trausner/mastering-openapi-a-comprehensive-guide-to-building-and-integrating-openapi-in-your-spring-boot-8613f633b6c0)
- [https://www.theserverside.com/video/OpenAPI-Swagger-and-Spring-Boot-REST-APIs](https://www.theserverside.com/video/OpenAPI-Swagger-and-Spring-Boot-REST-APIs)
- [https://www.youtube.com/watch?v=6kwmW_p_Tig](https://www.youtube.com/watch?v=6kwmW_p_Tig)

## Other sources
- [https://examples.javacodegeeks.com/convert-csv-to-json-using-java/](https://examples.javacodegeeks.com/convert-csv-to-json-using-java/)
- [https://medium.com/@AlexanderObregon/javas-bufferedreader-readline-method-explained-66b76877a7e4](https://medium.com/@AlexanderObregon/javas-bufferedreader-readline-method-explained-66b76877a7e4)
- [https://stackoverflow.com/questions/49456017/map-first-element-of-stream-differently-than-rest](https://stackoverflow.com/questions/49456017/map-first-element-of-stream-differently-than-rest)