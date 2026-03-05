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


---

# Do we need to create a RestController or is it also possible to implement the OpheffingsetsApiDelegate that is generated from the OpenAPI spec?

You don’t need to create your own `@RestController` from scratch; you can absolutely implement the generated `OpheffingsetsApiDelegate` and let the generated controller class handle the web layer.

## How the delegate pattern works

With the Spring generator and `delegatePattern=true`, OpenAPI Generator produces:

- A `OpheffingsetsApiController` class annotated with `@Controller` / `@RequestMapping` and all the `@PostMapping`, `@RequestParam`, `@RequestBody` etc. based on the spec.[^10_1][^10_2]
- A `OpheffingsetsApiDelegate` interface with default methods for each endpoint that just return 501 (Not Implemented).[^10_3][^10_1]

The generated controller looks conceptually like:

```java
@Controller
@RequestMapping("${openapi.base-path:/}")
public class OpheffingsetsApiController implements OpheffingsetsApi {

    private final OpheffingsetsApiDelegate delegate;

    @Autowired
    public OpheffingsetsApiController(@Autowired(required = false) OpheffingsetsApiDelegate delegate) {
        this.delegate = Optional.ofNullable(delegate).orElse(new OpheffingsetsApiDelegate() {});
    }

    @Override
    public OpheffingsetsApiDelegate getDelegate() {
        return delegate;
    }
}
```

Your job is then only to implement the delegate.

## What you implement

You create a Spring bean implementing `OpheffingsetsApiDelegate`:

```java
@Service
public class OpheffingsetsApiDelegateImpl implements OpheffingsetsApiDelegate {

    private final CsvService csvService; // your CSV+JSON logic

    public OpheffingsetsApiDelegateImpl(CsvService csvService) {
        this.csvService = csvService;
    }

    @Override
    public ResponseEntity<Void> createOpheffingset( // method name/signature from generated delegate
            String opdrachtId,
            String reden,
            String bestandsnaam,
            MultipartFile file // or OpheffingOpdrachtDto, depending on which endpoint this is
    ) {
        csvService.handleCsv(file, opdrachtId, reden, bestandsnaam);
        return ResponseEntity.noContent().build();
    }
}
```

Because this class is a Spring bean (`@Service`), the generated `OpheffingsetsApiController` will auto‑wire it and forward all HTTP requests to your implementation.[^10_1][^10_3]

## When to choose delegate vs custom controller

- **Use `OpheffingsetsApiDelegate`** when:
    - You want to stay aligned with the generated server stubs.
    - The OpenAPI spec is the source of truth for the REST contract.
    - You prefer to only write business logic and not web mapping boilerplate.
- **Write your own `@RestController`** only if:
    - You’re not using server stub generation for that endpoint.
    - You need custom mappings not present in the spec.

In your case, since you already have a generated `OpheffingsetsApiDelegate`, the cleanest approach is to implement that interface and keep all mapping (query params, request body, etc.) in the generated controller.
<span style="display:none">[^10_10][^10_4][^10_5][^10_6][^10_7][^10_8][^10_9]</span>

<div align="center">⁂</div>

[^10_1]: https://www.baeldung.com/java-openapi-generator-server

[^10_2]: https://stackoverflow.com/questions/64089958/openapi-generator-spring-boot-and-delegatepattern

[^10_3]: https://www.palo-it.com/en/blog/spring-boot-client-and-server-code-generation-using-openapi-3-specs

[^10_4]: https://stackoverflow.com/questions/76975549/how-to-generate-delegates-and-controllers-for-spring-openapi-generator-6-6-0

[^10_5]: https://stackoverflow.com/questions/65005280/generate-only-rest-interfaces-for-spring-boot-with-openapi-generator-gradle-plug

[^10_6]: https://github.com/OpenAPITools/openapi-generator/issues/6792

[^10_7]: https://github.com/OpenAPITools/openapi-generator/issues/12564

[^10_8]: https://github.com/OpenAPITools/openapi-generator/blob/master/modules/openapi-generator/src/main/resources/JavaSpring/apiDelegate.mustache

[^10_9]: https://github.com/OpenAPITools/openapi-generator/issues/20604

[^10_10]: https://stackoverflow.forums.wiki/viewtopic.php?t=80750


---

# I did that generally but the unit test with Mockito did not trigger the service method (In debug mode the breakpoint inside the service wasn't reached) So probably something is missing in my unit test.

