<img src="https://r2cdn.perplexity.ai/pplx-full-logo-primary-dark%402x.png" style="height:64px;margin-right:32px"/>

# With OpenAPI (API first) \& Spring Boot, define a service where the GET request can download a zip file

For an API-first Spring Boot service that downloads a ZIP on `GET`, define the response as `application/zip` with a binary schema, then implement the endpoint to return the archive with a `Content-Disposition: attachment` header. OpenAPI 3 uses `type: string` with `format: binary` for file responses, and Spring can serve file content via `ResponseEntity<Resource>` or streaming output.[^1_1][^1_2]

## OpenAPI definition

Use this in your spec for the download endpoint:

```yaml
paths:
  /exports/{id}/zip:
    get:
      operationId: downloadExportZip
      summary: Download export as ZIP
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
      responses:
        '200':
          description: ZIP archive
          headers:
            Content-Disposition:
              schema:
                type: string
              description: Suggested filename for the downloaded file
              example: attachment; filename="export.zip"
          content:
            application/zip:
              schema:
                type: string
                format: binary
        '404':
          description: Export not found
```

That response shape is the standard OpenAPI way to describe downloadable binary content, including ZIP files.[^1_1]

## Spring Boot implementation

A simple controller can return the ZIP as a stream or resource and set the download header:

```java
@RestController
@RequiredArgsConstructor
public class ExportController {

    @GetMapping(value = "/exports/{id}/zip", produces = "application/zip")
    public ResponseEntity<StreamingResponseBody> downloadExportZip(@PathVariable String id) {
        StreamingResponseBody body = outputStream -> {
            try (ZipOutputStream zipOut = new ZipOutputStream(outputStream)) {
                // add entries to zipOut
                // zipOut.putNextEntry(new ZipEntry("file.txt"));
                // zipOut.write(bytes);
                // zipOut.closeEntry();
                zipOut.finish();
            }
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"export.zip\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(body);
    }
}
```

Spring’s `ResponseEntity<Resource>` is also a valid approach for file content, but streaming is usually better for ZIPs because it avoids buffering the whole archive in memory.[^1_3][^1_2]

## OpenAPI Generator notes

If you generate the Spring server from the spec, keep the response as `application/zip` with `format: binary` so the generated interface reflects a binary download instead of JSON. OpenAPI Generator’s Spring server supports `useResponseEntity`, which is helpful for returning headers plus the body from generated APIs.[^1_4][^1_5]

## Practical pattern

A clean API-first setup is:

- OpenAPI spec defines `GET /exports/{id}/zip` with `application/zip` and binary content.[^1_1]
- Generated Spring interface exposes a binary/file response type.[^1_5]
- Your implementation streams the ZIP and sets `Content-Disposition: attachment`.[^1_2][^1_3]

A useful detail: if Swagger UI shows text instead of downloading, the `content` type must be `application/zip` and the schema must be binary.[^1_6]

Would you like a complete `openapi.yaml` plus the matching generated Spring controller signature?
<span style="display:none">[^1_10][^1_11][^1_12][^1_13][^1_14][^1_15][^1_16][^1_17][^1_18][^1_7][^1_8][^1_9]</span>

<div align="center">⁂</div>

[^1_1]: https://swagger.io/docs/specification/v3_0/describing-responses/

[^1_2]: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/responseentity.html

[^1_3]: https://www.baeldung.com/spring-boot-requestmapping-serve-zip

[^1_4]: https://stackoverflow.com/questions/74191632/openapi-generator-spring-boot-and-client-post-and-receive-file

[^1_5]: https://openapi-generator.tech/docs/generators/spring/

[^1_6]: https://github.com/swagger-api/swagger-ui/issues/8319

[^1_7]: https://stackoverflow.com/questions/75596452/springboot-openapi-rest-controller-download-zip-file-even-if-an-exception-is-thr

[^1_8]: https://github.com/OpenAPITools/openapi-generator/issues/12161

[^1_9]: https://stackoverflow.com/questions/51342844/spring-boot-rest-service-to-download-a-zip-file-which-contains-multiple-file

[^1_10]: https://stackoverflow.com/questions/40544063/how-can-i-download-zip-file-from-controller-on-client-side-spring-boot/40544162

[^1_11]: https://github.com/boronngo/openapi-spring-boot-file-download

[^1_12]: https://github.com/OpenAPITools/openapi-generator/issues/17801

[^1_13]: https://stackoverflow.com/questions/59395957/im-trying-to-download-a-zip-file-from-springboot-controller-but-im-getting-wie

[^1_14]: https://www.codejava.net/frameworks/spring-boot/file-download-upload-rest-api-examples

[^1_15]: https://spec.openapis.org/registry/media-type/binary

