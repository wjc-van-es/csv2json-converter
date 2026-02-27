<img src="https://r2cdn.perplexity.ai/pplx-full-logo-primary-dark%402x.png" style="height:64px;margin-right:32px"/>

# With OpenAPI \& Spring Boot, define a service where the request body is an uploaded CSV file that should be processed as a BufferedReader

You model it in OpenAPI as a `multipart/form-data` file upload (`string` + `binary`), which Spring (or your generator) maps to `MultipartFile`; in the controller you turn that into a `BufferedReader` and keep the rest of your logic unchanged.[^1_1][^1_2][^1_3]

## OpenAPI specification

Minimal OpenAPI 3 snippet for “upload a CSV file”:

```yaml
paths:
  /csv:
    post:
      summary: Upload CSV for processing
      requestBody:
        required: true
        content:
          multipart/form-data:
            schema:
              type: object
              required:
                - file
              properties:
                file:
                  type: string
                  format: binary   # maps to MultipartFile
      responses:
        '204':
          description: Processed successfully
```

This is the canonical pattern for file uploads where the body is `multipart/form-data` and the file field is `string` with `binary` format.[^1_2][^1_1]

If you use something like openapi‑processor or springdoc codegen, configure `string:binary` → `org.springframework.web.multipart.MultipartFile` in the type mapping (if not already default).[^1_1]

## Spring Boot controller using BufferedReader

Controller method, as Springdoc will document it correctly as multipart upload:

```java
@RestController
@RequestMapping("/csv")
public class CsvController {

    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Void> uploadCsv(
            @RequestParam("file") MultipartFile file   // or @RequestPart("file")
    ) throws IOException {

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            // Your existing CSV → JSON / whatever logic that consumes a BufferedReader
            processCsv(reader);
        }

        return ResponseEntity.noContent().build();
    }

    private void processCsv(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            // parse/process each CSV line here
        }
    }
}
```

Using `file.getInputStream()` wrapped in `InputStreamReader` and `BufferedReader` is the standard way to read an uploaded CSV line‑by‑line in Spring.[^1_4][^1_3]

## If you want the signature itself to expose BufferedReader

Spring MVC cannot bind the raw request body directly as a `BufferedReader` in combination with multipart; the binding type for file parts is `MultipartFile`, and you adapt it inside the method as shown.[^1_3][^1_2][^1_1]

If you really had a plain `text/csv` body instead of multipart, you could accept a `Reader` or `InputStream` directly:

```java
@PostMapping(path = "/csv/raw", consumes = "text/csv")
public ResponseEntity<Void> uploadRawCsv(Reader reader) throws IOException {
    try (BufferedReader br = new BufferedReader(reader)) {
        processCsv(br);
    }
    return ResponseEntity.noContent().build();
}
```

…but for browser uploads and typical OpenAPI tooling, the multipart + `MultipartFile` pattern is the interoperable choice.
<span style="display:none">[^1_10][^1_5][^1_6][^1_7][^1_8][^1_9]</span>

<div align="center">⁂</div>

[^1_1]: https://openapiprocessor.io/spring/processor/requestbody.html

[^1_2]: https://springdoc.org/faq.html

[^1_3]: https://www.appsloveworld.com/springmvc/100/2/read-data-from-multipartfile-which-has-csv-uploaded-from-browser

[^1_4]: https://www.bezkoder.com/spring-boot-upload-csv-file/

[^1_5]: https://stackoverflow.com/questions/78017028/is-it-possible-to-get-both-multipart-file-and-a-requestbody-in-springboot

[^1_6]: https://stackoverflow.com/questions/72982587/why-springdoc-openapi-doesnt-understand-multipartfile-payload/76282897

[^1_7]: https://nainaistar.hatenablog.com/entry/2020/12/19/083000

[^1_8]: https://github.com/springdoc/springdoc-openapi/issues/1793

[^1_9]: https://stackoverflow.com/questions/72982587/why-springdoc-openapi-doesnt-understand-multipartfile-payload

