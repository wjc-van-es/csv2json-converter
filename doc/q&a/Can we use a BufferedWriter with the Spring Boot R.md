<img src="https://r2cdn.perplexity.ai/pplx-full-logo-primary-dark%402x.png" style="height:64px;margin-right:32px"/>

# Can we use a BufferedWriter with the Spring Boot REST client to produce a rather large json request body?

You cannot directly use a BufferedWriter with the Spring Boot REST client to produce a large JSON request body in a streaming fashion. The typical Spring WebClient or RestTemplate expects the request body as a complete object or byte array rather than an open stream to which you write incrementally.

For large JSON request bodies, the recommended approach is to use Spring WebFlux's WebClient with reactive streams, where you can stream the content as DataBuffer objects wrapped in a Flux. This way, you avoid loading the entire JSON into memory at once, and the request body is sent as chunks, which is more memory efficient for very large payloads (e.g., 100+ MB JSON).[^1_1][^1_2]

Specifically, you prepare a Flux<DataBuffer> representing chunks of the serialized JSON and pass it to WebClient's body method. This approach leverages non-blocking I/O and streaming semantics under the hood, rather than traditional blocking writers like BufferedWriter. Using WebClient this way is the modern and efficient solution for streaming large JSON payloads in Spring Boot.

If you were using a server endpoint, BufferedWriter could be used to stream a large JSON response body by writing JSON elements incrementally to the HTTP response output stream. But as a client sending a request body, BufferedWriter is not a supported or practical approach in Spring Boot REST clients.

In summary:

- BufferedWriter is appropriate for server-side streaming JSON responses but not for client request streaming.
- For large JSON requests, use Spring WebClient with Flux<DataBuffer> to stream the body.
- This approach prevents memory spikes by chunking the JSON and sending it reactively.

If you want, I can provide code examples demonstrating how to stream large JSON request bodies with WebClient.
<span style="display:none">[^1_10][^1_3][^1_4][^1_5][^1_6][^1_7][^1_8][^1_9]</span>

<div align="center">⁂</div>

[^1_1]: https://stackoverflow.com/questions/75880974/send-a-huge-json-as-a-stream-using-spring-webclient

[^1_2]: https://www.linkedin.com/pulse/handling-enormous-json-payloads-java-spring-boot-using-saral-saxena-nxgtc

[^1_3]: https://jvmaware.com/streaming-json-response/

[^1_4]: https://www.baeldung.com/java-filewriter-vs-bufferedwriter

[^1_5]: https://stackoverflow.com/questions/57011717/how-to-send-large-json-data-to-the-spring-controller

[^1_6]: https://blog.stackademic.com/scenario-based-question-springboot-rest-3758a9c0872c

[^1_7]: https://dev.to/ratulsharker/consume-large-json-response-in-spring-421j

[^1_8]: https://forums.meteor.com/t/rest-api-with-large-post-requests/60198

[^1_9]: https://www.reddit.com/r/java/comments/b7s9rb/architectural_discussion_handle_very_large_json/

[^1_10]: https://howtodoinjava.com/spring-boot/consume-produce-json/


---

# How to stream JSON request bodies with Spring WebClient

To stream JSON request bodies with Spring WebClient, you can use a Flux<DataBuffer> to send the JSON body as a reactive stream of chunks. This enables sending large JSON request bodies incrementally without loading the entire content into memory at once.

Here is a basic approach:

1. Prepare your JSON content as a Flux<DataBuffer>. For example, you can convert a large JSON file or a serialized JSON string into small chunks wrapped as DataBuffer instances.
2. Use WebClient's body method to pass this Flux<DataBuffer> as the request body and set the content type to application/json.
3. The WebClient will send the request as a streaming HTTP request body.

Example code snippet:

```java
Flux<DataBuffer> jsonDataBufferFlux = ...; // prepare your streaming JSON as DataBuffer Flux

webClient.post()
   .uri("http://example.com/api")
   .contentType(MediaType.APPLICATION_JSON)
   .body(jsonDataBufferFlux, DataBuffer.class)
   .retrieve()
   .bodyToMono(Void.class)
   .block();
```

This approach leverages Spring WebFlux's reactive streams support to handle large JSON payloads efficiently by streaming them over HTTP, avoiding memory spikes.

If you want to build the Flux<DataBuffer> from a large JSON source, consider using DataBufferFactory to create DataBuffer chunks from input streams or byte arrays and wrap each chunk in the Flux stream.

This method contrasts with blocking approaches like BufferedWriter that do not directly integrate with Spring WebClient's reactive body handling for requests.

For typical use with POJOs or smaller JSON, you can just use `bodyValue` or `body(Mono.just(object), Class)` which serializes the full object before sending it. Streaming with Flux<DataBuffer> is best for very large JSON bodies that need to be sent incrementally.

Let me know if you want example code that converts JSON content (e.g., string or file) into a Flux<DataBuffer> for streaming in WebClient.

This is the documented and recommended way to stream JSON request bodies with Spring WebClient efficiently.[^2_2][^2_3][^2_4]
<span style="display:none">[^2_1][^2_10][^2_5][^2_6][^2_7][^2_8][^2_9]</span>

<div align="center">⁂</div>

[^2_1]: https://www.baeldung.com/spring-webclient-json-list

[^2_2]: https://stackoverflow.com/questions/66433565/spring-webclient-pass-dynamic-request-body-as-json

