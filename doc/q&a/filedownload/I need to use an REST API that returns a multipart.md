<img src="https://r2cdn.perplexity.ai/pplx-full-logo-primary-dark%402x.png" style="height:64px;margin-right:32px"/>

# I need to use an REST API that returns a multipart form response that would contain an application/json part and a binary/zip part. The OpenAPI looks like this:

/api/v1/portaal/verificatiebestanden/{id}:
post:
tags:
- portaal-controller
summary: Geeft een verificatie ZIP bestand met bijbehorende pakbon op basis
van ID.
operationId: getPortaalVerificatieBestand
parameters:
- name: Authorization
in: header
required: false
schema:
type: string
- name: id
in: path
required: true
schema:
type: integer
format: int32
responses:
"200":
description: Verificatie ZIP bestand met pakbon opgehaald.
content:
multipart/form-data:
schema:
\$ref: "\#/components/schemas/MultiValueMap"
The generated API code looks like:
@RequestLine("POST /api/v1/portaal/verificatiebestanden/{id}")
@Headers({
"Accept: application/json,multipart/form-data",
"Authorization: {authorization}"
})
ApiResponse<org.springframework.util.MultiValueMap> getPortaalVerificatieBestandWithHttpInfo(@Param("id") @jakarta.annotation.Nonnull Integer id, @Param("authorization") @jakarta.annotation.Nullable String authorization);

How am I to extract the application/json part and a binary/zip part from the org.springframework.util.MultiValueMap?

`org.springframework.util.MultiValueMap` is just the container; you still need to inspect each part by its multipart name and then convert the part value based on the part’s actual type. In practice, for a response that contains one JSON part and one ZIP/file part, you should **not** expect `MultiValueMap` to magically deserialize both for you; you usually extract a `MultipartFile`, `Resource`, `byte[]`, or raw part body for the file, and parse the JSON part separately. Spring’s multipart handling patterns also center on explicit part names such as `@RequestPart("student")` and `@RequestPart("file")`, which reflects the same idea on the client side: parts are handled individually, not as a single typed object.[^1_1][^1_2]

## What you can expect

A `MultiValueMap<String, Object>` from a multipart response typically contains entries like:

- `metadata` or `response` for the JSON part.
- `file` or `zip` for the binary part.

If the generator gives you `ApiResponse<MultiValueMap>`, the main task is to unwrap the map and interpret each entry according to its content type or runtime type. Feign/OpenFeign does not provide a special built-in “multipart response decoder” that automatically maps each part into strongly typed Java objects in the way you might hope; multipart support in Feign is mostly discussed for **requests**, not for decoding multipart **responses**.[^1_3][^1_4]

## Practical extraction

You can handle it like this:

```java
ApiResponse<MultiValueMap> response = api.getPortaalVerificatieBestandWithHttpInfo(id, authorization);

MultiValueMap<?, ?> parts = response.getBody();

Object jsonPart = parts.getFirst("response");   // or the actual part name
Object zipPart  = parts.getFirst("file");       // or the actual part name
```

Then convert based on the actual runtime type:

- If the JSON part is a `String`, deserialize it with Jackson.
- If it is a `byte[]`, turn it into a string first, then deserialize.
- If it is a `Resource` or `InputStream`, read the bytes and deserialize.
- If the ZIP part is a `byte[]`, `Resource`, or `MultipartFile`, read its bytes directly and save them as a `.zip` file.

Example:

```java
ObjectMapper mapper = new ObjectMapper();

Object jsonPart = parts.getFirst("response");
MyDto dto;

if (jsonPart instanceof String s) {
    dto = mapper.readValue(s, MyDto.class);
} else if (jsonPart instanceof byte[] bytes) {
    dto = mapper.readValue(bytes, MyDto.class);
} else {
    throw new IllegalStateException("Unsupported JSON part type: " + jsonPart.getClass());
}

Object zipPart = parts.getFirst("file");
byte[] zipBytes;

if (zipPart instanceof byte[] bytes) {
    zipBytes = bytes;
} else if (zipPart instanceof Resource resource) {
    zipBytes = resource.getInputStream().readAllBytes();
} else {
    throw new IllegalStateException("Unsupported ZIP part type: " + zipPart.getClass());
}
```


## Important caveat

Your OpenAPI schema is underspecified here. `multipart/form-data` with `MultiValueMap` does not tell the generator enough about the **names and schemas of the individual parts**. That is why the generated client is generic. If you control the API spec, define the response parts explicitly instead of `MultiValueMap`, for example with a multipart schema containing one JSON property and one binary property. That usually produces a much better client contract and avoids manual casting.

## Best fix

If possible, change the OpenAPI response from:

```yaml
multipart/form-data:
  schema:
    $ref: "#/components/schemas/MultiValueMap"
```

to a proper multipart object schema with named parts, something like:

```yaml
multipart/form-data:
  schema:
    type: object
    properties:
      metadata:
        $ref: "#/components/schemas/MyDto"
      file:
        type: string
        format: binary
```

That makes the response shape explicit and lets the client know exactly what parts exist. As written, the generated `MultiValueMap` is only a generic bag of parts, so manual extraction is the correct approach.[^1_5][^1_1]

Would you like a concrete example for **Feign + Spring** showing how to decode the multipart response into a DTO and ZIP bytes?
<span style="display:none">[^1_10][^1_6][^1_7][^1_8][^1_9]</span>

<div align="center">⁂</div>

[^1_1]: https://github.com/swagger-api/swagger-ui/issues/7691

[^1_2]: https://stackoverflow.com/questions/54470645/getting-spring-to-map-when-getting-json-along-with-a-file-using-multipart-form-d

[^1_3]: https://github.com/spring-cloud/spring-cloud-openfeign/issues/932

[^1_4]: https://www.baeldung.com/java-feign-file-upload

[^1_5]: https://github.com/springdoc/springdoc-openapi/issues/820

[^1_6]: https://stackoverflow.com/questions/73451103/send-multivaluemap-as-multipartformdata-in-feign-client

[^1_7]: https://stackoverflow.com/questions/46286939/spring-rest-return-multipart-form-data-with-application-json-content-type

[^1_8]: https://github.com/OpenFeign/feign/issues/2813

[^1_9]: https://stackoverflow.com/questions/52818107/how-to-send-the-multipart-file-and-json-data-to-spring-boot

[^1_10]: https://stackoverflow.com/questions/72982587/why-springdoc-openapi-doesnt-understand-multipartfile-payload