[^1_10]: https://nainaistar.hatenablog.com/?page=1608420600


---

# I have 3 additional parameters opdrachtId, reden and filename. They are required and I would like to make them query parameters to clearly distinguish them. Filename represents the name of the uploaded file, can I acquire that from the MultipartFile object, instead?

Use three required query parameters on the endpoint and keep only the CSV file in the multipart body; and yes, you can also derive the filename from `MultipartFile` if you want.

### 1. Controller signature with query params

```java
@RestController
@RequestMapping("/csv")
public class CsvController {

    @PostMapping(
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Void> uploadCsv(
            @RequestParam("opdrachtId") Long opdrachtId,
            @RequestParam("reden") String reden,
            @RequestParam("filename") String filename, // query param
            @RequestParam("file") MultipartFile file
    ) throws IOException {

        // Optional: validate filename vs uploaded name
        String uploadedName = file.getOriginalFilename();
        // compare uploadedName and filename, log or reject if mismatch

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            processCsv(opdrachtId, reden, filename, reader);
        }

        return ResponseEntity.noContent().build();
    }

    private void processCsv(Long opdrachtId, String reden,
                            String filename, BufferedReader reader) throws IOException {
        // your processing
    }
}
```

In OpenAPI terms these three become `in: query` parameters with `required: true`, while the request body remains the multipart CSV upload.

### 2. Getting filename from MultipartFile

From `MultipartFile` you can obtain:

- `file.getOriginalFilename()` – the filename as sent by the client (what the browser/file picker supplied).
- `file.getName()` – the name of the form field, i.e. `"file"` in this example.

So you *can* omit the `filename` query parameter and use:

```java
String filename = Optional.ofNullable(file.getOriginalFilename())
                          .orElse("unknown.csv");
```

Whether you keep `filename` as a separate query parameter depends on semantics:

- If you want the client to explicitly state the logical name under which the file should be processed/stored (possibly different from the physical upload name), keeping the query parameter is sensible.
- If you just need “the name of the uploaded file”, relying on `getOriginalFilename()` is enough; then you can drop the `filename` query param and keep only `opdrachtId` and `reden` as query parameters.
<span style="display:none">[^2_1][^2_10][^2_2][^2_3][^2_4][^2_5][^2_6][^2_7][^2_8][^2_9]</span>

<div align="center">⁂</div>

[^2_1]: https://alexbogovich.com/blog/spring-open-api-query-object/

[^2_2]: https://www.speakeasy.com/blog/openapi-tips-query-parameters-serialization

[^2_3]: https://www.baeldung.com/openapi-json-query-parameters

[^2_4]: https://www.speakeasy.com/openapi/requests/parameters/query-parameters

[^2_5]: https://apisyouwonthate.com/blog/generating-openapi-spring-boot/

[^2_6]: https://swagger.io/docs/specification/v3_0/describing-parameters/

[^2_7]: https://stackoverflow.com/questions/60672709/spring-boot-reusable-springdoc-openapi-parameter-annotations

[^2_8]: https://www.baeldung.com/spring-rest-openapi-documentation

[^2_9]: https://openapi-generator.tech/docs/generators/spring/

[^2_10]: https://stackoverflow.com/questions/66053702/springdoc-openapi-with-path-parameters-and-command-object


---

# OK, but does the uploaded file have to be annotated with @RequestParam("file")? What is the difference with @RequestPart("file")?

For a simple file upload like your CSV, both work, and the file does not have to be `@RequestParam("file")` specifically; it can also be `@RequestPart("file")`. The difference is conceptual and only matters once you start having *complex* multipart parts (e.g. JSON + file), not for a plain `MultipartFile`.

### When to use which

- `@RequestParam("file") MultipartFile file`
    - Binds from “request parameters”: query parameters, traditional form fields, and file parts in a multipart request.
    - Best fit for “normal form upload” semantics: `?opdrachtId=...&reden=...` in the query + a file field named `file` in the multipart body.
    - Typical for simple name–value pairs + files.