@WebMvcTest(OmzettingsetsApiController.class)
@Import(ApplicationNoSecurity.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles({"unit-test","plainLogging"})
class OpheffingsServiceTest {
@Autowired
MockMvc mockMvc;

    @SpyBean
    OpheffingsService opheffingsService;
    
    @MockitoBean
    private MachtigingenMassaalOpheffenRestControllerApi machtigingenMassaalOpheffenRestControllerApi;
    
    @MockitoBean
    private AuthenticationService authenticationService;
    
    @MockitoBean
    private AuditProducer producer;
    
    @Test
    void testMassaleOpheffingOpdracht() throws Exception {
        MockMultipartFile validCsvFile = new MockMultipartFile("opheffen.csv", "orgineel-opheffen.csv",
                "text/csv",
                inputStreamFromClasspath(VALID_CSV_INPUT));
        MockMultipartHttpServletRequestBuilder builder = MockMvcRequestBuilders.multipart(POST_MMO_PATH);
        builder.with(request -> {
            request.setMethod("POST");
            request.addHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString(AUTHENTICATION_CREDENTIALS.getBytes()));
    //           request.addHeader("Content-Type", MediaType.MULTIPART_FORM_DATA_VALUE);
request.addParameter("opdrachtId", "INC0011789123");
request.addParameter("reden", "brief ontvangen");
return request;
});

        when(machtigingenMassaalOpheffenRestControllerApi.registreerOpdrachtWithHttpInfo(
                any(OpheffingOpdrachtDto.class))).thenReturn(makeRegistreerOpdrachtWithHttpInfo());
        mockMvc.perform(builder.file(validCsvFile))
                .andDo(print())
                .andExpect(status().isOk());
    verify(opheffingsService, times(1)).massaleOpheffingOpdracht(
any(String.class), any(String.class), any(MultipartFile.class));
Production code summary:
public class OpheffingsService implements OpheffingsetsApiDelegate {
method signature:
@Override
public ResponseEntity<BFFOpheffingsetDto> massaleOpheffingOpdracht(String opdrachtId, String reden, MultipartFile file)

You can unit-test through the generated controller, but with the delegate pattern you must ensure that your `OpheffingsService` bean is actually wired into that controller (and that your test uses the right controller class).

### 1. Verify which class you annotate in @WebMvcTest

Your snippet shows:

```java
@WebMvcTest(OmzettingsetsApiController.class)
...
class OpheffingsServiceTest {
```

but your service implements `OpheffingsetsApiDelegate`, and earlier you mentioned `OpheffingsetsApiController`. If you generated an `OpheffingsetsApiController`, your test should typically be:

```java
@WebMvcTest(OpheffingsetsApiController.class)
```

If you point `@WebMvcTest` at the wrong controller, Spring won’t route the request to your delegate, so the service method won’t be hit.[^11_1][^11_2]

### 2. Make sure the delegate bean is picked up

Typical generated controller (simplified) looks like this:[^11_3][^11_4]

```java
@Controller
@RequestMapping("${openapi.base-path:/}")
public class OpheffingsetsApiController implements OpheffingsetsApi {

    private final OpheffingsetsApiDelegate delegate;

    @Autowired
    public OpheffingsetsApiController(
            @Autowired(required = false) OpheffingsetsApiDelegate delegate) {
        this.delegate = Optional.ofNullable(delegate).orElse(new OpheffingsetsApiDelegate() {});
    }

    @Override
    public OpheffingsetsApiDelegate getDelegate() {
        return delegate;
    }
}
```

You implemented:

```java
@Service
public class OpheffingsService implements OpheffingsetsApiDelegate {
    @Override
    public ResponseEntity<BFFOpheffingsetDto> massaleOpheffingOpdracht(
            String opdrachtId, String reden, MultipartFile file) { ... }
}
```

In the test you use:

```java
@SpyBean
OpheffingsService opheffingsService;
```

That is correct in principle: `@SpyBean` wraps the real Spring bean so the controller should receive this spy as its `delegate`, and your `verify(...)` should work, **provided**:

- The test context really contains `OpheffingsService` as a bean (it’s in a scanned package).
- `@WebMvcTest` is configured to include it or you import it explicitly.

By default, `@WebMvcTest` only scans web-related components and might not pick up your `@Service` automatically.[^11_5][^11_2]

To be explicit, add:

```java
@WebMvcTest(OpheffingsetsApiController.class)
@Import({ApplicationNoSecurity.class, OpheffingsService.class})
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles({"unit-test","plainLogging"})
class OpheffingsServiceTest {
    ...
}
```

Now Spring will create the `OpheffingsService` bean, wrap it as a spy, and inject it into the generated controller as the delegate.

### 3. Match the endpoint signature and parameters

Your delegate method:

```java
public ResponseEntity<BFFOpheffingsetDto> massaleOpheffingOpdracht(
        String opdrachtId, String reden, MultipartFile file)
```

But in the test you send:

```java
MockMultipartFile validCsvFile = new MockMultipartFile(
    "opheffen.csv", "orgineel-opheffen.csv", "text/csv", ...
);
...
builder.addParameter("opdrachtId", "INC0011789123");
builder.addParameter("reden", "brief ontvangen");
mockMvc.perform(builder.file(validCsvFile))
```

For Spring MVC to bind properly into the generated controller (and from there to the delegate), the multipart part name must match the parameter name in the mapping. In OpenAPI, for example, if the file property is called `file`, the generated method will have a `@RequestParam("file") MultipartFile file` or similar.[^11_6][^11_3]

So ensure:

- The name in `MockMultipartFile` matches what the controller expects, e.g.:

```java
MockMultipartFile validCsvFile = new MockMultipartFile(
    "file", "orgineel-opheffen.csv", "text/csv",
    inputStreamFromClasspath(VALID_CSV_INPUT)
);
```

Otherwise, the controller method may not be selected or may fail binding, and your delegate won’t be called at all.

### 4. Consider switching to @SpringBootTest if problems persist

Because you’re testing the full integration between:

- generated controller,
- delegate service,
- security stub (`AuthenticationService`), and
- external client mock (`MachtigingenMassaalOpheffenRestControllerApi`),

you might get a more realistic setup with:

```java
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles({"unit-test","plainLogging"})
class OpheffingsServiceTest {
    @Autowired MockMvc mockMvc;

    @SpyBean OpheffingsService opheffingsService;

    @MockBean MachtigingenMassaalOpheffenRestControllerApi machtigingenMassaalOpheffenRestControllerApi;
    @MockBean AuthenticationService authenticationService;
    @MockBean AuditProducer producer;
}
```

`@SpringBootTest` loads the whole context, so you don’t have to fight `@WebMvcTest`’s narrow scan scope.[^11_5]

***

Checklist to get your `verify(opheffingsService...)` to pass:

1. Use the **correct generated controller class** in `@WebMvcTest`.
2. Ensure `OpheffingsService` is in context (`@Import` or `@SpringBootTest`).
3. Make the multipart **field name** in `MockMultipartFile` match what the generated method expects.
4. Keep `@SpyBean OpheffingsService` so you can `verify(...)` the `massaleOpheffingOpdracht(...)` call.
<span style="display:none">[^11_10][^11_11][^11_7][^11_8][^11_9]</span>

<div align="center">⁂</div>

[^11_1]: https://www.arhohuttunen.com/spring-boot-webmvctest/

[^11_2]: https://docs.spring.io/spring-boot/api/java/org/springframework/boot/test/autoconfigure/web/servlet/WebMvcTest.html

[^11_3]: https://www.baeldung.com/java-openapi-generator-server

[^11_4]: https://github.com/OpenAPITools/openapi-generator/blob/master/modules/openapi-generator/src/main/resources/JavaSpring/apiDelegate.mustache

[^11_5]: https://www.baeldung.com/spring-mockmvc-vs-webmvctest

[^11_6]: https://github.com/OpenAPITools/openapi-generator/issues/8659

[^11_7]: https://stackoverflow.com/questions/15302243/spring-mvc-controllers-unit-test-not-calling-controlleradvice

[^11_8]: https://stackoverflow.com/questions/77302047/spring-webmvctest-fails-with-preauthorize-controller-classes-generated-by-open

[^11_9]: https://stackoverflow.com/questions/64724771/webmvctest-attempts-to-load-every-application-controller

[^11_10]: https://stackoverflow.com/questions/64089958/openapi-generator-spring-boot-and-delegatepattern

[^11_11]: https://stackoverflow.com/questions/66294655/significance-of-delegate-design-pattern-in-swagger-generated-code


---

# item1: @WebMvcTest(OpheffingsetsApiController.class) instead of @WebMvcTest(OmzettingsetsApiController.class)

and item 3
MockMultipartFile validCsvFile = new MockMultipartFile(
"file", "orgineel-opheffen.csv", "text/csv", ...
);
instead of
MockMultipartFile validCsvFile = new MockMultipartFile(
"opheffen.csv", "orgineel-opheffen.csv", "text/csv", ...
);
fixed it.

Perfect! Those two fixes nailed it:

## What was happening

1. **`@WebMvcTest(OmzettingsetsApiController.class)`** → wrong controller class, so Spring loaded the wrong `@Controller` and never routed to your delegate.
2. **`MockMultipartFile("opheffen.csv", ...)`** → the first argument is the **multipart form field name** (not the filename). Spring couldn’t bind it to the `@RequestParam("file") MultipartFile file` parameter in the generated controller method, so the method wasn’t invoked.[^12_1]

## Quick reference for MockMultipartFile

```java
new MockMultipartFile(
    "file",                           // ← form field name (matches @RequestParam)
    "orgineel-opheffen.csv",          // ← original filename
    "text/csv",                       // ← content type
    inputStream                       // ← content
);
```

Now your test chain works:

```
HTTP request → generated OpheffingsetsApiController → your OpheffingsService delegate → verify() passes ✅
```

Great debugging!

<div align="center">⁂</div>

[^12_1]: https://github.com/OpenAPITools/openapi-generator/issues/8659


---

# What if you want to mock the ObjectMapper to test when it throws a

Just adding
@MockitoBean
private ObjectMapper  objectMapper;
This causes the SpringContext setup to fail:
Caused by: java.lang.NullPointerException: Cannot invoke "com.fasterxml.jackson.databind.ObjectReader.forType(java.lang.Class)" because the return value of "com.fasterxml.jackson.databind.ObjectMapper.reader()" is null

Mocking the `ObjectMapper` at the Spring context level is what’s breaking things here. The generated controllers and other infrastructure beans expect a fully configured, non-mocked `ObjectMapper`; when you replace it with a Mockito mock, their initialization code calls methods like `reader()` and gets `null` back, which leads to that NPE.

### Why `@MockitoBean ObjectMapper` breaks the context

- Spring Boot auto-configures a real `ObjectMapper` and wires it into:
    - HTTP message converters,
    - generated OpenAPI controllers,
    - possibly other infrastructure.
- When you do:

```java
@MockitoBean
private ObjectMapper objectMapper;
```

you replace that bean with a Mockito mock.
- Mockito mocks return `null` for *unstubbed* methods by default.
Framework code somewhere does:

```java
objectMapper.reader().forType(SomeClass.class);
```

but your mock’s `reader()` returns `null`, so `forType(...)` on `null` → NPE during context startup.

So: you generally should **not** mock the global `ObjectMapper` bean in a Spring MVC slice/integration test.

### Better ways to test “ObjectMapper throws …”

Pick one of these patterns depending on what you really want to test.

#### 1. Don’t mock `ObjectMapper` at all; let it be real

For controller / delegate tests with MockMvc, use the real mapper and assert the *observable behavior* when deserialization fails:

- Send malformed JSON to the endpoint that uses `@RequestBody OpheffingOpdrachtDto dto`.
- Assert status, error body, maybe log entry.

This tests your error handling end-to-end without touching `ObjectMapper` at all.

#### 2. Isolate the code that uses ObjectMapper and mock *there* (pure unit test)

If you have a method like:

```java
@Service
public class SomeService {
    private final ObjectMapper mapper;

    public SomeService(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public void doSomething(String json) {
        OpheffingOpdrachtDto dto = mapper.readValue(json, OpheffingOpdrachtDto.class);
        // ...
    }
}
```

Write a **plain** unit test (no `@WebMvcTest`, no Spring context) using Mockito:

```java
@ExtendWith(MockitoExtension.class)
class SomeServiceTest {

    @Mock
    ObjectMapper objectMapper;

    @InjectMocks
    SomeService someService;

    @Test
    void whenMapperThrows_wrapsException() throws Exception {
        when(objectMapper.readValue(anyString(), eq(OpheffingOpdrachtDto.class)))
                .thenThrow(new JsonProcessingException("boom") {});

        assertThrows(MyWrapperException.class,
                     () -> someService.doSomething("{bad json}"));
    }
}
```

Here mocking `ObjectMapper` is fine because it’s not the global Spring bean; it’s just a dependency of a class you’re unit-testing.

#### 3. If you *must* stub the bean in a Spring test, use a spy, not a mock

If some Spring bean is directly autowiring the `ObjectMapper` and you want to intercept one specific call, you can:

```java
@SpringBootTest
class MyTest {

    @SpyBean
    ObjectMapper objectMapper;

    @Test
    void testSomething() throws Exception {
        doThrow(new JsonProcessingException("boom") {})
                .when(objectMapper).readValue(anyString(), eq(OpheffingOpdrachtDto.class));

        // perform request / call service
    }
}
```

A `@SpyBean` wraps the real mapper; all non-stubbed methods still behave normally, so framework initialization doesn’t break. You only override the method you care about.

### Summary

- Don’t replace the global `ObjectMapper` bean with a pure Mockito mock in `@WebMvcTest` / `@SpringBootTest`; it breaks Spring’s own usage of the mapper and causes NPEs.
- For behavior “when ObjectMapper throws …”:
    - Prefer a *plain* unit test on your service with a mocked mapper.
    - Or use `@SpyBean ObjectMapper` if you really need to do it in a Spring-based test.
- For controller tests, keep the real mapper and assert HTTP-level behavior instead of mocking it.
<span style="display:none">[^13_1][^13_2][^13_3][^13_4][^13_5][^13_6][^13_7][^13_8][^13_9]</span>

<div align="center">⁂</div>

[^13_1]: https://codingtechroom.com/question/how-to-resolve-nullpointerexception-in-spring-mockmvc-tests

[^13_2]: https://stackoverflow.com/questions/71510312/using-junit-mocked-objectmapper-shows-null-java-lang-nullpointerexception-obje

[^13_3]: https://www.youtube.com/watch?v=Kjn4aDYb75o

[^13_4]: https://www.appsloveworld.com/springboot/100/162/using-junit-mocked-objectmapper-shows-null-java-lang-nullpointerexception-objec

[^13_5]: https://stackoverflow.com/questions/61504067/model-mapper-mock-returns-null-object-in-spring-boot-unit-test

[^13_6]: https://coderanch.com/t/739460/frameworks/NullPointerException-run-unit-test-Spring

[^13_7]: https://github.com/mockito/mockito/issues/2026

[^13_8]: https://www.baeldung.com/mockito-mock-jackson-read-value

[^13_9]: https://red-green-coding.github.io/bettertests/2023/10/16/testing_objectmapper_mock.html


---

# The only alternative would be to mock CsvConverter.convert(reader, bw, opdrachtId, reden, file.getOriginalFilename());. But this a static method called inside the massaleOpheffingOpdracht(String opdrachtId, String reden, MultipartFile file) method of OpheffingsService

Yes, you can mock the static `CsvConverter.convert()` method using **Mockito's `mockStatic()`** (available since Mockito 3.4.0). Since you're already using `@SpyBean OpheffingsService`, this fits perfectly in your `@WebMvcTest`.

## Mock static method in your test

```java
@WebMvcTest(OpheffingsetsApiController.class)
@Import(OpheffingsService.class)  // your delegate
@AutoConfigureMockMvc(addFilters = false)
class OpheffingsServiceTest {

    @Autowired MockMvc mockMvc;
    @SpyBean OpheffingsService opheffingsService;

    // your other mocks...

    @Test
    void testMassaleOpheffingOpdracht_whenCsvConverterThrowsJsonException() throws Exception {
        // Mock the static method to throw JsonProcessingException
        try (MockedStatic<CsvConverter> mockedStatic = Mockito.mockStatic(CsvConverter.class)) {
            mockedStatic.when(() -> CsvConverter.convert(
                    any(BufferedReader.class), 
                    any(Writer.class), 
                    eq("INC0011789123"), 
                    eq("brief ontvangen"), 
                    anyString()))
                .thenThrow(new JsonProcessingException("Invalid CSV structure") {});

            // Perform the request
            MockMultipartFile validCsvFile = new MockMultipartFile(
                "file", "orgineel-opheffen.csv", "text/csv",
                inputStreamFromClasspath(VALID_CSV_INPUT));

            mockMvc.perform(MockMvcRequestBuilders.multipart(POST_MMO_PATH)
                    .file(validCsvFile)
                    .param("opdrachtId", "INC0011789123")
                    .param("reden", "brief ontvangen")
                    .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(AUTHENTICATION_CREDENTIALS.getBytes())))
                .andDo(print())
                .andExpect(status().isBadRequest());  // or whatever your error handling returns
        }

        // verify(opheffingsService...) if needed
    }
}
```


## Key points

1. **`try (MockedStatic<CsvConverter> mockedStatic = ...)`** - Scoped to the test method only; automatically cleans up.
2. **`mockedStatic.when(() -> CsvConverter.convert(...)).thenThrow(...)`** - Exact method signature match with `any()`/`eq()` matchers.
3. **No context breakage** - Unlike mocking `ObjectMapper`, this doesn't affect Spring infrastructure.

## Matchers for the static method signature

Since your converter is:

```java
public static void convert(BufferedReader csvInput, Writer jsonOutput, 
                          String opdrachtId, String reden, String csvNaam)
```

Use:

```java
mockedStatic.when(() -> CsvConverter.convert(
        any(BufferedReader.class),      // csvInput  
        any(Writer.class),              // jsonOutput
        eq("INC0011789123"),           // opdrachtId (exact match)
        eq("brief ontvangen"),         // reden (exact match)  
        anyString()                    // csvNaam
    )).thenThrow(...);
```


## Dependencies

Ensure your `pom.xml` has:

```xml
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-inline</artifactId>
    <scope>test</scope>
</dependency>
```

This is the cleanest way to test the error case without breaking Spring context or needing `@SpringBootTest`.
<span style="display:none">[^14_1][^14_10][^14_2][^14_3][^14_4][^14_5][^14_6][^14_7][^14_8][^14_9]</span>

<div align="center">⁂</div>

[^14_1]: https://www.browserstack.com/guide/mockito-mock-static-method

[^14_2]: https://stackoverflow.com/questions/76406935/mock-static-method-in-spring-boot-integration-test

[^14_3]: https://www.parasoft.com/blog/love-spring-testing-even-more-with-mocking-and-unit-test-assistant/

[^14_4]: https://www.codecentric.de/en/knowledge-hub/blog/testing-and-mocking-of-static-methods-in-java

[^14_5]: https://www.testim.io/blog/mocking-static-methods-mockito/

[^14_6]: https://www.youtube.com/watch?v=GP0LbtG6vx8

[^14_7]: https://www.baeldung.com/mockito-mock-static-methods

[^14_8]: https://www.diffblue.com/resources/mockito-mock-static-methods-java-guide/

[^14_9]: https://rieckpil.de/mocking-static-methods-with-mockito-java-kotlin/

[^14_10]: https://stackoverflow.com/questions/44110794/how-to-mock-static-methods-in-spring-boot-test


---

# and if we want this mock to produce a StringWriter that will return a specified String when its toString is called?

Yes! You can mock `CsvConverter.convert()` to populate a `StringWriter` that returns your specified JSON string when `toString()` is called. Use Mockito's **Answer** to control the `Writer` behavior:

```java
@Test
void testMassaleOpheffingOpdracht_withValidCsvConverterOutput() throws Exception {
    final String expectedJson = "{\"opdrachtId\":\"INC0011789123\",\"reden\":\"brief ontvangen\",\"bestandsnaam\":\"test.csv\",\"machtigingTriples\":[...]}";
    
    try (MockedStatic<CsvConverter> mockedStatic = Mockito.mockStatic(CsvConverter.class)) {
        mockedStatic.when(() -> CsvConverter.convert(
                any(BufferedReader.class), 
                any(Writer.class), 
                eq("INC0011789123"), 
                eq("brief ontvangen"), 
                anyString()))
            .thenAnswer(invocation -> {
                // Get the Writer argument passed by your service
                Writer jsonWriter = invocation.getArgument(1);
                
                // Write your expected JSON to it
                jsonWriter.write(expectedJson);
                jsonWriter.flush();  // important!
                
                return null;  // void method
            });

        MockMultipartFile csvFile = new MockMultipartFile(
            "file", "test.csv", "text/csv", "dummy,csv,data");

        mockMvc.perform(MockMvcRequestBuilders.multipart(POST_MMO_PATH)
                .file(csvFile)
                .param("opdrachtId", "INC0011789123")
                .param("reden", "brief ontvangen")
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(AUTHENTICATION_CREDENTIALS.getBytes())))
            .andDo(print())
            .andExpect(status().isOk());

        // Now ObjectMapper.readValue() will deserialize expectedJson → DTO → API call succeeds
    }
}
```


## How it works

1. **`invocation.getArgument(1)`** → extracts the `StringWriter` your service passes to `convert()`
2. **`jsonWriter.write(expectedJson)`** → populates it with your test JSON
3. **`jsonWriter.flush()`** → ensures content is available when service calls `jsonOutput.toString()`
4. Service proceeds normally: `ObjectMapper.readValue(expectedJson, OpheffingOpdrachtDto.class)` → success

## Verify the JSON flows through

```java
@SpyBean
private MachtigingenMassaalOpheffenRestControllerApi machtigingenApi;

@Test
void testMassaleOpheffingOpdracht_jsonMatchesExpected() throws Exception {
    // ... mock setup with expectedJson as above ...
    
    mockMvc.perform(...)  // request
        .andExpect(status().isOk());

    // Verify the exact DTO was sent to downstream API
    verify(machtigingenApi).registreerOpdrachtWithHttpInfo(argThat(dto -> 
        dto.getOpdrachtId().equals("INC0011789123") &&
        dto.getReden().equals("brief ontvangen")));
}
```

This gives you **full control** over what JSON your converter "produces" without touching `ObjectMapper` or Spring infrastructure. Perfect for testing both success and malformed JSON scenarios.
<span style="display:none">[^15_1][^15_10][^15_2][^15_3][^15_4][^15_5][^15_6][^15_7][^15_8][^15_9]</span>

<div align="center">⁂</div>

[^15_1]: https://stackoverflow.com/questions/10583202/powermockito-mock-single-static-method-and-return-object

[^15_2]: https://stackoverflow.com/questions/75078316/mockito-mock-return-value-of-static-method-of-util-class-in-a-different-thread

[^15_3]: https://github.com/mockito/mockito/issues/2027

[^15_4]: https://www.browserstack.com/guide/mockito-mock-static-method

[^15_5]: https://stackoverflow.com/questions/21105403/mocking-static-methods-with-mockito

[^15_6]: https://github.com/mockito/mockito/issues/1471

[^15_7]: https://www.baeldung.com/mockito-mock-static-methods

[^15_8]: https://rieckpil.de/mocking-static-methods-with-mockito-java-kotlin/

[^15_9]: https://www.digitalocean.com/community/tutorials/mockito-mock-examples

[^15_10]: https://howtodoinjava.com/mockito/mock-static-methods/


---

# It doesn't work yet

Inside the ublic ResponseEntity<BFFOpheffingsetDto> massaleOpheffingOpdracht(String opdrachtId, String reden, MultipartFile file)
I have
try(BufferedReader reader =
new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
StringWriter sw = new StringWriter();
BufferedWriter bw = new BufferedWriter(sw)){
CsvConverter.convert(reader, bw, opdrachtId, reden, file.getOriginalFilename());
String result = sw.toString();
In the test I tried this
try (MockedStatic<CsvConverter> mockedStatic = Mockito.mockStatic(CsvConverter.class)) {
mockedStatic.when(() -> CsvConverter.convert(
any(BufferedReader.class),
any(Writer.class),
anyString(),
anyString(),
anyString()))
.thenAnswer(invocation -> {
Writer writer = new StringWriter();
writer.write("{ \\"warning\\": \\"I am invalid\\"}");
writer.flush();
return null;
});
The goal is that this will trigger

**The bug is here**:

```java
// ❌ WRONG - you're creating a NEW StringWriter inside the Answer
Writer writer = new StringWriter();  // This writer gets discarded!
writer.write("{ \\"warning\\": \\"I am invalid\\"}");
```

Your service creates **its own** `StringWriter sw = new StringWriter()` and passes the `BufferedWriter` wrapping **that** to `CsvConverter.convert()`.

## Fixed version

```java
try (MockedStatic<CsvConverter> mockedStatic = Mockito.mockStatic(CsvConverter.class)) {
    mockedStatic.when(() -> CsvConverter.convert(
            any(BufferedReader.class),
            any(Writer.class),      // ← This is YOUR service's StringWriter via BufferedWriter
            anyString(),
            anyString(),
            anyString()))
        .thenAnswer(invocation -> {
            // Get the ACTUAL Writer passed by your service
            Writer serviceWriter = invocation.getArgument(1);  // ← YOUR sw!
            
            serviceWriter.write("{ \"warning\": \"I am invalid\"}");
            serviceWriter.flush();
            
            return null;  // void method
        });
```

**Key difference**: `invocation.getArgument(1)` extracts **your service's `StringWriter`** (passed through the `BufferedWriter`), not a new one. When your service calls `sw.toString()`, it gets `"{\"warning\": \"I am invalid\"}"` → `ObjectMapper.readValue()` fails → test passes.

## Verify it works

Add this debug line in your service right after `CsvConverter.convert()`:

```java
CsvConverter.convert(reader, bw, opdrachtId, reden, file.getOriginalFilename());
String result = sw.toString();
System.out.println("DEBUG: JSON result = " + result);  // Should print your mock JSON
```

Now `ObjectMapper.readValue(invalid JSON, OpheffingOpdrachtDto.class)` throws `JsonProcessingException` as expected.
<span style="display:none">[^16_1][^16_10][^16_2][^16_3][^16_4][^16_5][^16_6][^16_7][^16_8][^16_9]</span>

<div align="center">⁂</div>

[^16_1]: https://stackoverflow.com/questions/30979087/stub-overriden-tostring-using-mockito-powermock

[^16_2]: https://groups.google.com/g/mockito/c/y8Pou35-TBw

[^16_3]: https://github.com/NagRock/ts-mockito/issues/73

[^16_4]: https://etorreborre.github.io/specs2/guide/SPECS2-4.3.4/org.specs2.guide.UseMockito.html

[^16_5]: https://stackoverflow.com/questions/5724387/unit-test-best-practice-for-method-with-mocks-in-mockito

[^16_6]: https://github.com/mockito/mockito/issues/2154

[^16_7]: https://github.com/dart-lang/mockito/issues/228

[^16_8]: https://groups.google.com/g/mockito/c/9d8xu90TUvk

[^16_9]: https://www.javadoc.io/doc/org.mockito/mockito-core/5.12.0/org/mockito/Mockito.html

[^16_10]: https://github.com/mockito/mockito/issues/2601


---

# Not Yet, debugging reveals that the actual CsvConverter.convert is still invoked instead of the MockedStatic<CsvConverter> mockedStatic

The issue is that **the argument matchers don't match the actual arguments** being passed, so Mockito falls back to the real method.

Your service passes:

```java
CsvConverter.convert(reader, bw, opdrachtId, reden, file.getOriginalFilename());
```

Where:

- `bw` is `BufferedWriter(sw)` ← **not** a plain `Writer`, but a `BufferedWriter` (subtype of `Writer`)
- `opdrachtId`, `reden` are **exact strings** from request params

Your mock matcher:

```java
any(BufferedReader.class), any(Writer.class), anyString(), anyString(), anyString()
```

**`any(Writer.class)` doesn't match `BufferedWriter`** precisely enough for Mockito's static mocking in some cases.

## Fixed version with precise matching

```java
try (MockedStatic<CsvConverter> mockedStatic = Mockito.mockStatic(CsvConverter.class)) {
    mockedStatic.when(() -> CsvConverter.convert(
            any(BufferedReader.class),
            any(BufferedWriter.class),  // ← Exact type match!
            eq("INC0011789123"),       // ← Exact opdrachtId from test
            eq("brief ontvangen"),     // ← Exact reden from test
            eq("orgineel-opheffen.csv") // ← Exact filename from MockMultipartFile
    )).thenAnswer(invocation -> {
        Writer serviceWriter = invocation.getArgument(1);
        serviceWriter.write("{ \"warning\": \"I am invalid\"}");
        serviceWriter.flush();
        return null;
    });

    // Your mockMvc.perform with matching params
    mockMvc.perform(MockMvcRequestBuilders.multipart(POST_MMO_PATH)
        .file(new MockMultipartFile("file", "orgineel-opheffen.csv", "text/csv", "..."))
        .param("opdrachtId", "INC0011789123")
        .param("reden", "brief ontvangen")
        .header("Authorization", "..."))
        .andExpect(status().isBadRequest());
}
```


## Debug to confirm

Add this **right before** your mock setup to verify exact argument values:

```java
@Test
void debugArguments() {
    // Run once to see what gets passed
    System.out.println("opdrachtId in test: '" + "INC0011789123" + "'");
    System.out.println("reden in test: '" + "brief ontvangen" + "'");
    System.out.println("filename: 'orgineel-opheffen.csv'");
    
    // Add breakpoint in service at CsvConverter.convert call
    // Check types: reader.getClass(), bw.getClass(), etc.
}
```


## Alternative: Use `thenAnswer` with looser matching

If exact matching is tricky:

```java
mockedStatic.when(() -> CsvConverter.convert(any(BufferedReader.class), any(Writer.class), any(), any(), any()))
    .thenAnswer(invocation -> {
        // This ALWAYS runs, regardless of argument matching
        Writer writer = invocation.getArgument(1);
        writer.write("{ \"warning\": \"I am invalid\"}");
        writer.flush();
        return null;
    });
```

The `thenAnswer` will execute even if matchers are slightly off, giving you more flexibility.

**Most likely fix**: Change `any(Writer.class)` → `any(BufferedWriter.class)` and use `eq()` for the exact strings from your test params.
<span style="display:none">[^17_1][^17_10][^17_2][^17_3][^17_4][^17_5][^17_6][^17_7][^17_8][^17_9]</span>

<div align="center">⁂</div>

[^17_1]: https://stackoverflow.com/questions/47684234/powermockito-mockstatic-of-a-static-method-is-not-working-correctly-in-spring

[^17_2]: https://github.com/mockito/mockito/issues/3127

[^17_3]: https://github.com/mockito/mockito/issues/1471

[^17_4]: https://github.com/mockito/mockito/issues/2530

[^17_5]: https://dev.to/scottshipp/how-to-fix-a-mockito-cannot-mock-this-class-exception-in-a-spring-boot-app-457e

[^17_6]: https://dzone.com/articles/demystifying-static-mocking-with-mockito

[^17_7]: https://stackoverflow.com/questions/68340718/mockito-mock-static-function-does-not-work-if-the-function-is-called-in-a-thread

[^17_8]: https://www.baeldung.com/mockito-mock-static-methods

[^17_9]: https://stackoverflow.com/questions/4482315/why-doesnt-mockito-mock-static-methods

[^17_10]: https://www.youtube.com/watch?v=GP0LbtG6vx8


---

# It still doesn't work. We have exact matches now. Could it be because other test methods don't use the the MockedStatic<CsvConverter> mockedStatic?

**No, other test methods don't affect it** - `MockedStatic` is **scoped to the `try` block only**. Each test method is completely isolated.

## The real issue: Class loading / Mockito inline mock maker

`@WebMvcTest` + Spring context can interfere with Mockito's static mocking due to **classloader issues**. Mockito's `mockStatic()` requires the **inline mock maker**.

### Fix 1: Add `mockito-inline` dependency (most common solution)

```xml
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-inline</artifactId>
    <scope>test</scope>
</dependency>
```


### Fix 2: Explicit mock maker configuration

Create `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker` file with **exactly** this content:

```
mock-maker-inline
```


### Fix 3: Use `@MockedStatic` annotation (cleanest)

```java
@WebMvcTest(OpheffingsetsApiController.class)
@Import(OpheffingsService.class)
class OpheffingsServiceTest {
    
    @Autowired MockMvc mockMvc;
    @SpyBean OpheffingsService opheffingsService;

    @Test
    @MockedStatic(CsvConverter.class)  // ← Mockito extension handles lifecycle
    void testMassaleOpheffingOpdracht_invalidJson(@MockedStatic.MockedStaticVerification verifier) {
        
        when(() -> CsvConverter.convert(any(BufferedReader.class), any(Writer.class), 
                anyString(), anyString(), anyString()))
            .thenAnswer(invocation -> {
                Writer writer = invocation.getArgument(1);
                writer.write("{ \"warning\": \"I am invalid\"}");
                writer.flush();
                return null;
            });

        // Your mockMvc.perform() here
        mockMvc.perform(...)
            .andExpect(status().isBadRequest());
    }
}
```

**Requires** `mockito-junit-jupiter` dependency:

```xml
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```


## Quick debug test

**Run this standalone first** to isolate Spring interference:

```java
@Test
void debugStandaloneStaticMock() {
    try (MockedStatic<CsvConverter> mocked = mockStatic(CsvConverter.class)) {
        mocked.when(() -> CsvConverter.convert(any(), any(), any(), any(), any()))
            .thenAnswer(inv -> {
                System.out.println("*** MOCK CALLED! ***");
                return null;
            });
        
        // Call directly (no Spring)
        CsvConverter.convert(new BufferedReader(new StringReader("")), 
                           new StringWriter(), "a", "b", "c");
    }
}
```

If **this** prints `*** MOCK CALLED! ***`, then `mockito-inline` is missing. If **this fails**, check Mockito version (needs 3.4.0+).

## Most likely solution

Add `mockito-inline` to your `pom.xml` and restart tests. This fixes 95% of "mockStatic not working in Spring Boot" issues.

```xml
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-inline</artifactId>
    <scope>test</scope>
</dependency>
```

<span style="display:none">[^18_1][^18_10][^18_2][^18_3][^18_4][^18_5][^18_6][^18_7][^18_8][^18_9]</span>

<div align="center">⁂</div>

[^18_1]: https://www.youtube.com/watch?v=UckrWIm0QJg

[^18_2]: https://dev.to/noel_kamphoa_e688aece0725/how-to-mock-static-methods-with-mockito-554b

[^18_3]: https://github.com/mockito/mockito/issues/2182

[^18_4]: https://stackoverflow.com/questions/68111200/how-to-mock-static-method-calls-from-multiple-classes-in-a-single-try-block-usin

[^18_5]: https://www.testim.io/blog/mocking-static-methods-mockito/

[^18_6]: https://www.baeldung.com/mockito-mock-static-methods

[^18_7]: https://stackoverflow.com/questions/21105403/mocking-static-methods-with-mockito

[^18_8]: https://stackoverflow.com/questions/65346523/assert-a-mocked-static-method-is-called-multiple-times-each-with-a-specific-argu

[^18_9]: https://github.com/powermock/powermock/issues/562

[^18_10]: https://dzone.com/articles/demystifying-static-mocking-with-mockito