[^2_3]: https://stackoverflow.com/questions/75880974/send-a-huge-json-as-a-stream-using-spring-webclient

[^2_4]: https://docs.spring.io/spring-framework/reference/web/webflux-webclient/client-body.html

[^2_5]: https://www.youtube.com/watch?v=NllnDVDSIJo

[^2_6]: https://www.reddit.com/r/learnjava/comments/tqejnt/get_response_json_with_webclient_in_spring_boot/

[^2_7]: https://github.com/spring-projects/spring-framework/issues/24951

[^2_8]: https://github.com/spring-projects/spring-framework/issues/25472

[^2_9]: https://www.reddit.com/r/SpringBoot/comments/1i24tnv/how_to_persist_the_response_body_of_a_http/

[^2_10]: https://dev.to/stevenpg/logging-with-spring-webclient-2j6o


---

# At present I have code that reads rows of a csv file from a BufferedReader and process it row for row to convert it to elements in a json array written to a BufferedWriter. How would I convert that to fill a Flux<DataBuffer>

The current code fragment:

```
public static void convert(BufferedReader input,
                           BufferedWriter output,
                           char separator,
                           long precalculatedRowSize) throws IOException {
    output.write(PREFIX);
    Wrapper wrapper = setup(input, separator);
    final AtomicBoolean first = new AtomicBoolean(true);
    final AtomicReference<String[]> firstLine = new AtomicReference<>();
    final AtomicLong count = new AtomicLong(0L);
    wrapper.rows().forEach(row -> {
        long currentRow = count.incrementAndGet();
        if (first.get()) {
            firstLine.set(row);
            first.set(false);
        } else {
            procesRow(firstLine, row, output, currentRow == precalculatedRowSize);
        }
    });
    output.write(SUFFIX);
}
```

To convert your current CSV-to-JSON code that uses BufferedReader and BufferedWriter into a Flux<DataBuffer> for streaming JSON request bodies in Spring WebClient, you can follow this approach:

1. Instead of writing synchronously to BufferedWriter, produce a Flux<String> that emits JSON pieces incrementally for each CSV row processed.
2. Convert each JSON string chunk into a DataBuffer using a DataBufferFactory.
3. Return the Flux<DataBuffer> which can be passed to WebClient.body() for streaming.

Here is how you can conceptually adapt your code:

- Keep your logic that reads CSV rows and converts each row to a JSON element string.
- Emit these JSON element strings on a Flux processor or generate a Flux from an Iterator that processes rows on demand.
- Add the opening JSON array prefix (`[`) as the first emitted chunk and the closing suffix (`]`) as the last emitted chunk.
- Use something like this to convert strings to DataBuffer:

```java
DataBufferFactory dataBufferFactory = new DefaultDataBufferFactory();
Flux<DataBuffer> jsonDataBuffers = Flux.fromIterable(jsonPieces)
    .map(piece -> {
        byte[] bytes = piece.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = dataBufferFactory.allocateBuffer(bytes.length);
        buffer.write(bytes);
        return buffer;
    });
```

For example, if `jsonPieces` is a Flux or iterable of JSON strings representing parts of the array (the prefix `[`, each row's JSON string, and suffix `]`), this will create a Flux<DataBuffer> that streams the JSON.

Your CSV reading logic could be wrapped in a `Flux.generate` or `Flux.create` call to push each JSON row as a string asynchronously as it is parsed.

This approach avoids buffering the complete JSON in memory and fits a reactive streaming model that WebClient supports.

If you want, I can help you write a sample reactive method that reads from a CSV BufferedReader and returns a Flux<DataBuffer> of the JSON array, respecting your existing row processing logic.

Key points:

- Use Flux for streaming JSON strings generated row-by-row.
- Convert strings to DataBuffer with DataBufferFactory.
- Include JSON array syntax in emitted chunks.
- Feed result Flux<DataBuffer> to WebClient body.

This fits your described use case for large CSV-to-JSON streaming to the server request.[^3_2][^3_3][^3_10]
<span style="display:none">[^3_1][^3_4][^3_5][^3_6][^3_7][^3_8][^3_9]</span>

<div align="center">⁂</div>

[^3_1]: https://www.youtube.com/watch?v=9XmvT6BA7bQ

[^3_2]: https://stackoverflow.com/questions/72926192/how-to-transform-between-byte-and-fluxdatabuffer

[^3_3]: https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/core/io/buffer/DataBufferUtils.html

[^3_4]: https://gist.github.com/jeronimonunes/cd08f7a03dd45108d2ab94b1d7701737

[^3_5]: https://docs.spring.io/spring-framework/reference/core/databuffer-codec.html

[^3_6]: https://docs.spring.io/spring-framework/docs/5.0.15.RELEASE_to_5.0.16.RELEASE/Spring%20Framework%205.0.15.RELEASE/index.html?org%2Fspringframework%2Fcore%2Fio%2Fbuffer%2FDataBufferUtils.html

[^3_7]: https://www.baeldung.com/convert-input-stream-to-array-of-bytes

[^3_8]: https://javadoc.io/doc/org.springframework/spring-core/5.1.5.RELEASE/org/springframework/core/io/buffer/DataBufferUtils.html

[^3_9]: https://www.baeldung.com/spring-reactive-read-flux-into-inputstream

[^3_10]: https://stackoverflow.com/questions/52882074/in-spring-webflux-how-to-go-from-an-outputstream-to-a-fluxdatabuffer/52898869