- `@RequestPart("file") MultipartFile file`
    - Explicitly says “this is a multipart **part**”.
    - Mainly useful when:
        - You have multiple parts with their own `Content-Type` (e.g. one part is JSON, one is a file).
        - You want non‑String/non‑Multipart types to be deserialized via `HttpMessageConverter`, similar to `@RequestBody`.

For your use case:

```java
@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<Void> uploadCsv(
        @RequestParam("opdrachtId") Long opdrachtId,
        @RequestParam("reden") String reden,
        @RequestParam("filename") String filename,
        @RequestParam("file") MultipartFile file
) { ... }
```

is perfectly fine and idiomatic.

If later you change the API to “file + JSON metadata in the body” instead of query parameters, you would normally switch the JSON part to `@RequestPart`:

```java
@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<Void> uploadCsv(
        @RequestPart("metadata") MyMetadataDto metadata,
        @RequestPart("file") MultipartFile file
) { ... }
```

For a single CSV `MultipartFile` plus simple query parameters, sticking with `@RequestParam("file")` is clear and conventional.
<span style="display:none">[^3_1][^3_10][^3_2][^3_3][^3_4][^3_5][^3_6][^3_7][^3_8][^3_9]</span>

<div align="center">⁂</div>

[^3_1]: https://somuchthings.tistory.com/160?category=983431

[^3_2]: https://somuchthings.tistory.com/160

[^3_3]: https://stackoverflow.com/questions/73521326/whats-the-practical-difference-between-requestparam-and-requestpart-for-multi

[^3_4]: https://docs.spring.io/spring-framework/docs/5.0.2.RELEASE/kdoc-api/spring-framework/org.springframework.web.bind.annotation/-request-part/

[^3_5]: https://velog.io/@yhee/Spring-RequestParam-vs-RequestPart-차이점

[^3_6]: https://github.com/openapitools/openapi-generator/issues/21457

[^3_7]: https://www.linkedin.com/posts/bablugiri2001_spring-boot-annotations-requestparam-activity-7376581418950307840-0wzE

[^3_8]: https://docs.spring.io/spring-framework/docs/5.0.0.M5_to_5.0.0.RC1/Spring%20Framework%205.0.0.M5/org/springframework/web/bind/annotation/RequestPart.html

[^3_9]: https://www.baeldung.com/sprint-boot-multipart-requests

[^3_10]: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/requestparam.html


---

# OK I got it, my uploaded file will be singular not multipart, so I stick with the @RequestParam("file") MultipartFile file.

That’s a good choice: for a single uploaded CSV file plus separate query parameters, `@RequestParam("file") MultipartFile file` is the idiomatic and simplest option in Spring MVC.

Your endpoint would then look like:

```java
@PostMapping(path = "/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<Void> uploadCsv(
        @RequestParam("opdrachtId") Long opdrachtId,
        @RequestParam("reden") String reden,
        @RequestParam("filename") String filename, // or drop this and use file.getOriginalFilename()
        @RequestParam("file") MultipartFile file
) throws IOException {

    try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
        processCsv(opdrachtId, reden, filename, reader);
    }

    return ResponseEntity.noContent().build();
}
```

If later you decide you don’t need an explicit `filename` parameter, you can just derive it with `file.getOriginalFilename()` inside the method.

---

# But In the OpenAPI definition the uploaded file is still defined in the request body right? So what  would the OpenAPI definition look like with the opdrachtId and reden query parameters and the file upload?

Yes, the uploaded file is defined in the **requestBody** as `multipart/form-data` even with query parameters—the query params (`opdrachtId`, `reden`, `filename`) are **separate** from the body and defined as `parameters` in OpenAPI.

## Complete OpenAPI 3.0 definition