[^1_16]: https://github.com/swagger-api/swagger-core/issues/1662

[^1_17]: https://github.com/hey-api/openapi-ts/issues/550

[^1_18]: https://stackoverflow.com/questions/63974208/how-to-create-a-swaggerresponse-that-produces-a-binary-application-pdf-file


---

# What would a client using the generated interface extension of the ApiClient.Api look like?

A generated Spring client usually gives you an `Api` interface class with a method whose return type depends on how the response is modeled in the spec. For a ZIP download, the cleanest client shape is typically a method that returns either `ResponseEntity<Resource>`, `Resource`, or a raw response wrapper, depending on the generator/library settings.[^2_1][^2_2][^2_3]

## Typical generated client shape

If your OpenAPI response is `application/zip` with `string` + `binary`, the generated API method often looks conceptually like this:

```java
public interface ExportApi {
    ResponseEntity<Resource> downloadExportZip(String id);
}
```

Or, with a lower-level client style:

```java
public interface ExportApi {
    Response downloadExportZip(String id);
}
```

The exact signature depends on generator options such as `useResponseEntity`, the chosen Java library, and binary type mappings.[^2_2][^2_3][^2_1]

## How you use it

With a Spring-based client bean, you typically inject the generated `ExportApi` and call the method directly:

```java
@Service
@RequiredArgsConstructor
public class ExportService {

    private final ExportApi exportApi;

    public void saveZip(String id, Path target) throws IOException {
        ResponseEntity<Resource> response = exportApi.downloadExportZip(id);
        try (InputStream in = response.getBody().getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
```

That matches the usual generated-client pattern where the `ApiClient` is the low-level HTTP layer and the `Api` interface is the operation-specific entry point.[^2_4][^2_1]

## For binary downloads

For binary file endpoints, generator examples and discussions commonly map the response to a file-like type rather than JSON, and some setups need explicit binary type mappings such as `string+binary=Resource` to get a Spring-friendly client signature.[^2_3][^2_2]

If you want a response wrapper instead of a materialized file type, the lower-level response object can be useful because it gives you direct access to headers and the response body stream.[^2_3]

## Practical recommendation

For a ZIP download client in Spring Boot, I’d aim for:

- OpenAPI response: `application/zip` + `format: binary`.[^2_5]
- Generated client: `ExportApi.downloadExportZip(id)`.[^2_1]
- Return type: `ResponseEntity<Resource>` if you want idiomatic Spring usage, or a raw response wrapper if you need streaming control.[^2_1][^2_3]

A direct answer to your question: the generated interface extension of `ApiClient.Api` would usually expose a method like `downloadExportZip(...)`, and in a Spring-friendly setup you call it as an injected bean, then read the body as a `Resource` or stream.[^2_2][^2_1]

Would you like a concrete example for the Spring client generator config and the exact generated Java signature for your ZIP endpoint?
<span style="display:none">[^2_10][^2_11][^2_12][^2_13][^2_14][^2_15][^2_16][^2_17][^2_6][^2_7][^2_8][^2_9]</span>

<div align="center">⁂</div>

[^2_1]: https://openapi-generator.tech/docs/generators/spring/

[^2_2]: https://stackoverflow.com/questions/76676681/client-side-file-download-with-openapi-generator-plugin

[^2_3]: https://stackoverflow.com/questions/63133959/openapi-generator-generate-correct-method-profile-for-downloading-binary-file

[^2_4]: https://www.palo-it.com/en/blog/spring-boot-client-and-server-code-generation-using-openapi-3-specs

[^2_5]: https://swagger.io/docs/specification/v3_0/describing-responses/

[^2_6]: https://github.com/openapitools/openapi-generator

[^2_7]: https://dev.to/toliyansky/openapi-generation-with-spring-1e57

[^2_8]: https://stackoverflow.com/questions/74191632/openapi-generator-spring-boot-and-client-post-and-receive-file

[^2_9]: https://github.com/OpenAPITools/openapi-generator/issues/17927

[^2_10]: https://openapi-generator.tech/docs/generators/kotlin-spring/

[^2_11]: https://github.com/OpenAPITools/openapi-generator/issues/3905

[^2_12]: https://maciejwalkowiak.com/blog/spring-boot-openapi-generate-client/

[^2_13]: https://www.youtube.com/watch?v=EHMeeqjBTI0

[^2_14]: https://github.com/OpenAPITools/openapi-generator/issues/23381

[^2_15]: https://github.com/OpenAPITools/openapi-generator/issues/6135

[^2_16]: https://www.npmjs.com/package/@openapitools/openapi-generator-cli

[^2_17]: https://www.baeldung.com/spring-boot-http-client-from-openapi-spec