```yaml
openapi: 3.0.0
info:
  title: CSV Upload API
  version: 1.0.0

paths:
  /csv:
    post:
      summary: Upload CSV file for processing
      parameters:
        - name: opdrachtId
          in: query
          required: true
          schema:
            type: integer
            format: int64
        - name: reden
          in: query
          required: true
          schema:
            type: string
        - name: filename
          in: query
          required: true
          schema:
            type: string
      requestBody:
        required: true
        content:
          multipart/form-data:
            schema:
              type: object
              properties:
                file:
                  type: string
                  format: binary  # maps to MultipartFile
      responses:
        '204':
          description: CSV processed successfully
```


## Key points

**Query parameters** (`parameters` array):

- `opdrachtId`, `reden`, `filename` appear in the URL: `/csv?opdrachtId=123&reden=import&filename=data.csv`

**Request body** (`requestBody`):

- Contains **only** the `file` part as `multipart/form-data`
- The `schema` is an `object` with one property `file: string(binary)`
- Spring maps this to `@RequestParam("file") MultipartFile file`

**Resulting HTTP request**:

```
POST /csv?opdrachtId=123&reden=import&filename=data.csv HTTP/1.1
Content-Type: multipart/form-data; boundary=----WebKitFormBoundary

------WebKitFormBoundary
Content-Disposition: form-data; name="file"; filename="data.csv"
Content-Type: text/csv

col1,col2,col3
value1,value2,value3
------WebKitFormBoundary--
```

This matches your controller signature perfectly and generates correct OpenAPI docs via springdoc-openapi.[^5_1][^5_2]
<span style="display:none">[^5_10][^5_3][^5_4][^5_5][^5_6][^5_7][^5_8][^5_9]</span>

<div align="center">⁂</div>

[^5_1]: https://swagger.io/docs/specification/v3_0/describing-request-body/file-upload/

[^5_2]: https://openapiprocessor.io/spring/processor/requestbody.html

[^5_3]: https://ogen.dev/docs/spec/file_upload/

[^5_4]: https://www.speakeasy.com/openapi/content/file-uploads

[^5_5]: https://swagger.io/docs/specification/v3_0/describing-request-body/describing-request-body/

[^5_6]: https://swagger.dev.org.tw/docs/specification/v3_0/describing-request-body/file-upload/

[^5_7]: https://spec.openapis.org/oas/v3.1.0.html

[^5_8]: https://spec.openapis.org/oas/v3.0.3.html

[^5_9]: https://spec.openapis.org/oas/v3.2.0.html

[^5_10]: https://stackoverflow.com/questions/14455408/how-to-post-files-in-swagger-openapi


---

# I have to convert the csv file content to json. For this I made a converter that has the following method signature: public static void convert(BufferedReader csvInput, Writer jsonOutput, String opdrachtId, String reden, String csvNaam)

I have to pass the resulting jsonOutput to contract first OpenAPI service that creates an API client for us.
Fragment of their OpenAPI spec
requestBody:
content:
application/json:
schema:
\$ref: "\#/components/schemas/OpheffingOpdrachtDto"
This would compel us to convert the content from the Writer jsonOutput to be mapped to an extended tree of (generated class sources) Java objects. Couldn't we simply fool the interface on our end by replacing the requestBody with a simple String object instead of the schema ref, knowing that the json content of that string would conform to the schema rules?

No, you cannot simply "fool" the generated client by changing the OpenAPI spec to `string` instead of `OpheffingOpdrachtDto`—the generated API client code is hardcoded to expect the schema-ref DTO, and changing the spec would break the contract with the service provider.

## Why changing to `string` won't work

Generated clients (e.g. openapi-generator `spring` or `java`) produce **type‑safe** interface methods like:

```java
// Generated from their spec
postSomething(@RequestBody OpheffingOpdrachtDto body);  // hardcoded DTO
```

This won't accept a `String`; it expects the POJO. You can't pass your JSON string directly without:

1. **Modifying the generated client code** (fragile, breaks on regen).
2. **Using a low‑level HTTP client** (bypassing the generated API entirely).

## Better approaches

### 1. Use your converter + StringWriter → ObjectMapper → DTO (Recommended)

Keep the generated client; just build the DTO from your CSV:

```java
public void handleCsv(MultipartFile file, String opdrachtId, String reden, String csvNaam) throws IOException {
    StringWriter jsonOutput = new StringWriter();
    
    try (BufferedReader csvReader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
        CsvToJson.convert(csvReader, jsonOutput, opdrachtId, reden, csvNaam);
    }
    
    // Convert JSON string to DTO
    ObjectMapper mapper = new ObjectMapper();
    OpheffingOpdrachtDto dto = mapper.readValue(jsonOutput.toString(), OpheffingOpdrachtDto.class);
    
    // Call the generated client
    apiClient.postSomething(dto);
}
```

`StringWriter` lets you capture the `Writer jsonOutput` content as a `String`, then `ObjectMapper.readValue()` deserializes it to the exact DTO tree.[^6_1]

**Pros**: Type‑safe, validates schema, uses generated client as‑is.
**Cons**: Loads full JSON into memory (fine unless CSV is gigantic).

### 2. Reactive streaming (if the client supports it)

If the contract service accepts `application/json` streaming and your client is WebClient/WebFlux‑based:

```java
// Generate Flux<DataBuffer> from your CSV converter
Flux<DataBuffer> jsonFlux = produceJsonStream(csvReader, opdrachtId, reden, csvNaam);

webClient.post()
         .uri("/their-endpoint")
         .contentType(MediaType.APPLICATION_JSON)
         .body(BodyInserters.fromValue(jsonFlux))  // streams without full load
         .retrieve()
         .bodyToMono(Void.class)
         .block();
```

But this requires adapting `convert(BufferedReader, Writer, ...)` to stream JSON chunks incrementally (no `StringWriter`).

### 3. Low‑level HTTP client (if DTO tree is too complex)

Bypass generated client entirely:

```java
RestTemplate restTemplate = new RestTemplate();
HttpHeaders headers = new HttpHeaders();
headers.setContentType(MediaType.APPLICATION_JSON);

StringWriter jsonOutput = new StringWriter();
CsvToJson.convert(csvReader, jsonOutput, opdrachtId, reden, csvNaam);

HttpEntity<String> entity = new HttpEntity<>(jsonOutput.toString(), headers);
restTemplate.postForEntity("https://their-service/api", entity, Void.class);
```

**Pros**: Simple, works with any JSON.
**Cons**: No type safety, manual error handling, doesn't use generated client.

**Recommendation**: Go with **Option 1** (`StringWriter` + `ObjectMapper` to DTO)—it leverages your existing converter, the generated client, and schema validation without changing anything upstream.
<span style="display:none">[^6_10][^6_2][^6_3][^6_4][^6_5][^6_6][^6_7][^6_8][^6_9]</span>

<div align="center">⁂</div>

[^6_1]: https://stackoverflow.com/questions/62347380/how-can-i-retrieve-the-raw-json-payload-passed-to-a-spring-boot-post-api

[^6_2]: https://www.codegenes.net/blog/how-to-access-plain-json-body-in-spring-rest-controller/

[^6_3]: https://stackoverflow.com/questions/49145371/spring-boot-json-post-request-body-as-both-pojo-and-string/49146062

[^6_4]: https://www.youtube.com/watch?v=hEr5x6KnH4Y

[^6_5]: https://openapiprocessor.io/spring/processor/requestbody.html

[^6_6]: https://stackoverflow.com/questions/59245851/how-do-i-specify-a-json-raw-message-in-api-object-using-swagger-2-0-or-openapi

[^6_7]: https://community.retool.com/t/api-rest-post-request-body-in-json-is-treated-as-string/35955

[^6_8]: https://swagger.io/docs/specification/v3_0/describing-request-body/describing-request-body/

[^6_9]: https://www.speakeasy.com/api-design/request-body

[^6_10]: https://github.com/Azure/azure-sdk-for-java/wiki/[Management]-Sending-Raw-Json


---

# Is option 1 feasible regarding heapspace memory if the CSV has 2000 lines? How much memory would you allocate for a Kubernetes pod into which this code is deployed?

Yes, option 1 is completely feasible for 2000 CSV lines—peak heap usage would be well under 100MB even in the worst case.

## Memory estimation for 2000 lines

**Rough breakdown** (conservative, assumes ~10 fields/row, ~50 chars/field average):

- CSV input: ~1MB raw (streams, near-zero heap).
- JSON string (`StringWriter.toString()`): ~2-4MB (UTF‑8 chars, compact in Java 9+).
- DTO tree (`ObjectMapper.readValue()`): ~10-50MB peak.
    - Each `String` object: ~50-100 bytes overhead + data.
    - Nested objects/arrays: ~2-5x JSON size multiplier typical.
    - Total for 2000 rows: 20k objects → ~30MB loaded.

**Peak during processing**: ~50-80MB heap spike, gone after `apiClient.postSomething(dto)` completes (GC eligible).[^7_1][^7_2]

**Factors that increase usage**:

- Deeply nested DTO (many levels → more object overhead).
- Long strings or base64 blobs in fields.
- Jackson retaining references during deserialization.

Test it: run with `-Xmx256m` or JProfiler; if >100MB peak, profile object counts.

## Kubernetes pod resources

For this workload (occasional CSV uploads, Spring Boot app):

```
resources:
  requests:
    memory: "256Mi"
    cpu: "250m"
  limits:
    memory: "512Mi"
    cpu: "500m"
```

**Why**:

- **Requests 256Mi**: Covers idle Spring Boot (~100-150Mi base) + 2000-line burst.
- **Limits 512Mi**: Headroom for GC pauses, minor spikes, JVM metaspace.
- Scale horizontally if >10 concurrent uploads/min.

**JVM flags** (add to pod env or args):

```
-Xms128m -Xmx384m -XX:+UseG1GC -XX:MaxGCPauseMillis=100
```

Monitor with Prometheus/Grafana; if OOMs occur (unlikely), bump limits to 1Gi and investigate DTO structure.
<span style="display:none">[^7_3][^7_4][^7_5][^7_6][^7_7][^7_8][^7_9]</span>

<div align="center">⁂</div>

[^7_1]: https://blog.ionelmc.ro/2015/11/22/memory-use-and-speed-of-json-parsers/

[^7_2]: https://ask.clojure.org/index.php/8920/memory-usage-parsing-smallish-csv-file

[^7_3]: https://pythonspeed.com/articles/json-memory-streaming/

[^7_4]: https://www.youtube.com/watch?v=th3vsCDhujo

[^7_5]: https://stackoverflow.com/questions/67636691/out-of-memory-issue-heap-from-generating-large-csv-file

[^7_6]: https://www.reddit.com/r/golang/comments/261m8u/memory_consumption_for_parsing_a_csv_file450mb/

[^7_7]: https://discuss.python.org/t/optimizing-memory-usage-for-large-csv-processing-in-python-3-12/98287

[^7_8]: https://community.altair.com/discussion/46572/solved-amount-of-expected-memory-usage-with-read-csv/p1

[^7_9]: https://dev.to/lovestaco/handling-large-json-files-in-python-efficient-read-write-and-update-strategies-3jgg


---

# With the first option: isn't there an injectable Jackson ObjectMapper managed by Spring MVC Rest? And shouldn't that be used instead of constructing a new ObjectMapper every time the service is called?

Yes, Spring Boot auto-configures a primary `ObjectMapper` bean for Jackson JSON processing in MVC REST controllers, and you should inject it instead of creating new instances.

## Inject the Spring-managed ObjectMapper

**In your service/controller** (constructor injection preferred):

```java
@Service
public class CsvService {
    
    private final ObjectMapper objectMapper;
    
    public CsvService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;  // Spring's configured instance
    }
    
    public void handleCsv(MultipartFile file, String opdrachtId, String reden, String csvNaam) 
            throws IOException {
        
        StringWriter jsonOutput = new StringWriter();
        try (BufferedReader csvReader = new BufferedReader(
                new InputStreamReader(file.getInputStream()))) {
            CsvToJson.convert(csvReader, jsonOutput, opdrachtId, reden, csvNaam);
        }
        
        // Use injected ObjectMapper (shares config with REST controllers)
        OpheffingOpdrachtDto dto = objectMapper.readValue(
            jsonOutput.toString(), OpheffingOpdrachtDto.class);
        
        apiClient.postSomething(dto);
    }
}
```


## Why use Spring's ObjectMapper?

- **Shared configuration**: Inherits your `application.yml` Jackson settings (`spring.jackson.*`).
- **Primary bean**: Auto-configured by `JacksonAutoConfiguration`.
- **Performance**: Single instance, thread-safe, cached modules.
- **Consistency**: Same serialization/deserialization behavior as your REST endpoints.
- **Customizations**: Respects `@JsonComponent` modules, custom serializers.

**No config needed**—Spring Boot provides it automatically when Jackson is on classpath.

## Verify it's working

Spring logs on startup:

```
MappingJackson2HttpMessageConverter configured with ObjectMapper...
```

Or autowire and log:

```java
@PostConstruct
public void init() {
    log.info("Using ObjectMapper: {}", objectMapper.getClass().getName());
}
```

This is the Spring way—clean, consistent, and efficient.
<span style="display:none">[^8_1][^8_10][^8_2][^8_3][^8_4][^8_5][^8_6][^8_7][^8_8][^8_9]</span>

<div align="center">⁂</div>

[^8_1]: https://www.youtube.com/watch?v=JNvapWB9DDY

[^8_2]: https://stackoverflow.com/questions/40423928/how-to-write-single-objectmapper-for-spring-data-rest-and-to-my-restcontroller

[^8_3]: https://stackoverflow.com/questions/51083302/how-to-obtain-jackson-object-mapper-in-spring-rest-mvc-5

[^8_4]: https://stackoverflow.com/questions/71144480/how-to-inject-objectmapper-in-spring

[^8_5]: https://dzone.com/articles/configuring-a-custom-objectmapper-for-spring-restt

[^8_6]: https://www.petrikainulainen.net/programming/testing/writing-unit-tests-for-a-spring-mvc-rest-api-configuration/

[^8_7]: https://github.com/spring-projects/spring-kafka/discussions/2773

[^8_8]: https://www.baeldung.com/spring-boot-customize-jackson-objectmapper

[^8_9]: https://docs.spring.io/spring-boot/docs/1.0.0.RC5/reference/html/howto-spring-mvc.html

[^8_10]: https://www.linkedin.com/posts/sagar-medtiya_springboot-java-jackson-activity-7409087191492173824-tk2r


---

# Returning to the memory usage for a bit.

Besides mapping the json BufferedWriter output to the OpheffingOpdrachtDto Java objects with Jackson, does this take into account the usage of the CsvConverter (which isn't optimized for thrifty memory usage yet. Storing rows in a List<Map<String, Object>). The OpheffingOpdrachtDto looks like this in jsonschema:
{
"\$schema": "https://json-schema.org/draft-07/schema",
"title": "MMO Schema",
"description": "for validating request body ...",
"type": "object",
"properties": {
"opdrachtId": {
"minLength": 1,
"maxLength": 20,
"type": "string"
},
"machtigingTriples": {
"type": "array",
"items": {
"type": "object",
"properties": {
"scanbestandsnaam": {
"minLength": 1,
"maxLength": 30,
"type": "string"
},
"vertegenwoordigdenummer": {
"minLength": 1,
"maxLength": 20,
"type": "string"
},
"gemachtigdenummer": {
"minLength": 1,
"maxLength": 20,
"type": "string"
},
"dienstNaam": {
"type": "string"
},
"intrekkingscode": {
"type": "integer"
}
},
"required": [
"scanbestandsnaam",
"dienstNaam",
"gemachtigdenummer",
"intrekkingscode",
"vertegenwoordigdenummer"
]
},
"minItems": 0
},
"reden": {
"type": "string"
},
"bestandsnaam": {
"type": "string"
}
},
"required": [
"opdrachtId",
"machtigingTriples",
"reden",
"bestandsnaam"
]
}
where the machtigingTriples items can amount to  2000 (corresponding 2000 lines in the CSV)

Yes, my previous estimate (~50-80MB peak) did **not** account for your `CsvConverter`'s intermediate `List<Map<String, Object>>`—that adds significant heap pressure and pushes total peak usage much higher.

## Updated memory breakdown (2000 rows)

**1. CsvConverter peak (`List<Map<String, Object>>`)**[^9_1]

```
Per row Map (~6 keys from schema: scanbestandsnaam, vertegenwoordigdenummer, etc.):
- HashMap overhead: ~64 bytes
- 6 entries × (String key 40B + String value 50B + pointers) ≈ 500 bytes
- Per Map total: ~600 bytes

2000 rows × 600B = **1.2MB for List + Maps**
+ String data (~20 chars/field × 6 × 2000) ≈ **500KB**

CsvConverter peak: ~2MB**
```

**2. JSON StringWriter output**

```
~2-4MB (compact UTF-8 chars)
```

**3. DTO tree (`machtigingTriples` array of 2000 objects)**

```
Per MachtigingTriple object:
- Object overhead: 16B  
- 5 String fields (~50B each): 250B
- Total per triple: ~300B

2000 × 300B = **600KB objects**
+ String data: ~2MB
+ Array/List overhead: ~100KB

DTO peak during Jackson deserialization: **~10MB**
```

**4. **Grand total peak: ~16MB** (concurrent peaks of all 3 phases)

**Still very safe** for 256Mi Kubernetes pod.

## Critical optimization path

Your `CsvConverter` is already reasonably efficient. To make it **thrifty**:

```java
// Instead of List<Map<String,Object>> → direct JSON streaming
public static void convert(BufferedReader csv, Writer json, String opdrachtId, 
                          String reden, String csvNaam) throws IOException {
    json.append("{\"opdrachtId\":\"").append(opdrachtId).append("\",");
    json.append("\"reden\":\"").append(reden).append("\",");
    json.append("\"bestandsnaam\":\"").append(csvNaam).append("\",");
    json.append("\"machtigingTriples\":[");
    
    boolean first = true;
    String[] headers = readHeaders(csv);  // single row
    
    String line;
    while ((line = csv.readLine()) != null) {
        String[] values = parseCsvLine(line);  // OpenCSV or split
        
        if (!first) json.append(",");
        first = false;
        
        json.append("{\"scanbestandsnaam\":\"")
           .append(values[headerIndex("scanbestandsnaam")]).append("\",");
        // ... other 4 fields
        json.append("}");
    }
    
    json.append("]}");
}
```

**Result**: **~4MB peak total** (CSV stream + JSON string only, no intermediate objects).

This eliminates both the `List<Map>` and DTO phases entirely for the contract service call—you stream JSON directly via `WebClient` `Flux<DataBuffer>`. Perfect memory profile.
<span style="display:none">[^9_2][^9_3][^9_4][^9_5][^9_6][^9_7][^9_8]</span>

<div align="center">⁂</div>

[^9_1]: https://lemire.me/blog/2015/10/15/on-the-memory-usage-of-maps-in-java/

[^9_2]: https://stackoverflow.com/questions/33595542/memory-efficient-mapping-of-string-to-liststring

[^9_3]: https://stackoverflow.com/questions/78730892/high-memory-usage-with-large-number-of-object-in-java

[^9_4]: https://stackoverflow.com/questions/6157363/how-to-calculate-hashmap-memory-usage-in-java

[^9_5]: https://dev.to/haraf/java-memory-management-tips-weak-maps-weak-sets-and-pre-sized-collections-with-examples-4o5j

[^9_6]: https://codingtechroom.com/question/understanding-memory-consumption-of-streams-in-java

[^9_7]: https://www.baeldung.com/java-hashmap

[^9_8]: https://blog.stackademic.com/how-to-cut-memory-usage-in-java-without-sacrificing-performance-957dde8610c9

